package pos.modetest.observers;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.Manifest;
import android.content.Context;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TelephonyNetworkSpecifier;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DataObserver extends NetworkObserver {
    public static final String TAG = TAG_PREFIX + "DataObs";
    private final Executor mExecutor;
    private final Listener mListener;
    private final SubscriptionManager sm;

    public DataObserver(@NonNull Context context, @NonNull Listener listener,
                        @Nullable Executor executor) {
        super(context, createMobileNetworkRequest());
        mListener = listener;
        mExecutor = Objects.requireNonNullElse(executor, Executors.newSingleThreadExecutor());
        sm = context.getSystemService(SubscriptionManager.class);
    }

    @Override
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public void onNetworksChanged(@NonNull Map<Network, NetworkCapabilities> networkMap) {
        Map<Network, SubscriptionInfo> map = new HashMap<>();
        for (var network : networkMap.keySet()) {
            var networkCaps = networkMap.get(network);
            if (networkCaps != null && networkCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                var spec = (TelephonyNetworkSpecifier) networkCaps.getNetworkSpecifier();
                if (spec != null) {
                    var subInfo = sm.getActiveSubscriptionInfo(spec.getSubscriptionId());
                    if (subInfo != null) {
                        map.put(network, subInfo);
                    }
                }
            }
        }
        mExecutor.execute(() -> mListener.onDataNetworksChanged(map));
    }

    public static NetworkRequest createMobileNetworkRequest() {
        return new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();
    }

    public interface Listener {
        void onDataNetworksChanged(@NonNull Map<Network, SubscriptionInfo> dataInfoMap);
    }
}
