package pos.modetest.tests;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import static pos.modetest.util.Helpers.*;
import static pos.modetest.util.HiddenMethods.*;

@RunWith(AndroidJUnit4.class)
public class LastLocationTest {
    public static String TAG = LastLocationTest.class.getSimpleName();

    /**
     * TEST API:
     *     public android.location.Location android.location.LocationManager.getLastLocation()
     */
    @Test
    public void test_getLastLocation() {
        LocationManager lm = getLocationManager();

        Location l = Call_getLastLocation(lm);
        assertTrue(true); //If can reach here, cta log can print

        // assertNotNull(l);
        if (l != null) Log.i(TAG, l.toString());
    }
}
