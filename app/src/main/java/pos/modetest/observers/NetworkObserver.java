package pos.modetest.observers;

import static android.net.ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import java.util.HashMap;
import java.util.Map;

public abstract class NetworkObserver implements IObserver {
    public static final String TAG = TAG_PREFIX + "NetworkObserver";

    private final NetworkRequest mRequest;
    private final ConnectivityManager cm;

    private final Map<Network, NetworkCapabilities> mActiveNetworkMap;

    public static final String[] permissions = new String[]{
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    private final ConnectivityManager.NetworkCallback networkCallback
            = new ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            mActiveNetworkMap.put(network, networkCapabilities);
            onNetworksChanged(new HashMap<>(mActiveNetworkMap));
        }

        @Override
        public void onLost(@NonNull Network network) {
            mActiveNetworkMap.remove(network);
            onNetworksChanged(new HashMap<>(mActiveNetworkMap));
        }

        @Override
        public void onUnavailable() {
            mActiveNetworkMap.clear();
            onNetworksChanged(new HashMap<>(mActiveNetworkMap));
        }
    };

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public NetworkObserver(@NonNull Context context, @NonNull NetworkRequest request) {
        mRequest = request;
        cm = context.getSystemService(ConnectivityManager.class);
        mActiveNetworkMap = new HashMap<>();
    }

    public Network getActiveNetwork() {
        return cm.getActiveNetwork();
    }
    public Map<Network, NetworkCapabilities> getAllActiveNetworks() {
        return mActiveNetworkMap;
    }

    public void startObserving() {
        cm.registerNetworkCallback(mRequest, networkCallback);
    }

    public void stopObserving() {
        cm.unregisterNetworkCallback(networkCallback);
    }

    public abstract void onNetworksChanged(@NonNull Map<Network, NetworkCapabilities> networkMap);
}
