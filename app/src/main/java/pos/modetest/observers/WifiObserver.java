package pos.modetest.observers;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.content.Context;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WifiObserver extends NetworkObserver {
    public static final String TAG = TAG_PREFIX + "WifiObs";
    private final Executor mExecutor;
    private final Listener mListener;

    public WifiObserver(@NonNull Context context, @NonNull Listener listener,
                        @Nullable Executor executor) {
        super(context, createWifiNetworkRequest());
        mListener = listener;
        mExecutor = Objects.requireNonNullElse(executor, Executors.newSingleThreadExecutor());
    }

    @Override
    public void onNetworksChanged(@NonNull Map<Network, NetworkCapabilities> networkMap) {
        Map<Network, WifiInfo> map = new HashMap<>();
        for (var network : networkMap.keySet()) {
            var networkCaps = networkMap.get(network);
            if (networkCaps != null && networkCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiInfo info = (WifiInfo) networkCaps.getTransportInfo();
                map.put(network, info);
            }
        }
        mExecutor.execute(() -> mListener.onWifiNetworksChanged(map));
    }

    public static NetworkRequest createWifiNetworkRequest() {
        return new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
    }

    public interface Listener {
        void onWifiNetworksChanged(@NonNull Map<Network, WifiInfo> wifiInfoMap);
    }
}
