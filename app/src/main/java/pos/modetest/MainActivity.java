package pos.modetest;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static pos.modetest.observers.LocationObserver.AIDING_DATA_PRESET_COLD;
import static pos.modetest.observers.LocationObserver.AIDING_DATA_PRESET_WARM;
import static pos.modetest.observers.LocationObserver.DEL_AD_DELAY_MS;
import static pos.modetest.utils.Constants.EMOJI_NG;
import static pos.modetest.utils.Constants.EMOJI_OK;
import static pos.modetest.utils.Constants.EMPTY_TEXT_2C;
import static pos.modetest.utils.Constants.EMPTY_TEXT_3C;
import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.GnssCapabilities;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationRequest;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyDisplayInfo;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import pos.modetest.data.CellInfoHeaderHolder;
import pos.modetest.data.CellInfoHolder;
import pos.modetest.data.CellInfoHolderFactory;
import pos.modetest.data.GnssSvStatusHeaderHolder;
import pos.modetest.data.GnssSvStatusHolder;
import pos.modetest.data.LocationHolder;
import pos.modetest.databinding.ActivityPosModeTestBinding;
import pos.modetest.databinding.LayoutCellInfoRowBinding;
import pos.modetest.databinding.LayoutSnipTextBinding;
import pos.modetest.databinding.LayoutSvStatusRowBinding;
import pos.modetest.observers.DataObserver;
import pos.modetest.observers.LocationObserver;
import pos.modetest.observers.TelephonyObserver;
import pos.modetest.observers.WifiObserver;
import pos.modetest.utils.ConfigUtils;
import pos.modetest.utils.Constants;
import pos.modetest.utils.HelperUtils;
import pos.modetest.utils.IntentUtils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = TAG_PREFIX + "Test";

    private static final int ALL_PERMISSIONS_REQUEST = 0;
    private static final Uri mAgnssUri = Settings.Global.getUriFor("assisted_gps_enabled");

    private ConnectivityManager cm;

    private ToneGenerator toneGenerator;
    private Handler mHandler;
    private Executor mExecutor;
    private Executor mBgExecutor;
    private List<String> mDelAdItems;

    private TelephonyObserver telephonyObserver;
    private DataObserver dataObserver;
    private WifiObserver wifiObserver;
    private LocationObserver locationObserver;

    private int mCurrentTtffMillis = 0;
    private int mCurrentFixCount = 0;
    private Runnable mOnPermissionsRunnable;

    private ActivityPosModeTestBinding mainBinding;

    private final TelephonyObserver.Listener telephonyObserverListener
            = new TelephonyObserver.Listener() {
        private static final int INVALID_SLOT = -1;
        private String dataInfo = "";
        private String dataActivity = "";
        private int activeSlot = INVALID_SLOT;

        private final Map<SubscriptionInfo, List<CellInfo>> cellInfoMap = new HashMap<>();

        @Override
        public void onSubscriptionChanged(int slot, @Nullable SubscriptionInfo subInfo) {
            dataInfo = "";
            dataActivity = "";
            if (subInfo == null) {
                if (activeSlot == slot) {
                    activeSlot = INVALID_SLOT;
                }
            } else if (subInfo.getSubscriptionId() == telephonyObserver.getDefaultSubId()) {
                activeSlot = slot;
            }
            Log.d(TAG, String.format("onSubscriptionChanged(%d) -> %s", slot, slot == activeSlot));
            doUpdateDisplay();
        }

        @Override
        public void onSubscriptionsChanged(@NonNull Map<Integer, SubscriptionInfo> subInfoMap) {
            Log.v(TAG, String.format("onSubscriptionsChanged(%s) -> %s",
                    subInfoMap.keySet(),
                    Arrays.toString(subInfoMap.values().stream()
                            .map(SubscriptionInfo::getSubscriptionId).toArray())
            ));
            cellInfoMap.clear();
            for (var subInfo : subInfoMap.values()) {
                cellInfoMap.put(subInfo, null);
            }
            doUpdateDisplay();
        }

        @Override
        public void onCellInfoChanged(@NonNull SubscriptionInfo subInfo,
                                      @NonNull List<CellInfo> cellInfo) {
            Log.v(TAG, String.format("onCellInfoChanged(%d, %d)",
                    subInfo.getSimSlotIndex(),
                    cellInfo.size()
            ));
            if (cellInfoMap.containsKey(subInfo)) {
                cellInfoMap.put(subInfo, cellInfo);
            }
            updateCellInfo(cellInfoMap);
        }

        private void doUpdateDisplay() {
            String info = String.format("%s %s", dataInfo, dataActivity);
            mainBinding.layoutNetInfo.setPhoneStatus((activeSlot == INVALID_SLOT || info.isBlank())
                    ? EMPTY_TEXT_3C : info);
        }

        @Override
        public void onDisplayInfoChanged(@NonNull SubscriptionInfo subInfo,
                                         @NonNull TelephonyDisplayInfo telephonyDisplayInfo) {
            dataInfo = String.format(Locale.getDefault(), "SIM%d/%s",
                    subInfo.getSimSlotIndex() + 1,
                    TelephonyObserver.getNetworkTypeString(
                            telephonyDisplayInfo.getNetworkType(),
                            telephonyDisplayInfo.getOverrideNetworkType()
                    ));
            Log.v(TAG, String.format("onDisplayInfoChanged(%d) -> %s",
                    subInfo.getSimSlotIndex(),
                    dataInfo
            ));
            doUpdateDisplay();
        }

        @Override
        public void onDataActivity(@NonNull SubscriptionInfo subInfo, int direction) {
            dataActivity = TelephonyObserver.getDataActivityString(direction);
            Log.v(TAG, String.format("onDataActivity(%d) -> %s",
                    subInfo.getSimSlotIndex(),
                    dataActivity
            ));
            doUpdateDisplay();
        }
    };

    private final DataObserver.Listener dataObserverListener = new DataObserver.Listener() {
        /** @noinspection deprecation*/
        @Override
        public void onDataNetworksChanged(@NonNull Map<Network, SubscriptionInfo> dataInfoMap) {
            Log.v(TAG, String.format("onDataNetworksChanged(%s) -> %s",
                    Arrays.toString(dataInfoMap.keySet().stream()
                            .map(Network::toString).toArray()),
                    Arrays.toString(dataInfoMap.values().stream()
                            .filter(Objects::nonNull)
                            .map(SubscriptionInfo::getSimSlotIndex).toArray())
            ));
            StringJoiner msgData = new StringJoiner(" ");
            StringJoiner msgRoam = new StringJoiner(" / ");
            StringJoiner msgApn = new StringJoiner(" ");
            msgData.setEmptyValue(EMPTY_TEXT_3C);
            msgRoam.setEmptyValue(EMPTY_TEXT_3C);
            msgApn.setEmptyValue(EMPTY_TEXT_3C);
            var networks = dataObserver.getAllActiveNetworks();
            for (var entry : dataInfoMap.entrySet()) {
                var caps = networks.get(entry.getKey());
                var subInfo = entry.getValue();
                if (subInfo == null) continue;
                msgData.add(String.format(Locale.getDefault(), "SIM%d",
                        subInfo.getSimSlotIndex() + 1
                ));
                // noinspection MissingPermission
                boolean roaming = telephonyObserver.isRoaming(subInfo.getSubscriptionId());
                msgRoam.add(String.format(Locale.getDefault(), "%s (%s-%s)",
                        roaming ? "Roaming" : "Home",
                        subInfo.getMccString(),
                        subInfo.getMncString()
                ));
                if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_SUPL)) {
                    msgData.add("(SUPL)");
                    // No unrestricted alternative for this API
                    NetworkInfo info = cm.getNetworkInfo(entry.getKey());
                    if (info != null) msgApn.add(info.getExtraInfo());
                }
            }
            mainBinding.layoutNetInfo.setDataStatus(msgData.toString());
            mainBinding.layoutNetInfo.setRoamStatus(msgRoam.toString());
            mainBinding.layoutNetInfo.setSuplApn(msgApn.toString());
        }
    };

    private final WifiObserver.Listener wifiObserverListener = new WifiObserver.Listener() {

        @Override
        public void onWifiNetworksChanged(@NonNull Map<Network, WifiInfo> wifiInfoMap) {
            Log.v(TAG, String.format("onWifiNetworksChanged(%s) -> %s",
                    Arrays.toString(wifiInfoMap.keySet().stream()
                            .map(Network::toString).toArray()),
                    Arrays.toString(wifiInfoMap.values().stream()
                            .map(WifiInfo::getSSID).toArray())
            ));
            StringJoiner msg = new StringJoiner(", ");
            msg.setEmptyValue(EMPTY_TEXT_3C);
            for (var info : wifiInfoMap.values()) {
                String ssid = info.getSSID();
                ssid = ssid.equals(WifiManager.UNKNOWN_SSID)
                        ? "" : ssid.substring(1, ssid.length() - 1);
                msg.add(String.format(Locale.getDefault(), "%s (WiFi%d)",
                        ssid, info.getWifiStandard()
                ));
            }
            mainBinding.layoutNetInfo.setWifiStatus(msg.toString());
        }
    };

    private final LocationObserver.Listener locationObserverListener
            = new LocationObserver.DefaultListener() {
        private GnssStatus svStatus = null;

        @Override
        public void onLocationChanged(@Nullable Location location) {
            Log.i(TAG, String.format("onLocationChanged(%s)",
                    location != null ? location.toString() : ""));
            mainBinding.layoutLocInfo.setLoc(new LocationHolder(location));
            String mode = mainBinding.spinnerPosMode.getSelectedItem().toString();
            if (mode.equals(getString(R.string.pos_mode_single)) ||
                    mode.equals(getString(R.string.pos_mode_last))) {
                doStopLocating();
            }
            mainBinding.fixTimer.stop();
            mCurrentFixCount += 1;
            mainBinding.setFixCount(mCurrentFixCount);
            soundBEEP();
        }

        @Override
        public void onGnssStarted() {
            super.onGnssStarted();
            svStatus = null;
            updateGnssStatus(getString(R.string.gnss_status_started), svStatus);
            updateGnssStatusTable(svStatus);
        }

        @Override
        public void onFirstFix(int ttffMillis) {
            super.onFirstFix(ttffMillis);
            mCurrentTtffMillis = ttffMillis;
            updateGnssStatus(null, svStatus);
        }

        @Override
        public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
            super.onSatelliteStatusChanged(status);
            svStatus = status;
            updateGnssStatus(null, svStatus);
            updateGnssStatusTable(svStatus);
        }

        @Override
        public void onGnssMeasurementsReceived(@NonNull GnssMeasurementsEvent eventArgs) {
            super.onGnssMeasurementsReceived(eventArgs);
            updateGnssStatus(null, svStatus);
            updateGnssMiscStatus(eventArgs);
        }

        @Override
        public void onGnssStopped() {
            super.onGnssStopped();
            // Keep last svStatus till next session
            // svStatus = null;
            updateGnssStatus(getString(R.string.gnss_status_stopped), svStatus);
        }
    };

    private ContentObserver agnssStatusObserver;


    /**
     * Activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Enable edge-to-edge on A14 and lower
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        Log.d(TAG, String.format("onCreate(); SDK=%d", Build.VERSION.SDK_INT));

        // Keep screen on and disable auto-rotate
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        mainBinding = ActivityPosModeTestBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        // Setup action bar
        setSupportActionBar(mainBinding.toolbar);

        toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);

        mHandler = new Handler(Looper.getMainLooper());
        mExecutor = getMainExecutor();
        mBgExecutor = Executors.newSingleThreadExecutor();
        cm = getSystemService(ConnectivityManager.class);
        mDelAdItems = new ArrayList<>(Arrays.asList(AIDING_DATA_PRESET_COLD));

        telephonyObserver = new TelephonyObserver(this, telephonyObserverListener, mExecutor);
        dataObserver = new DataObserver(this, dataObserverListener, mExecutor);
        wifiObserver = new WifiObserver(this, wifiObserverListener, mExecutor);
        locationObserver = new LocationObserver(this, locationObserverListener, mExecutor);
        agnssStatusObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                doUpdateAgnssStatus();
            }
        };

        mainBinding.conf.setOnApplyWindowInsetsListener((v, windowInsets) -> {
            var insets = windowInsets.getInsets(WindowInsets.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    v.getPaddingBottom() + insets.bottom);
            return windowInsets;
        });

        mainBinding.svTable.setOnApplyWindowInsetsListener((v, windowInsets) -> {
            var insets = windowInsets.getInsets(WindowInsets.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    v.getPaddingBottom() + insets.bottom);
            return windowInsets;
        });

        mainBinding.buttonRun.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked || doCheckPermission(true, this::doStartLocating)
                    != PermissionChecker.PERMISSION_GRANTED) {
                doStopLocating();
            }
        });

        mainBinding.buttonDelete.setOnClickListener(v -> doDeleteAidingData());

        mainBinding.buttonDelete.setOnLongClickListener(v -> {
            doConfigDeleteAidingData();
            return true; // consumed
        });

        doCheckPermission(true, null);
    }

    private void doResume() {
        Log.i(TAG, "doResume()");
        HelperUtils.requestAddShortcut(this);

        doUpdateLocationOptions();

        //get assisted_gps_enabled
        doUpdateAgnssStatus();
        if (mAgnssUri != null) {
            this.getContentResolver().registerContentObserver(mAgnssUri, true,
                    agnssStatusObserver);
        }

        // Empty calls to preset fields
        doResetText();
        telephonyObserverListener.onSubscriptionsChanged(new HashMap<>());
        dataObserverListener.onDataNetworksChanged(new HashMap<>());
        wifiObserverListener.onWifiNetworksChanged(new HashMap<>());

        doCheckPermission(false, this::doPermissionsGrantedStart);
    }

    @RequiresPermission(allOf = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    private void doPermissionsGrantedStart() {
        Log.i(TAG, "doPermissionsGrantedStart()");
        doUpdateConfigData();
        telephonyObserver.startObserving();
        dataObserver.startObserving();
        wifiObserver.startObserving();
        locationObserver.startObserving();
    }

    private void shortUserMessage(String message) {
        Log.d(TAG, String.format("shortUserMessage(%s)", message));
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void doUpdateConfigData() {
        Log.d(TAG, "doUpdateConfigData()");
        mainBinding.conf.setText(ConfigUtils.readConfigsByType(getApplicationContext(),
                ConfigUtils.DEFAULT_CONFIG_TYPES));
    }

    private void doConfigDeleteAidingData() {
        Log.i(TAG, "doConfigDeleteAidingData()");
        var opts = Arrays.asList(LocationObserver.AIDING_DATA_OPTS);
        var sel = new boolean[opts.size()];
        for (String s : mDelAdItems) {
            sel[opts.indexOf(s)] = true;
        }
        Runnable saveSel = () -> {
            mDelAdItems.clear();
            for (var opt : opts) {
                if (sel[opts.indexOf(opt)]) {
                    mDelAdItems.add(opt);
                }
            }
        };
        var dlg = new AlertDialog.Builder(this)
            .setTitle("Select Aiding Data to Delete")
            .setMultiChoiceItems(LocationObserver.AIDING_DATA_OPTS, sel,
                    (dialog, which, isChecked) -> sel[which] = isChecked)
            .setPositiveButton("Delete", (dialog, which) -> {
                saveSel.run();
                doDeleteAidingData();
            })
            .setNeutralButton("Save", (dialog, which) -> saveSel.run())
            .setNegativeButton("Cancel", null)
            .create();
        dlg.create();
        FrameLayout custom = dlg.findViewById(androidx.appcompat.R.id.custom);
        FrameLayout customPanel = dlg.findViewById(androidx.appcompat.R.id.customPanel);
        if (customPanel != null && custom != null) {
            var snipText = LayoutSnipTextBinding.inflate(getLayoutInflater());
            snipText.setSnipText(String.format("Warm: %s\nCold: %s",
                    String.join(", ", AIDING_DATA_PRESET_WARM),
                    String.join(", ", AIDING_DATA_PRESET_COLD)
            ));
            custom.addView(snipText.getRoot(),
                    new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            customPanel.setVisibility(View.VISIBLE);
        }
        dlg.show();
    }

    private void doDeleteAidingData() {
        Log.i(TAG, "doDeleteAidingData()");
        StringJoiner buf = new StringJoiner(", ");
        buf.setEmptyValue(getString(R.string.tbl_empty_s));
        mDelAdItems.forEach(buf::add);
        var dlg = new AlertDialog.Builder(this)
                .setTitle("Deleting Aiding Data")
                .setMessage(buf.toString())
                .create();
        CountDownTimer timer = new CountDownTimer(DEL_AD_DELAY_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long rem_sec = millisUntilFinished/1000 + 1;
                dlg.setMessage(buf + "\n\nCloses in " + rem_sec + " seconds");
            }
            @Override
            public void onFinish() {
                dlg.cancel();
            }
        };
        dlg.setOnDismissListener(dialog -> timer.cancel());
        try {
            doUpdateLocationOptions();
            locationObserver.deleteAidingData(mDelAdItems.toArray(new String[0]));
            dlg.show();
            timer.start();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            shortUserMessage(e.getMessage());
        }
    }

    private void doUpdateLocationOptions() {
        String provider = mainBinding.spinnerPosProv.getSelectedItem().toString();
        String modeStr = mainBinding.spinnerPosMode.getSelectedItem().toString();
        String qualityStr = mainBinding.spinnerPosQualities.getSelectedItem().toString();
        boolean isFullTrack = mainBinding.checkFullTrack.isChecked();
        Log.i(TAG, String.format("doUpdateLocationOptions(%s, %s, %s, %s)",
                provider, modeStr, qualityStr, isFullTrack));

        if (locationObserver.isProviderUnavailable(provider)) {
            shortUserMessage(String.format("\"%s\" Provider is not available", provider));
            mainBinding.buttonRun.setChecked(false);
            return;
        }

        LocationObserver.LocationMode mode;
        if (modeStr.equals(getString(R.string.pos_mode_single))) {
            mode = LocationObserver.LocationMode.SINGLE;
        } else if (modeStr.equals(getString(R.string.pos_mode_track))) {
            mode = LocationObserver.LocationMode.TRACK;
        } else if (modeStr.equals(getString(R.string.pos_mode_last))) {
            mode = LocationObserver.LocationMode.LAST;
        } else {
            shortUserMessage(String.format("\"%s\" Mode is not supported", modeStr));
            mainBinding.buttonRun.setChecked(false);
            return;
        }

        int quality;
        if (qualityStr.equals(getString(R.string.pos_quality_high_acc))) {
            quality = LocationRequest.QUALITY_HIGH_ACCURACY;
        } else if (qualityStr.equals(getString(R.string.pos_quality_balanced))) {
            quality = LocationRequest.QUALITY_BALANCED_POWER_ACCURACY;
        } else if (qualityStr.equals(getString(R.string.pos_quality_low_power))) {
            quality = LocationRequest.QUALITY_LOW_POWER;
        } else {
            shortUserMessage(String.format("\"%s\" Quality is not supported", qualityStr));
            mainBinding.buttonRun.setChecked(false);
            return;
        }


        locationObserver.setLocationOptions(provider, mode, quality);
        locationObserver.setForceFullTracking(isFullTrack);
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void doStartLocating() {
        Log.i(TAG, "doStartLocating()");

        doUpdateLocationOptions();
        locationObserver.startLocating();

        // UI/UX updates
        doResetText();
        mainBinding.runTimer.start();
        mainBinding.fixTimer.start();
        if (!mainBinding.buttonRun.isChecked())
            mainBinding.buttonRun.toggle();
    }

    private void doStopLocating() {
        Log.i(TAG, "doStopLocating()");
        locationObserver.stopLocating();
        mainBinding.fixTimer.stop();
        mainBinding.runTimer.stop();
        if (mainBinding.buttonRun.isChecked())
            mainBinding.buttonRun.toggle();
    }

    private void doUpdateAgnssStatus() {
        boolean agnss_enabled = Settings.Global.getInt(this.getContentResolver(),
                "assisted_gps_enabled", 1) != 0;
        mainBinding.layoutNetInfo.setIsAgnssEnabled(agnss_enabled);
    }

    private void doPause() {
        Log.i(TAG, "doPause()");

        if (mAgnssUri != null) {
            this.getContentResolver().unregisterContentObserver(agnssStatusObserver);
        }

        if (doCheckPermission(false, null) != PermissionChecker.PERMISSION_GRANTED) {
            return;
        }
        doStopLocating();
        telephonyObserver.stopObserving();
        wifiObserver.stopObserving();
        dataObserver.stopObserving();
        locationObserver.stopObserving();
    }

    @Override
    protected void onResume() {
        super.onResume();
        doResume();
    }

    @Override
    protected void onPause() {
        doPause();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pos_mode_test, menu);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (pInfo != null && pInfo.versionName != null) {
                menu.findItem(R.id.menu_version_info).setTitle("Version: " + pInfo.versionName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error at getPackageInfo: ", e);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            var pkg = ConfigUtils.getNfwLocationPackage(this);
            var pInfo = getPackageManager().getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
            if (pInfo != null && pInfo.requestedPermissions != null &&
                    pInfo.requestedPermissionsFlags != null) {
                var fine = List.of(pInfo.requestedPermissions)
                        .indexOf(Manifest.permission.ACCESS_FINE_LOCATION);
                var granted = (pInfo.requestedPermissionsFlags[fine]
                        & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                var info = menu.findItem(R.id.menu_nfw_info);
                var info_txt = getString(R.string.menu_nfw_info);
                var info_txt_extra = (granted ? EMOJI_OK : EMOJI_NG);
                var extra_color = getColor(granted ? R.color.success : R.color.error);
                var spannable = new SpannableStringBuilder();
                spannable.append(info_txt);
                spannable.append(" ");
                spannable.append(info_txt_extra, new ForegroundColorSpan(extra_color),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                info.setTitle(spannable);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error at getPackageInfo: ", e);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ALL_PERMISSIONS_REQUEST) {
            if (grantResults.length > 0) {
                if (Arrays.stream(grantResults)
                        .allMatch(g -> g == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "onRequestPermissionsResult(granted)");
                    if (mOnPermissionsRunnable != null) {
                        var r = mOnPermissionsRunnable;
                        mOnPermissionsRunnable = null;
                        r.run();
                    }
                } else {
                    Log.d(TAG, "onRequestPermissionsResult(denied)");
                    shortUserMessage("Please grant permissions");
                    startIntent(IntentUtils.createAppDetailsSettingsIntent(this));
                }
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        Location location;
        if (itemId == R.id.menu_geocode || itemId == R.id.menu_open_in_map) {
            location = mainBinding.layoutLocInfo.getLoc().getLocation();
            if (location == null) {
                shortUserMessage("No Location fix");
                return true;
            }
        } else {
            location = null;
        }

        if (itemId == R.id.menu_geocode) {
            mBgExecutor.execute(() -> doGeocode(location));
        } else if (itemId == R.id.menu_open_in_map) {
            startIntent(IntentUtils.createLocationViewingIntent(location));
        } else if (itemId == R.id.menu_radio_info) {
            startIntent(IntentUtils.createRadioInfoSettingsIntent());
        } else if (itemId == R.id.menu_airplane_mode) {
            startIntent(IntentUtils.createAirplaneModeSettingsIntent());
        } else if (itemId == R.id.menu_location_settings) {
            startIntent(IntentUtils.createLocationSettingsIntent());
        } else if (itemId == R.id.menu_app_info) {
            startIntent(IntentUtils.createAppDetailsSettingsIntent(this));
        } else if (itemId == R.id.menu_nfw_info) {
            startIntent(IntentUtils.createNfwDetailsSettingsIntent(this));
        } else if (itemId == R.id.menu_logalong) {
            startIntent(IntentUtils.createLogalongLauncherIntent());
        } else if (itemId == R.id.menu_gms_update) {
            startIntent(IntentUtils.createGmsPlaystoreIntent());
        } else if (itemId == R.id.menu_read_config) {
            startIntent(new Intent(this, ReadConfigActivity.class));
        } else if (itemId == R.id.menu_bluesky_check) {
            AlertDialog ald = (new AlertDialog.Builder(this))
                    .setTitle("Bluesky Check")
                    .setMessage(getBlueskyCheckSpannable())
                    .setPositiveButton("OK", null)
                    .setNeutralButton("TRACK", (dialog, which) -> {
                        Intent i = new Intent(this, BlueskyTrackService.class);
                        var c = startService(i);
                        Log.d(TAG, "Started " + c);
                    })
                    .create();
            ald.show();
        } else {
            Log.w(TAG, "Unexpected menu selected : " + getResources().getResourceEntryName(itemId));
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @NonNull
    private Spannable getBlueskyCheckSpannable() {
        var spannable = new SpannableStringBuilder();
        var color_ok = getColor(R.color.success);
        var color_ng = getColor(R.color.error);

        var bluesky_okay = true;
        GnssCapabilities caps = null;
        // noinspection ObsoleteSdkInt
        var isAndroidQorLater = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        // noinspection ObsoleteSdkInt
        var isAndroidRorLater = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R);
        // noinspection ObsoleteSdkInt
        var isAndroidSorLater = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S);
        var isAndroidUorLater = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE);

        if (isAndroidRorLater) {
            caps = locationObserver.getGnssCapabilities();
        }

        // is Android Q or later ?
        spannable.append("Android Q or later : ");
        spannable.append(
                isAndroidQorLater ? EMOJI_OK : EMOJI_NG,
                new ForegroundColorSpan(isAndroidQorLater ? color_ok : color_ng),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        bluesky_okay &= isAndroidQorLater;

        // has GNSS measurements capability ?
        spannable.append("\nGNSS Measurements : ");
        if (isAndroidSorLater) {
            var isOkay = caps.hasMeasurements();
            spannable.append(
                    isOkay ? EMOJI_OK : EMOJI_NG,
                    new ForegroundColorSpan(isOkay ? color_ok : color_ng),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            // noinspection ConstantValue
            bluesky_okay &= isOkay;
        } else {
            spannable.append("-");
        }

        // has GNSS Measurement Corrections capability ?
        spannable.append("\nGNSS Measurement Corrections : ");
        if (isAndroidUorLater) {
            var isOkay = caps.hasMeasurementCorrections();
            spannable.append(
                    isOkay ? EMOJI_OK : EMOJI_NG,
                    new ForegroundColorSpan(isOkay ? color_ok : color_ng),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            bluesky_okay &= isOkay;
        } else {
            spannable.append("-");
        }

        // has GNSS Measurement Corrections LosSats capability ?
        spannable.append("\nGNSS Measurement Corrections LosSats : ");
        if (isAndroidUorLater) {
            var isOkay = caps.hasMeasurementCorrectionsLosSats();
            spannable.append(
                    isOkay ? EMOJI_OK : EMOJI_NG,
                    new ForegroundColorSpan(isOkay ? color_ok : color_ng),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            bluesky_okay &= isOkay;
        } else {
            spannable.append("-");
        }

        spannable.append("\n\nBluesky Okay : ");
        if (!isAndroidSorLater || !isAndroidUorLater) {
            spannable.append("-");
        } else {
            spannable.append(
                    bluesky_okay ? EMOJI_OK : EMOJI_NG,
                    new ForegroundColorSpan(bluesky_okay ? color_ok : color_ng),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        return spannable;
    }

    private void doGeocode(Location location) {
        final int MAX_RESULTS = 1;
        if (Geocoder.isPresent()) {
            Geocoder coder = new Geocoder(this);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    coder.getFromLocation(location.getLatitude(), location.getLongitude(),
                            MAX_RESULTS, addresses -> mExecutor.execute(() ->
                                    doHandleGeocodeResult(location, addresses)));
                } else {
                    var addresses = coder.getFromLocation(
                            location.getLatitude(), location.getLongitude(),
                            MAX_RESULTS);
                    mExecutor.execute(() -> doHandleGeocodeResult(location, addresses));
                }
            } catch (Exception e) {
                Log.e(TAG, "Geocoding Error", e);
            }
        } else {
            mExecutor.execute(() -> doHandleGeocodeResult(location, null));
        }
    }

    private void doHandleGeocodeResult(Location location, List<Address> addresses) {
        if (Geocoder.isPresent()) {
            if (addresses != null && !addresses.isEmpty()) {
                shortUserMessage(addresses.get(0).getAddressLine(0));
            } else {
                shortUserMessage("No addresses");
            }
        } else {
            shortUserMessage("No Geocoder");
        }
    }

    private void startIntent(Intent intent) {
        Log.i(TAG, String.format("startIntent(%s)", intent.toString()));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, null, e);
            shortUserMessage("Can't start: " + intent);
        }
    }

    private void updateCellInfo(@NonNull Map<SubscriptionInfo, List<CellInfo>> cellInfoMap) {
        mainBinding.cellInfoTable.removeAllViews();

        if (cellInfoMap.isEmpty()) {
            return;
        }

        // Header Row
        var cellInfoRowBinding = LayoutCellInfoRowBinding.inflate(getLayoutInflater(),
                        mainBinding.cellInfoTable, true);
        CellInfoHolder holder = new CellInfoHeaderHolder(getApplicationContext());
        cellInfoRowBinding.setCellInfo(holder);
        cellInfoRowBinding.getRoot().setBackgroundColor(getColor(R.color.tbl_header_bg));

        // Individual cell Rows
        for (final var info : cellInfoMap.entrySet()) {
            SubscriptionInfo subInfo = info.getKey();
            if (info.getValue() == null)
                continue;
            for (final CellInfo cellInfo : info.getValue()) {
                cellInfoRowBinding = LayoutCellInfoRowBinding.inflate(getLayoutInflater(),
                                mainBinding.cellInfoTable, true);
                holder = CellInfoHolderFactory.makeFor(subInfo.getSimSlotIndex(), cellInfo);
                cellInfoRowBinding.setCellInfo(holder);
            }
        }
    }

    @PermissionChecker.PermissionResult
    private int doCheckPermission(boolean ask, @Nullable Runnable callback) {
        Set<String> permSet = new HashSet<>();
        permSet.addAll(Arrays.asList(DataObserver.permissions));
        permSet.addAll(Arrays.asList(LocationObserver.permissions));
        permSet.addAll(Arrays.asList(TelephonyObserver.permissions));
        permSet.addAll(Arrays.asList(WifiObserver.permissions));
        permSet.addAll(BlueskyTrackService.getPermissions());

        String[] perms = permSet.toArray(new String[0]);

        boolean ok = HelperUtils.checkPermissions(this, perms);

        if (ok) {
            if (callback != null) callback.run();
        } else {
            if (ask) {
                mOnPermissionsRunnable = callback;
                requestPermissions(perms, ALL_PERMISSIONS_REQUEST);
            }
        }
        Log.i(TAG, String.format("doCheckPermission(%s) -> %s", ask, (ok ? "granted" : "denied")));
        return ok ? PermissionChecker.PERMISSION_GRANTED : PermissionChecker.PERMISSION_DENIED;
    }

    private void soundBEEP() {
        if (toneGenerator == null) {
            return;
        }
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
        } catch (Exception e) {
            Log.e(TAG, "ToneGenerator has Exception !" + e.getMessage());
        }
    }

    private void doResetText() {
        mainBinding.layoutLocInfo.setLoc(new LocationHolder(null));
        mainBinding.svTable.removeAllViews();
        switchConfAndTable(true);
        mainBinding.runTimer.setBase(SystemClock.elapsedRealtime());
        mainBinding.fixTimer.setBase(SystemClock.elapsedRealtime());
        mCurrentTtffMillis = 0;
        mCurrentFixCount = 0;
        mainBinding.setFixCount(mCurrentFixCount);
        updateGnssStatus(null, null);
        updateGnssMiscStatus(null);
    }

    private void switchConfAndTable(boolean showConf) {
        // conf is first view
        int now = mainBinding.switcherTblConf.getDisplayedChild();
        if ((showConf && now != 0) || (!showConf && now == 0)) {
            mainBinding.switcherTblConf.showNext();
        }
    }

    private void updateGnssStatus(String status, GnssStatus svStatus) {
        int total = svStatus != null ? svStatus.getSatelliteCount() : 0;
        int used = 0;
        int inView = 0;

        for (int i = 0; i < total; i++) {
            if (Float.compare(svStatus.getCn0DbHz(i), 0.0f) != 0)
                inView++;
            if (svStatus.usedInFix(i))
                used++;
        }

        if (status == null) {
            if (mCurrentTtffMillis > 0 && used > 0)
                status = getString(R.string.gnss_status_tracking);
            else if (inView > 0)
                status = getString(R.string.gnss_status_acquiring);
            else if (svStatus != null)
                status = getString(R.string.gnss_status_searching);
            else
                status = getString(R.string.gnss_status_stopped);
        }

        mainBinding.layoutGnssInfo.gnssStatus.setText(status);

        mainBinding.layoutGnssInfo.gnssTtff.setText(String.format("TTFF: %s",
                mCurrentTtffMillis != 0 ? mCurrentTtffMillis+"ms" : "No Fix yet"));

        mainBinding.layoutGnssInfo.gnssSvStatus.setText(String.format(
                "Used: %s    InView: %s    Total: %s",
                svStatus != null ? used : Constants.EMPTY_TEXT_2C,
                svStatus != null ? inView : Constants.EMPTY_TEXT_2C,
                svStatus != null ? total : Constants.EMPTY_TEXT_2C
        ));
    }

    private void updateGnssMiscStatus(@Nullable GnssMeasurementsEvent eventArgs) {
        boolean fullTracking = LocationObserver.getFullTracking(eventArgs);
        long mpCount = LocationObserver.getMultipathSvCount(eventArgs);
        double agcAvg = LocationObserver.getAgcAverage(eventArgs);
        StringJoiner misc = new StringJoiner("\n");
        misc.add(String.format("Full tracking: %s", fullTracking));
        misc.add(String.format("Multipath SVs: %s", mpCount == 0 ? EMPTY_TEXT_2C : mpCount));
        misc.add(String.format("AGC Average: %s", Double.isNaN(agcAvg) ? EMPTY_TEXT_2C
                : String.format(Locale.getDefault(), "%.02f", agcAvg)));
        mainBinding.layoutGnssInfo.gnssMiscStatus.setText(misc.toString());
    }

    private void updateGnssStatusTable(GnssStatus status) {
        mainBinding.svTable.removeAllViews();
        switchConfAndTable(false);

        // Don't draw table if no status. To differentiate between empty and no status
        if (status == null) return;

        // Header Row
        var svStatusRowBinding = LayoutSvStatusRowBinding.inflate(getLayoutInflater(),
                mainBinding.svTable, true);
        GnssSvStatusHolder holder = new GnssSvStatusHeaderHolder(getApplicationContext());
        svStatusRowBinding.setSv(holder);

        // Individual status Rows
        for (int i = 0; i < status.getSatelliteCount(); i++) {
            svStatusRowBinding = LayoutSvStatusRowBinding.inflate(getLayoutInflater(),
                    mainBinding.svTable, true);
            svStatusRowBinding.setSv(new GnssSvStatusHolder(getApplicationContext(), status, i));
        }
    }

}
