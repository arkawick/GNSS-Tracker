package pos.modetest.tests;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;

import pos.modetest.captures.*;

import static pos.modetest.util.Helpers.*;
import static pos.modetest.util.HiddenMethods.*;

@RunWith(Parameterized.class)
public class GetCurrentLocationTest {
    private static final String TAG = GetCurrentLocationTest.class.getSimpleName();

    @Parameter()
    public String mProvider;

    @Parameters(name = "{index}-{0}")
    public static Collection<Object[]> initParameters() {
        return Arrays.asList(new Object[][] {
                { LocationManager.FUSED_PROVIDER    },
                { LocationManager.GPS_PROVIDER      },
                { LocationManager.NETWORK_PROVIDER  },
                { LocationManager.PASSIVE_PROVIDER  },
        });
    }

    /**
     * TEST API:
     *     public void android.location.LocationManager.getCurrentLocation(java.lang.String,android.os.CancellationSignal,java.util.concurrent.Executor,java.util.function.Consumer)
     */
    @Test
    public void test_getCurrentLocation_1() {
        LocationManager lm = getLocationManager();
        Location l = null;

        try (GetCurrentLocationCapture capture = new GetCurrentLocationCapture()) {
            lm.getCurrentLocation(mProvider, capture.getCancellationSignal(),
                    Executors.newSingleThreadExecutor(), capture);
            assertTrue(true); //If can reach here, cta log can print
            l = capture.getLocation(DEFAULT_LOC_TIMEOUT_SHORT_MS);
        } catch (Exception e) {
            Log.e(TAG, "Cannot Get Location", e);
        }

        // assertNotNull(l);
        if (l != null) Log.i(TAG, l.toString());
    }

    /**
     * TEST API:
     *     public void android.location.LocationManager.getCurrentLocation(android.location.LocationRequest,android.os.CancellationSignal,java.util.concurrent.Executor,java.util.function.Consumer)
     */
    @Test
    public void test_getCurrentLocation_2() {
        LocationManager lm = getLocationManager();
        LocationRequest lr = buildLocationRequest(mProvider);
        Location l = null;

        try (GetCurrentLocationCapture capture = new GetCurrentLocationCapture()) {
            Call_getCurrentLocation(lm, lr, capture.getCancellationSignal(),
                    Executors.newSingleThreadExecutor(), capture);
            assertTrue(true); //If can reach here, cta log can print
            l = capture.getLocation(DEFAULT_LOC_TIMEOUT_SHORT_MS);
        } catch (Exception e) {
            Log.e(TAG, "Cannot get location", e);
        }

        // assertNotNull(l);
        if (l != null) Log.i(TAG, l.toString());
    }

    /**
     * TEST API:
     *     public void android.location.LocationManager.getCurrentLocation(java.lang.String,android.location.LocationRequest,android.os.CancellationSignal,java.util.concurrent.Executor,java.util.function.Consumer)
     */
    @Test
    public void test_getCurrentLocation_3() {
        LocationManager lm = getLocationManager();
        LocationRequest lr = buildLocationRequest(mProvider);
        Location l = null;

        String retProvider = Call_locationRequestGetProvider(lr);
        assumeNotNull(retProvider);
        assumeTrue(mProvider.compareTo(retProvider) == 0);

        try (GetCurrentLocationCapture capture = new GetCurrentLocationCapture()) {
            lm.getCurrentLocation(mProvider, lr, capture.getCancellationSignal(),
                    Executors.newSingleThreadExecutor(), capture);
            assertTrue(true); //If can reach here, cta log can print
            l = capture.getLocation(DEFAULT_LOC_TIMEOUT_SHORT_MS);
        } catch (Exception e) {
            Log.e(TAG, "Cannot get location", e);
        }

        // assertNotNull(l);
        if (l != null) Log.i(TAG, l.toString());
    }
}
