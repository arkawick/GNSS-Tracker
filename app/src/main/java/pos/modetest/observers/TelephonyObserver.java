package pos.modetest.observers;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.Manifest;
import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TelephonyObserver implements IObserver {
    public static final String TAG = TAG_PREFIX + "TelObs";
    private static final int INVALID_SUB_ID = -1;

    private final Executor mObserveExecutor;
    private final Executor mExecutor;
    private final Listener mListener;
    private final TelephonyManager tm;
    private final SubscriptionManager sm;

    private final List<Integer> mSlotStatus;
    private final Map<Integer, SubscriptionInfo> mSubInfoMap;
    private final List<TelephonyCb> mTelephonyCbs;

    public static final String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
    };

    private final SubscriptionManager.OnSubscriptionsChangedListener subsChangedListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
                public void onSubscriptionsChanged() {
                    checkAndUpdateSubCbs();
                }
            };

    public TelephonyObserver(@NonNull Context context, @NonNull Listener listener,
                             @Nullable Executor executor) {
        mObserveExecutor = Executors.newSingleThreadExecutor();
        mExecutor = Objects.requireNonNullElse(executor, Executors.newSingleThreadExecutor());
        mListener = listener;
        tm = context.getSystemService(TelephonyManager.class);
        sm = context.getSystemService(SubscriptionManager.class);
        mSubInfoMap = new HashMap<>();

        int count = tm.getSupportedModemCount();
        mSlotStatus = new ArrayList<>(count);
        mTelephonyCbs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            mSlotStatus.add(i, INVALID_SUB_ID);
            mTelephonyCbs.add(i, null);
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public void startObserving() {
        sm.addOnSubscriptionsChangedListener(mObserveExecutor, subsChangedListener);
    }

    public int getDefaultSubId() {
        return tm.getSubscriptionId();
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION})
    public boolean isRoaming(int subId) {
        var ss = tm.createForSubscriptionId(subId).getServiceState();
        if (ss != null) {
            return ss.getRoaming();
        }
        return false;
    }

    public void stopObserving() {
        sm.removeOnSubscriptionsChangedListener(subsChangedListener);
        for (int i = 0; i < mTelephonyCbs.size(); i++) {
            mSlotStatus.set(i, INVALID_SUB_ID);
            mTelephonyCbs.set(i, null);
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private void checkAndUpdateSubCbs() {
        boolean overallChanged = false;
        for (int i = 0; i < mSlotStatus.size(); i++) {
            boolean changed = false;
            var sub = sm.getActiveSubscriptionInfoForSimSlotIndex(i);
            int finalI = i;
            int curSub = mSlotStatus.get(i);
            if (sub == null) {
                // subId changed for slot
                if (curSub != INVALID_SUB_ID) {
                    changed = true;
                    // remove sub
                    mSubInfoMap.remove(i);
                    mSlotStatus.set(i, INVALID_SUB_ID);
                    mTelephonyCbs.set(i, null);
                    mExecutor.execute(() -> mListener.onSubscriptionChanged(finalI, null));
                }
            } else {
                int subId = sub.getSubscriptionId();
                // subId changed for slot
                if (subId != curSub) {
                    changed = true;
                    mSubInfoMap.put(i, sub);
                    // update out sub id for the slot
                    mSlotStatus.set(i, subId);
                    // create callback for new sub
                    var newCb = new TelephonyCb(sub);
                    mTelephonyCbs.set(i, newCb);
                    tm.createForSubscriptionId(subId)
                            .registerTelephonyCallback(mObserveExecutor, newCb);
                    mExecutor.execute(() -> mListener.onSubscriptionChanged(finalI, sub));
                }
            }
            overallChanged |= changed;
            Log.d(TAG, String.format("checkAndUpdateSubCbs(%d) -> %s (%s)",
                    i, mSlotStatus.get(i), changed));
        }
        if (overallChanged) {
            mExecutor.execute(() -> mListener.onSubscriptionsChanged(mSubInfoMap));
        }
    }

    public interface Listener {
        void onSubscriptionChanged(int slot, @Nullable SubscriptionInfo subInfo);

        void onSubscriptionsChanged(@NonNull Map<Integer, SubscriptionInfo> subInfoMap);

        void onCellInfoChanged(@NonNull SubscriptionInfo subInfo, @NonNull List<CellInfo> cellInfo);

        void onDisplayInfoChanged(@NonNull SubscriptionInfo subInfo, @NonNull TelephonyDisplayInfo telephonyDisplayInfo);

        void onDataActivity(@NonNull SubscriptionInfo subInfo, int direction);
    }

    @NonNull
    public static String getNetworkTypeString(int networkType, int overrideNetworkType) {
        return switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS";
            case TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA";
            case TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A";
            case TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT";
            case TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA";
            case TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN";
            case TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B";
            case TelephonyManager.NETWORK_TYPE_LTE -> switch (overrideNetworkType) {
                case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "LTE_CA";
                case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "LTE_ADV_PRO";
                case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "NR_NSA";
                case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "NR_ADVANCED";
                default -> "LTE";
            };
            case TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD";
            case TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPAP";
            case TelephonyManager.NETWORK_TYPE_GSM -> "GSM";
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA";
            case TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN";
            case TelephonyManager.NETWORK_TYPE_NR -> "NR";
            default -> "UNKNOWN";
        };
    }

    @NonNull
    public static String getDataActivityString(int direction) {
        return switch (direction) {
            case TelephonyManager.DATA_ACTIVITY_IN -> " ↓";
            case TelephonyManager.DATA_ACTIVITY_OUT -> "↑ ";
            case TelephonyManager.DATA_ACTIVITY_INOUT -> "⇅";
            case TelephonyManager.DATA_ACTIVITY_DORMANT -> "~";
            default -> "-"; // -
        };
    }

    private class TelephonyCb extends TelephonyCallback implements
            TelephonyCallback.CellInfoListener,
            TelephonyCallback.DataActivityListener,
            TelephonyCallback.ServiceStateListener,
            TelephonyCallback.DisplayInfoListener {
        private final SubscriptionInfo mSubInfo;

        public TelephonyCb(@NonNull SubscriptionInfo subInfo) {
            super();
            mSubInfo = subInfo;
        }

        @Override
        public void onCellInfoChanged(@NonNull List<CellInfo> cellInfo) {
            mExecutor.execute(() -> mListener.onCellInfoChanged(mSubInfo, cellInfo));
        }

        @Override
        public void onDisplayInfoChanged(@NonNull TelephonyDisplayInfo telephonyDisplayInfo) {
            mExecutor.execute(() -> mListener.onDisplayInfoChanged(mSubInfo, telephonyDisplayInfo));
        }

        @Override
        public void onDataActivity(int direction) {
            mListener.onDataActivity(mSubInfo, direction);
        }

        @Override
        public void onServiceStateChanged(@NonNull ServiceState serviceState) {
            Log.d(TAG, String.format(Locale.getDefault(), "onServiceStateChanged(%s)",
                    serviceState));
            // noinspection MissingPermission
            tm.requestCellInfoUpdate(mObserveExecutor, new TelephonyManager.CellInfoCallback() {
                @Override
                public void onCellInfo(@NonNull List<CellInfo> cellInfo) {
                    onCellInfoChanged(cellInfo);
                }
            });
        }
    }
}
