package pos.modetest.tests;

import android.location.LocationManager;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.lang.reflect.Method;

import static pos.modetest.util.Helpers.*;
import static pos.modetest.util.HiddenMethods.*;

@RunWith(AndroidJUnit4.class)
public class ApiMethodsTest {
    private static final String TAG = ApiMethodsTest.class.getSimpleName();

    /**
     * Test whether all methods except are accessible except getLastLocation
     */
    @Test
    public void test_methods() {
        LocationManager lm = getAppContext().getSystemService(LocationManager.class);
        Method[] methods = lm.getClass().getMethods();

        assumeNotNull((Object) methods);
        assumeFalse(methods.length == 0);

        int getLastLocationCount = 0;
        int getLastKnownLocationCount = 0;
        int getCurrentLocationCount = 0;
        int requestSingleUpdateCount = 0;
        int requestLocationUpdatesCount = 0;
        for (Method m : methods) {
            String name = m.getName();
            if (name.matches(METHOD_GET_LAST_LOCATION)) {
                getLastLocationCount += 1;
                Log.i(TAG, m.toString());
            } else if (name.matches(METHOD_GET_LAST_KNOWN_LOCATION)) {
                getLastKnownLocationCount += 1;
                Log.i(TAG, m.toString());
            } else if (name.matches(METHOD_GET_CURRENT_LOCATION)) {
                getCurrentLocationCount += 1;
                Log.i(TAG, m.toString());
            } else if (name.matches(METHOD_REQUEST_SINGLE_UPDATE)) {
                requestSingleUpdateCount += 1;
                Log.i(TAG, m.toString());
            } else if (name.matches(METHOD_REQUEST_LOCATION_UPDATES)) {
                requestLocationUpdatesCount += 1;
                Log.i(TAG, m.toString());
            }
        }

        assertEquals(getLastLocationCount, 0); // 0 since getLastLocation is hidden
        assertEquals(getLastKnownLocationCount, 2);
        assertEquals(getCurrentLocationCount, 3);
        assertEquals(requestSingleUpdateCount, 4);
        assertEquals(requestLocationUpdatesCount, 12);
    }
}
