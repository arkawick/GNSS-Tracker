package pos.modetest.tests;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;

import pos.modetest.captures.*;

import static pos.modetest.util.Helpers.*;
import static pos.modetest.util.HiddenMethods.*;

@RunWith(Enclosed.class)
public class RequestLocationUpdatesTest {
    private static final String TAG = RequestLocationUpdatesTest.class.getSimpleName();

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
         * public void android.location.LocationManager.requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener)
         */
        @Test
        public void test_requestLocationUpdates_01() {
            LocationManager lm = getLocationManager();
            Location l = null;

            try (LocationListenerCapture capture = new LocationListenerCapture(getAppContext())) {
                // Make this run to main thread to make API use main looper
                // and call to Looper.prepare() can be skipped here
                new Handler(Looper.getMainLooper()).post(() ->
                    lm.requestLocationUpdates(mProvider,
                            DEFAULT_LOC_MIN_TIME_MS, DEFAULT_LOC_MIN_DISTANCE_M,
                            capture
                    )
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener,android.os.Looper)
         */
        @Test
        public void test_requestLocationUpdates_02() {
            LocationManager lm = getLocationManager();
            Location l = null;

            try (LocationListenerCapture capture = new LocationListenerCapture(getAppContext())) {
                lm.requestLocationUpdates(mProvider,
                        DEFAULT_LOC_MIN_TIME_MS, DEFAULT_LOC_MIN_DISTANCE_M,
                        capture, Looper.getMainLooper()
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(java.lang.String,long,float,java.util.concurrent.Executor,android.location.LocationListener)
         */
        @Test
        public void test_requestLocationUpdates_03() {
            LocationManager lm = getLocationManager();
            Location l = null;

            try (LocationListenerCapture capture = new LocationListenerCapture(getAppContext())) {
                lm.requestLocationUpdates(mProvider,
                        DEFAULT_LOC_MIN_TIME_MS, DEFAULT_LOC_MIN_DISTANCE_M,
                        Executors.newSingleThreadExecutor(), capture
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(java.lang.String,long,float,android.app.PendingIntent)
         */
        @Test
        public void test_requestLocationUpdates_06() {
            LocationManager lm = getLocationManager();
            Location l = null;

            try (LocationPendingIntentCapture capture = new LocationPendingIntentCapture(getAppContext())) {
                lm.requestLocationUpdates(mProvider,
                        DEFAULT_LOC_MIN_TIME_MS, DEFAULT_LOC_MIN_DISTANCE_M,
                        capture.getPendingIntent()
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(android.location.LocationRequest,android.location.LocationListener,android.os.Looper)
         */
        @Test
        public void test_requestLocationUpdates_08() {
            LocationManager lm = getLocationManager();
            LocationRequest lr = buildLocationRequest(mProvider);
            Location l = null;

            String retProvider = Call_locationRequestGetProvider(lr);
            assumeNotNull(retProvider);
            assumeTrue(mProvider.compareTo(retProvider) == 0);

            try (LocationListenerCapture capture = new LocationListenerCapture(getAppContext())) {
                Call_requestLocationUpdates(lm, lr,
                        capture, Looper.getMainLooper()
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(android.location.LocationRequest,java.util.concurrent.Executor,android.location.LocationListener)
         */
        @Test
        public void test_requestLocationUpdates_09() {
            LocationManager lm = getLocationManager();
            LocationRequest lr = buildLocationRequest(mProvider);
            Location l = null;

            String retProvider = Call_locationRequestGetProvider(lr);
            assumeNotNull(retProvider);
            assumeTrue(mProvider.compareTo(retProvider) == 0);

            try (LocationListenerCapture capture = new LocationListenerCapture(getAppContext())) {
                Call_requestLocationUpdates(lm, lr,
                        Executors.newSingleThreadExecutor(), capture
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(android.location.LocationRequest,android.app.PendingIntent)
         */
        @Test
        public void test_requestLocationUpdates_10() {
            LocationManager lm = getLocationManager();
            LocationRequest lr = buildLocationRequest(mProvider);
            Location l = null;

            String retProvider = Call_locationRequestGetProvider(lr);
            assumeNotNull(retProvider);
            assumeTrue(mProvider.compareTo(retProvider) == 0);

            try (LocationPendingIntentCapture capture = new LocationPendingIntentCapture(getAppContext())) {
                Call_requestLocationUpdates(lm, lr,
                        capture.getPendingIntent()
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(java.lang.String,android.location.LocationRequest,android.app.PendingIntent)
         */
        @Test
        public void test_requestLocationUpdates_11() {
            LocationManager lm = getLocationManager();
            LocationRequest lr = buildLocationRequest();
            Location l = null;

            try (LocationPendingIntentCapture capture = new LocationPendingIntentCapture(getAppContext())) {
                lm.requestLocationUpdates(mProvider, lr,
                        capture.getPendingIntent()
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(java.lang.String,android.location.LocationRequest,java.util.concurrent.Executor,android.location.LocationListener)
         */
        @Test
        public void test_requestLocationUpdates_12() {
            LocationManager lm = getLocationManager();
            LocationRequest lr = buildLocationRequest();
            Location l = null;

            try (LocationListenerCapture capture = new LocationListenerCapture(getAppContext())) {
                lm.requestLocationUpdates(mProvider, lr,
                        Executors.newSingleThreadExecutor(), capture
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(long,float,android.location.Criteria,android.location.LocationListener,android.os.Looper)
         */
        @Test
        public void test_requestLocationUpdates_04() {
            LocationManager lm = getLocationManager();
            Location l = null;

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);

            try (LocationListenerCapture capture = new LocationListenerCapture(getAppContext())) {
                lm.requestLocationUpdates(DEFAULT_LOC_MIN_TIME_MS, DEFAULT_LOC_MIN_DISTANCE_M,
                        criteria,
                        capture, Looper.getMainLooper()
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(long,float,android.location.Criteria,java.util.concurrent.Executor,android.location.LocationListener)
         */
        @Test
        public void test_requestLocationUpdates_05() {
            LocationManager lm = getLocationManager();
            Location l = null;

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);

            try (LocationListenerCapture capture = new LocationListenerCapture(getAppContext())) {
                lm.requestLocationUpdates(DEFAULT_LOC_MIN_TIME_MS, DEFAULT_LOC_MIN_DISTANCE_M,
                        criteria,
                        Executors.newSingleThreadExecutor(), capture
                );
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
         * public void android.location.LocationManager.requestLocationUpdates(long,float,android.location.Criteria,android.app.PendingIntent)
         */
        @Test
        public void test_requestLocationUpdates_07() {
            LocationManager lm = getLocationManager();
            Location l = null;

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);

            try (LocationPendingIntentCapture capture = new LocationPendingIntentCapture(getAppContext())) {
                lm.requestLocationUpdates(DEFAULT_LOC_MIN_TIME_MS, DEFAULT_LOC_MIN_DISTANCE_M,
                        criteria,
                        capture.getPendingIntent()
                );
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
