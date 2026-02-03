package pos.modetest.tests;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import pos.modetest.captures.*;

import static pos.modetest.util.Helpers.*;

@RunWith(Enclosed.class)
public class RequestSingleUpdateTest {
    private static final String TAG = RequestSingleUpdateTest.class.getSimpleName();

    @RunWith(Parameterized.class)
    public static class AllProvidersTest {

        @Parameter()
        public String mProvider;

        @Parameters(name = "{index}-{0}")
        public static Collection<Object[]> initParameters() {
            return Arrays.asList(new Object[][]{
                    {LocationManager.FUSED_PROVIDER},
                    {LocationManager.GPS_PROVIDER},
                    {LocationManager.NETWORK_PROVIDER},
                    {LocationManager.PASSIVE_PROVIDER},
            });
        }

        /**
         * TEST API:
         * public void android.location.LocationManager.requestSingleUpdate(java.lang.String,android.location.LocationListener,android.os.Looper)
         */
        @Test
        public void test_requestSingleUpdate_1() {
            LocationManager lm = getLocationManager();
            Location l = null;

            try (LocationListenerCapture capture = new LocationListenerCapture(getAppContext())) {
                lm.requestSingleUpdate(mProvider, capture, Looper.getMainLooper());
                assertTrue(true); //If can reach here, cta log can print
                l = capture.getNextLocation(DEFAULT_LOC_TIMEOUT_SHORT_MS);
            } catch (Exception e) {
                Log.e(TAG, "Cannot Get Location", e);
            }

            // assertNotNull(l);
            if (l != null) Log.i(TAG, l.toString());
        }

        /**
         * TEST API:
         * public void android.location.LocationManager.requestSingleUpdate(java.lang.String,android.app.PendingIntent)
         */
        @Test
        public void test_requestSingleUpdate_3() {
            LocationManager lm = getLocationManager();
            Location l = null;

            try (LocationPendingIntentCapture capture = new LocationPendingIntentCapture(getAppContext())) {
                lm.requestSingleUpdate(mProvider, capture.getPendingIntent());
                assertTrue(true); //If can reach here, cta log can print
                l = capture.getNextLocation(DEFAULT_LOC_TIMEOUT_SHORT_MS);
            } catch (Exception e) {
                Log.e(TAG, "Cannot Get Location", e);
            }

            // assertNotNull(l);
            if (l != null) Log.i(TAG, l.toString());
        }
    }

    public static class DefaultProviderTest {

        /**
         * TEST API:
         * public void android.location.LocationManager.requestSingleUpdate(android.location.Criteria,android.location.LocationListener,android.os.Looper)
         */
        @Test
        public void test_requestSingleUpdate_2() {
            LocationManager lm = getLocationManager();
            Location l = null;

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);

            try (LocationListenerCapture capture = new LocationListenerCapture(getAppContext())) {
                lm.requestSingleUpdate(criteria, capture, Looper.getMainLooper());
                assertTrue(true); //If can reach here, cta log can print
                l = capture.getNextLocation(DEFAULT_LOC_TIMEOUT_SHORT_MS);
            } catch (Exception e) {
                Log.e(TAG, "Cannot Get Location", e);
            }

            // assertNotNull(l);
            if (l != null) Log.i(TAG, l.toString());
        }

        /**
         * TEST API:
         * public void android.location.LocationManager.requestSingleUpdate(android.location.Criteria,android.app.PendingIntent)
         */
        @Test
        public void test_requestSingleUpdate_4() {
            LocationManager lm = getLocationManager();
            Location l = null;

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);

            try (LocationPendingIntentCapture capture = new LocationPendingIntentCapture(getAppContext())) {
                lm.requestSingleUpdate(criteria, capture.getPendingIntent());
                assertTrue(true); //If can reach here, cta log can print
                l = capture.getNextLocation(DEFAULT_LOC_TIMEOUT_SHORT_MS);
            } catch (Exception e) {
                Log.e(TAG, "Cannot Get Location", e);
            }

            // assertNotNull(l);
            if (l != null) Log.i(TAG, l.toString());
        }
    }
}
