package pos.modetest.tests;

import android.location.Location;
import android.location.LocationManager;
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

import static pos.modetest.util.Helpers.*;
import static pos.modetest.util.HiddenMethods.*;

@RunWith(Parameterized.class)
public class LastKnownLocationTest {
    private static final String TAG = LastKnownLocationTest.class.getSimpleName();

    @Parameter
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
     *     public android.location.Location android.location.LocationManager.getLastKnownLocation(java.lang.String)
     */
    @Test
    public void test_getLastKnownLocation_1() {
        LocationManager lm = getLocationManager();

        Location l = lm.getLastKnownLocation(mProvider);
        assertTrue(true); //If can reach here, cta log can print

        // assertNotNull(l);
        if (l != null) Log.i(TAG, l.toString());
    }

    /**
     * TEST API:
     *     public android.location.Location android.location.LocationManager.getLastKnownLocation(java.lang.String,android.location.LastLocationRequest)
     */
    @Test
    public void test_getLastKnownLocation_2() {
        LocationManager lm = getLocationManager();

        Object lastLocationRequest = Call_buildLastLocationRequest();
        assumeNotNull(lastLocationRequest);

        Location l = Call_getLastKnownLocation(lm, mProvider, lastLocationRequest);
        assertTrue(true); //If can reach here, cta log can print

        // assertNotNull(l);
        if (l != null) Log.i(TAG, l.toString());
    }
}
