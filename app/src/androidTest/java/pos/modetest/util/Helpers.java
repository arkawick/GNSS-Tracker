package pos.modetest.util;

import android.Manifest.permission;
import android.app.UiAutomation;
import android.content.Context;
import android.location.LocationManager;
import android.location.LocationRequest;

import androidx.test.platform.app.InstrumentationRegistry;

import static pos.modetest.util.HiddenMethods.*;

public class Helpers {
    public static final long DEFAULT_LOC_INTERVAL_MS = 0;
    public static final long DEFAULT_LOC_TIMEOUT_SHORT_MS = 5 * 1000;
    public static final long DEFAULT_LOC_TIMEOUT_LONG_MS = 30 * 1000;
    public static final long DEFAULT_LOC_MIN_TIME_MS = 0;
    public static final float DEFAULT_LOC_MIN_DISTANCE_M = 0;

    public static Context getAppContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    public static LocationManager getLocationManager() {
        return getAppContext().getSystemService(LocationManager.class);
    }

    public static void grantLocationPermissions() {
        UiAutomation uia = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        String packageName = getAppContext().getPackageName();
        uia.grantRuntimePermission(packageName, permission.ACCESS_COARSE_LOCATION);
        uia.grantRuntimePermission(packageName, permission.ACCESS_FINE_LOCATION);
        uia.grantRuntimePermission(packageName, permission.ACCESS_BACKGROUND_LOCATION);
    }

    public static void revokeLocationPermissions() {
        UiAutomation uia = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        String packageName = getAppContext().getPackageName();
        uia.grantRuntimePermission(packageName, permission.ACCESS_COARSE_LOCATION);
        uia.grantRuntimePermission(packageName, permission.ACCESS_FINE_LOCATION);
        uia.grantRuntimePermission(packageName, permission.ACCESS_BACKGROUND_LOCATION);
    }

    public static LocationRequest buildLocationRequest() {
        return (new LocationRequest.Builder(DEFAULT_LOC_INTERVAL_MS)).build();
    }

    public static LocationRequest buildLocationRequest(String provider) {
        return Call_locationRequestSetProvider(buildLocationRequest(), provider);
    }
}
