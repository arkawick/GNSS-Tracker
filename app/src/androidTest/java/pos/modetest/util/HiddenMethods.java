package pos.modetest.util;

import android.app.PendingIntent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.CancellationSignal;
import android.os.Looper;

import org.junit.AssumptionViolatedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class HiddenMethods {
    private static final String CLASS_LAST_LOCATION_REQUEST = "android.location.LastLocationRequest";
    private static final String CLASS_LAST_LOCATION_REQUEST_BUILDER = "android.location.LastLocationRequest$Builder";
    private static final String METHOD_LAST_LOCATION_REQUEST_BUILDER_BUILD = "build";
    private static final String METHOD_LOCATION_REQUEST_SET_PROVIDER = "setProvider";
    private static final String METHOD_LOCATION_REQUEST_GET_PROVIDER = "getProvider";

    public static final String METHOD_GET_LAST_LOCATION = "getLastLocation";
    public static final String METHOD_GET_LAST_KNOWN_LOCATION = "getLastKnownLocation";
    public static final String METHOD_GET_CURRENT_LOCATION = "getCurrentLocation";
    public static final String METHOD_REQUEST_SINGLE_UPDATE = "requestSingleUpdate";
    public static final String METHOD_REQUEST_LOCATION_UPDATES = "requestLocationUpdates";

    public static Object Call_buildLastLocationRequest() {
        try {
            Class<?> lastLocationRequestBuilderClass = Class.forName(CLASS_LAST_LOCATION_REQUEST_BUILDER);
            Method buildMethod = lastLocationRequestBuilderClass.getMethod(METHOD_LAST_LOCATION_REQUEST_BUILDER_BUILD);
            Constructor<?> ctor = lastLocationRequestBuilderClass.getDeclaredConstructor();
            return buildMethod.invoke(ctor.newInstance());
        } catch (Exception e) {
            throw new AssumptionViolatedException("LastLocationRequest cannot be built", e);
        }
    }

    public static LocationRequest Call_locationRequestSetProvider(LocationRequest lr, String provider) {
        try {
            Method setProviderMethod = LocationRequest.class.getMethod(METHOD_LOCATION_REQUEST_SET_PROVIDER, String.class);
            return (LocationRequest) setProviderMethod.invoke(lr, provider);
        } catch (Exception e) {
            throw new AssumptionViolatedException("Cannot Set Provider", e);
        }
    }

    public static String Call_locationRequestGetProvider(LocationRequest lr) {
        try {
            Method getProviderMethod = LocationRequest.class.getMethod(METHOD_LOCATION_REQUEST_GET_PROVIDER);
            return (String) getProviderMethod.invoke(lr);
        } catch (Exception e) {
            throw new AssumptionViolatedException("Cannot Get Provider", e);
        }
    }

    public static Location Call_getLastLocation(LocationManager lm) {
        try {
            Method m = lm.getClass().getMethod(METHOD_GET_LAST_LOCATION);
            return (Location) m.invoke(lm);
        } catch (Exception e) {
            throw new AssumptionViolatedException("Method Unreachable", e);
        }
    }

    public static Location Call_getLastKnownLocation(LocationManager lm, String provider, Object lastLocationRequest) {
        try {
            Class<?> lastLocationRequestClass = Class.forName(CLASS_LAST_LOCATION_REQUEST);
            Method m = lm.getClass().getMethod(METHOD_GET_LAST_KNOWN_LOCATION, String.class, lastLocationRequestClass);
            return (Location) m.invoke(lm, provider, lastLocationRequest);
        } catch (Exception e) {
            throw new AssumptionViolatedException("Method Unreachable", e);
        }
    }

    public static void Call_getCurrentLocation(LocationManager lm, LocationRequest lr, CancellationSignal cs, Executor executor, Consumer<?> consumer) {
        try {
            Method m = lm.getClass().getMethod(METHOD_GET_CURRENT_LOCATION, LocationRequest.class, CancellationSignal.class, Executor.class, Consumer.class);
            m.invoke(lm, lr, cs, executor, consumer);
        } catch (Exception e) {
            throw new AssumptionViolatedException("Method Unreachable", e);
        }
    }

    public static void Call_requestLocationUpdates(LocationManager lm, LocationRequest lr, LocationListener listener, Looper looper) {
        try {
            Method m = lm.getClass().getMethod(METHOD_REQUEST_LOCATION_UPDATES, LocationRequest.class, LocationListener.class, Looper.class);
            m.invoke(lm, lr, listener, looper);
        } catch (Exception e) {
            throw new AssumptionViolatedException("Method Unreachable", e);
        }
    }

    public static void Call_requestLocationUpdates(LocationManager lm, LocationRequest lr, Executor executor, LocationListener listener) {
        try {
            Method m = lm.getClass().getMethod(METHOD_REQUEST_LOCATION_UPDATES, LocationRequest.class, Executor.class, LocationListener.class);
            m.invoke(lm, lr, executor, listener);
        } catch (Exception e) {
            throw new AssumptionViolatedException("Method Unreachable", e);
        }
    }

    public static void Call_requestLocationUpdates(LocationManager lm, LocationRequest lr, PendingIntent pi) {
        try {
            Method m = lm.getClass().getMethod(METHOD_REQUEST_LOCATION_UPDATES, LocationRequest.class, PendingIntent.class);
            m.invoke(lm, lr, pi);
        } catch (Exception e) {
            throw new AssumptionViolatedException("Method Unreachable", e);
        }
    }
}
