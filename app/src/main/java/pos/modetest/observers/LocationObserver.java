package pos.modetest.observers;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GnssCapabilities;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementRequest;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LocationObserver implements IObserver {
    public static final String TAG = TAG_PREFIX + "LocObs";
    public static final int DEL_AD_DELAY_MS = 5000;

    private final Context mContext;
    private final LocationManager lm;

    private final Listener mListener;
    private final Executor mObserverExecutor;
    private final Executor mExecutor;

    // Must match with R.array.pos_qualities
    private static final List<Integer> QUALITY_LIST = List.of(
            LocationRequest.QUALITY_BALANCED_POWER_ACCURACY,
            LocationRequest.QUALITY_HIGH_ACCURACY,
            LocationRequest.QUALITY_LOW_POWER
    );

    private String mProvider;
    private LocationMode mMode;
    private int mQuality;
    private boolean mForceFullTrack;

    private static final String DELETE_AIDING_DATA_COMMAND = "delete_aiding_data";
    public static final String[] AIDING_DATA_OPTS = new String[]{
            "all",
            "ephemeris",
            "almanac",
            "position",
            "time",
            "iono",
            "utc",
            "health",
            "svdir",
            "svsteer",
            "sadata",
            "rti",
            "celldb-info",
    };

    public static final String[] AIDING_DATA_PRESET_COLD = new String[]{
            "all",
    };

    public static final String[] AIDING_DATA_PRESET_WARM = new String[]{
            "ephemeris",
            "utc",
    };

    public static final String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    private final LocationListener mLocationListener = new LocationListener() {
        private final Set<String> mLastEnabledProviders = new HashSet<>();

        @Override
        public void onLocationChanged(@Nullable Location location) {
            mExecutor.execute(() -> mListener.onLocationChanged(location));
        }

        private void onProvidersChanged() {
            var providers = lm.getProviders(true);
            if (!(mLastEnabledProviders.size() == providers.size() &&
                    mLastEnabledProviders.equals(new HashSet<>(providers)))) {
                mLastEnabledProviders.clear();
                mLastEnabledProviders.addAll(providers);
                mExecutor.execute(() -> mListener.onProvidersChanged(providers));
            }
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            onProvidersChanged();
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            onProvidersChanged();
        }
    };

    private final GnssStatus.Callback mGnssStatusCallback = new GnssStatus.Callback() {
        @Override
        @RequiresPermission(anyOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION})
        public void onStarted() {
            mExecutor.execute(mListener::onGnssStarted);
          // Call after location request triggers engine start. SUPL wasn't triggered otherwise.
            GnssMeasurementRequest request = (new GnssMeasurementRequest.Builder())
                    .setFullTracking(mForceFullTrack)
                    .build();
            lm.registerGnssMeasurementsCallback(request, mObserverExecutor, mGnssMeasurementsCallback);
            lm.registerGnssNavigationMessageCallback(mObserverExecutor, mGnssNavigationMessageCallback);
        }

        @Override
        public void onStopped() {
            mExecutor.execute(mListener::onGnssStopped);
        }

        @Override
        public void onFirstFix(int ttffMillis) {
            mExecutor.execute(() -> mListener.onFirstFix(ttffMillis));
        }

        @Override
        public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
            mExecutor.execute(() -> mListener.onSatelliteStatusChanged(status));
        }
    };

    private final GnssMeasurementsEvent.Callback mGnssMeasurementsCallback =
            new GnssMeasurementsEvent.Callback() {
        private boolean isFullTrackingLast = true;

        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
            boolean isFullTracking = getFullTracking(eventArgs);
            if (isFullTracking != isFullTrackingLast) {
                isFullTrackingLast = isFullTracking;
                mExecutor.execute(() -> mListener.onGnssFullTrackingChanged(isFullTracking));
            }
            mExecutor.execute(() -> mListener.onGnssMeasurementsReceived(eventArgs));
        }
    };

    private final GnssNavigationMessage.Callback mGnssNavigationMessageCallback =
            new GnssNavigationMessage.Callback() {
        @Override
        public void onGnssNavigationMessageReceived(GnssNavigationMessage message) {
            mExecutor.execute(() -> mListener.onGnssNavigationMessageReceived(message));
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case LocationManager.ACTION_GNSS_CAPABILITIES_CHANGED -> mObserverExecutor.execute(() ->
                        mExecutor.execute(() -> mListener.onGnssCapabilitiesChanged(
                                lm.getGnssCapabilities(),
                                lm.getGnssHardwareModelName(),
                                lm.getGnssYearOfHardware()
                        )));
                case LocationManager.MODE_CHANGED_ACTION -> mObserverExecutor.execute(() ->
                        mExecutor.execute(() ->
                                mListener.onLocationModeChanged(lm.isLocationEnabled())));
                case LocationManager.PROVIDERS_CHANGED_ACTION -> mObserverExecutor.execute(() -> {
                    String provider = intent.getStringExtra(
                            LocationManager.EXTRA_PROVIDER_NAME);
                    boolean enabled = intent.getBooleanExtra(
                            LocationManager.EXTRA_PROVIDER_ENABLED, false);
                    if (provider == null || provider.isEmpty()) return;
                    if (enabled) {
                        mLocationListener.onProviderEnabled(provider);
                    } else {
                        mLocationListener.onProviderDisabled(provider);
                    }
                });
            }
        }
    };

    public LocationObserver(@NonNull Context context, @NonNull Listener listener,
                            @Nullable Executor executor) {
        mContext = context;
        mListener = listener;
        mObserverExecutor = Executors.newSingleThreadExecutor();
        mExecutor = Objects.requireNonNullElse(executor, Executors.newSingleThreadExecutor());
        lm = context.getSystemService(LocationManager.class);

        // API/UX Defaults
        setForceFullTracking(false);
        setLocationOptions(LocationManager.GPS_PROVIDER, LocationMode.TRACK,
                QUALITY_LIST.get(0));
    }

    public boolean isProviderUnavailable(String provider) {
        return provider == null || !lm.hasProvider(provider) || !lm.isProviderEnabled(provider);
    }

    private boolean isQualityValid(int quality) {
        return QUALITY_LIST.contains(quality);
    }

    private boolean areOptionsInValid() {
        return isProviderUnavailable(mProvider)
                || mMode == null
                || !isQualityValid(mQuality);
    }

    public void setLocationOptions(String provider, LocationMode mode, int quality) {
        mProvider = provider;
        mMode = mode;
        mQuality = quality;
    }

    public String getProvider() {
        return mProvider;
    }

    public LocationMode getMode() {
        return mMode;
    }

    public int getQuality() {
        return mQuality;
    }

    public void setForceFullTracking(boolean enabled) {
        mForceFullTrack = enabled;
    }

    public void deleteAidingData(String[] opts) {
        if (areOptionsInValid()) {
            throw new IllegalStateException("Options are not set");
        }
        if (!LocationManager.GPS_PROVIDER.equals(mProvider)) {
            throw new IllegalStateException("Provider is not " + LocationManager.GPS_PROVIDER);
        }
        Bundle extras = new Bundle();
        Arrays.stream(opts).forEach(o -> extras.putBoolean(o, true));
        lm.sendExtraCommand(mProvider, DELETE_AIDING_DATA_COMMAND, extras);
    }

    @SuppressLint("ObsoleteSdkInt")
    @RequiresApi(api = Build.VERSION_CODES.R)
    public GnssCapabilities getGnssCapabilities() {
        return lm.getGnssCapabilities();
    }

    @RequiresPermission(anyOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    public void startObserving() {
        final IntentFilter filter = new IntentFilter();
        List.of(
                LocationManager.ACTION_GNSS_CAPABILITIES_CHANGED,
                LocationManager.MODE_CHANGED_ACTION,
                LocationManager.PROVIDERS_CHANGED_ACTION
        ).forEach(filter::addAction);
        ContextCompat.registerReceiver(mContext, mBroadcastReceiver,
                filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // initial calls
        mExecutor.execute(() -> mListener.onLocationModeChanged(lm.isLocationEnabled()));
        //noinspection DataFlowIssue
        mExecutor.execute(() -> mLocationListener.onProviderEnabled(null));
        mExecutor.execute(() -> mListener.onGnssCapabilitiesChanged(
                lm.getGnssCapabilities(),
                lm.getGnssHardwareModelName(),
                lm.getGnssYearOfHardware()
        ));
    }

    @RequiresPermission(anyOf = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION})
    public void startLocating() {
        if (areOptionsInValid()) {
            throw new IllegalStateException("Options are not set");
        }

        switch (mMode) {
            case SINGLE, TRACK -> {
                LocationRequest lr = (new LocationRequest.Builder(0))
                        .setMaxUpdates(mMode == LocationMode.SINGLE ? 1 : Integer.MAX_VALUE)
                        .setQuality(mQuality)
                        .build();

                lm.requestLocationUpdates(mProvider, lr,
                        mObserverExecutor, mLocationListener);
                lm.registerGnssStatusCallback(mObserverExecutor, mGnssStatusCallback);
            }
            case LAST -> {
                final Location loc = lm.getLastKnownLocation(mProvider);
                //noinspection DataFlowIssue
                mObserverExecutor.execute(() -> mLocationListener.onLocationChanged(loc));
            }
        }
    }

    public void stopLocating() {
        switch (mMode) {
            case SINGLE, TRACK -> {
                lm.removeUpdates(mLocationListener);
                if (LocationManager.GPS_PROVIDER.equals(mProvider)) {
                    lm.unregisterGnssNavigationMessageCallback(mGnssNavigationMessageCallback);
                    lm.unregisterGnssMeasurementsCallback(mGnssMeasurementsCallback);
                    lm.unregisterGnssStatusCallback(mGnssStatusCallback);
                }
            }
            case LAST -> {
                // Do nothing
            }
        }
    }

    public void stopObserving() {
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    public enum LocationMode {
        SINGLE,
        TRACK,
        LAST
    }

    public static boolean getFullTracking(@Nullable GnssMeasurementsEvent eventArgs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && eventArgs != null) {
            return eventArgs.hasIsFullTracking() && eventArgs.isFullTracking();
        }
        return true; // default
    }

    public static int getMultipathSvCount(@Nullable GnssMeasurementsEvent eventArgs) {
        if (eventArgs == null) return 0;
        return (int) eventArgs.getMeasurements().stream()
                .filter(m -> m.getMultipathIndicator()
                        == GnssMeasurement.MULTIPATH_INDICATOR_DETECTED)
                .count();
    }

    public static double getAgcAverage(@Nullable GnssMeasurementsEvent eventArgs) {
        if (eventArgs == null) return Double.NaN;
        var agcList = Arrays.asList(eventArgs.getMeasurements().stream()
                .filter(GnssMeasurement::hasAutomaticGainControlLevelDb)
                .map(GnssMeasurement::getAutomaticGainControlLevelDb)
                .toArray(Double[]::new));
        return agcList.stream().reduce(Double::sum).orElse(0d) / agcList.size();
    }

    public interface Listener {
        void onLocationModeChanged(boolean enabled);
        void onProvidersChanged(@NonNull List<String> enabledProviders);
        void onGnssCapabilitiesChanged(@NonNull GnssCapabilities caps, String hwModel, int yearOfHw);
        void onLocationChanged(@Nullable Location location);
        void onGnssStarted();
        void onFirstFix(int ttffMillis);
        void onSatelliteStatusChanged(@NonNull GnssStatus status);
        void onGnssMeasurementsReceived(@NonNull GnssMeasurementsEvent eventArgs);
        void onGnssNavigationMessageReceived(@NonNull GnssNavigationMessage message);
        void onGnssFullTrackingChanged(boolean enabled);
        void onGnssStopped();
    }

    public abstract static class DefaultListener implements Listener {
        @Override
        public void onLocationModeChanged(boolean enabled) {
            Log.d(TAG, String.format("onLocationModeChanged(%s)", enabled ? "enabled" : "disabled"));
        }

        @Override
        public void onProvidersChanged(@NonNull List<String> enabledProviders) {
            Log.d(TAG, String.format("onProvidersChanged(%s)",
                    String.join("|", enabledProviders)));
        }

        @Override
        public void onGnssCapabilitiesChanged(@NonNull GnssCapabilities caps,
                                              String hwModel, int yearOfHw) {
            Log.d(TAG, String.format("onGnssCapabilitiesChanged %s (%d) %s",
                    hwModel, yearOfHw, caps));
        }

        @Override
        public abstract void onLocationChanged(@Nullable Location location);

        @Override
        public void onGnssStarted() {
            Log.d(TAG, "onGnssStarted");
        }

        @Override
        public void onFirstFix(int ttffMillis) {
            Log.d(TAG, String.format("onGnssNavigationMessageReceived(%dms)", ttffMillis));
        }

        @Override
        public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
            Log.d(TAG, String.format("onSatelliteStatusChanged(%s)", status));
        }

        @Override
        public void onGnssMeasurementsReceived(@NonNull GnssMeasurementsEvent eventArgs) {
            Log.d(TAG, String.format("onGnssMeasurementsReceived(%s)", eventArgs));
        }

        @Override
        public void onGnssNavigationMessageReceived(@NonNull GnssNavigationMessage message) {
            Log.d(TAG, String.format("onGnssNavigationMessageReceived(%s)", message));
        }

        @Override
        public void onGnssFullTrackingChanged(boolean enabled) {
            Log.d(TAG, String.format("onGnssFullTrackingChanged(%s)", enabled));
        }

        @Override
        public void onGnssStopped() {
            Log.d(TAG, "onGnssStopped");
        }
    }
}
