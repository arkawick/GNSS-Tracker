/**
 * FILE:
 *      cts/tests/location/common/src/android/location/cts/common/GetCurrentLocationCapture.java
 * REVISION:
 *      824e0bca8bda5fa4340ecb2faeaa77b418d7029d
 * CHANGES:
 *      - Replace Package
 *      - Use androidx.annotation.Nullable instead of android.annotation.Nullable
 */
package pos.modetest.captures;

import android.location.Location;
import android.os.CancellationSignal;

import androidx.annotation.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class GetCurrentLocationCapture implements Consumer<Location>, AutoCloseable {

    private final CancellationSignal mCancellationSignal;
    private final CountDownLatch mLatch;
    private @Nullable Location mLocation;

    public GetCurrentLocationCapture() {
        mCancellationSignal = new CancellationSignal();
        mLatch = new CountDownLatch(1);
    }

    public CancellationSignal getCancellationSignal() {
        return mCancellationSignal;
    }

    public boolean hasLocation(long timeoutMs) throws InterruptedException {
        return mLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public @Nullable Location getLocation(long timeoutMs) throws InterruptedException, TimeoutException {
        if (mLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            return mLocation;
        } else {
            throw new TimeoutException("no location received before timeout");
        }
    }

    @Override
    public void accept(Location location) {
        if (mLatch.getCount() == 0) {
            throw new AssertionError("callback received multiple locations");
        }

        mLocation = location;
        mLatch.countDown();
    }

    @Override
    public void close() {
        mCancellationSignal.cancel();
    }
}
