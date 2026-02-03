/**
 * FILE:
 *      frameworks/base/core/java/android/widget/Chronometer.java
 * REVISION:
 *      7b1f850c50427ff38c20c83721c2e7aa173e62c2
 * CHANGES:
 *      - Reduce to a minimalistic implementation
 *      - Extend AppCompatTextView instead of TextView
 *      - Add milliseconds field
 *      - Update at native refresh rate
 */
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pos.modetest.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;

import java.util.Locale;

import pos.modetest.R;

/**
 * Class that implements a simple timer.
 * <p>
 * You can give it a start time in the {@link SystemClock#elapsedRealtime} timebase,
 * and it counts up from that, or if you don't give it a base time, it will use the
 * time at which you call {@link #start}.
 *
 * <p>The timer can also count downward towards the base time by
 * setting {@link #setCountDown(boolean)} to true.
 *
 * <p>By default it will display the current
 * timer value in the form "MM:SS.uuu" or "H:MM:SS.uuu".
 *
 * @attr ref R.styleable#Chronometer_format
 * @attr ref R.styleable#Chronometer_countDown
 */
public class Chronometer extends AppCompatTextView {
    private static final String TAG = "Chronometer";
    private final StringBuilder mRecycle = new StringBuilder(8);
    private long mBase;
    private boolean mVisible;
    private boolean mStarted;
    private boolean mRunning;
    private boolean mCountDown;
    private final Runnable mTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRunning) {
                updateText(SystemClock.elapsedRealtime());
                postOnAnimation(this);
            }
        }
    };

    /**
     * Initialize this Chronometer object.
     * Sets the base to the current time.
     */
    public Chronometer(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize with standard view layout information.
     * Sets the base to the current time.
     */
    public Chronometer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize with standard view layout information and style.
     * Sets the base to the current time.
     */
    public Chronometer(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Chronometer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Chronometer, defStyleAttr, defStyleRes);
        saveAttributeDataForStyleable(context, R.styleable.Chronometer, attrs, a, defStyleAttr, defStyleRes);
        setCountDown(a.getBoolean(R.styleable.Chronometer_countDown, false));
        a.recycle();

        init();
    }

    private void init() {
        mBase = SystemClock.elapsedRealtime();
        updateText(mBase);
    }

    /**
     * @return whether this view counts down
     * @see #setCountDown(boolean)
     */
    public boolean isCountDown() {
        return mCountDown;
    }

    /**
     * Set this view to count down to the base instead of counting up from it.
     *
     * @param countDown whether this view should count down
     * @see #setBase(long)
     */
    public void setCountDown(boolean countDown) {
        mCountDown = countDown;
        updateText(SystemClock.elapsedRealtime());
    }

    /**
     * Return the base time as set through {@link #setBase}.
     */
    public long getBase() {
        return mBase;
    }

    /**
     * Set the time that the count-up timer is in reference to.
     *
     * @param base Use the {@link SystemClock#elapsedRealtime} time base.
     */
    public void setBase(long base) {
        mBase = base;
        updateText(SystemClock.elapsedRealtime());
    }

    /**
     * Start counting up.  This does not affect the base as set from {@link #setBase}, just
     * the view display.
     * <p>
     * Chronometer works by regularly scheduling messages to the handler, even when the
     * Widget is not visible.  To make sure resource leaks do not occur, the user should
     * make sure that each start() call has a reciprocal call to {@link #stop}.
     */
    public void start() {
        mStarted = true;
        updateRunning();
    }

    /**
     * Stop counting up.  This does not affect the base as set from {@link #setBase}, just
     * the view display.
     * <p>
     * This stops the messages to the handler, effectively releasing resources that would
     * be held as the chronometer is running, via {@link #start}.
     */
    public void stop() {
        mStarted = false;
        updateRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mVisible = visibility == VISIBLE;
        updateRunning();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateRunning();
    }

    private synchronized void updateText(long now) {
        long elapsed = mCountDown ? mBase - now : now - mBase;
        long seconds = elapsed / 1000;
        long milliseconds = elapsed % 1000;
        boolean negative = false;
        if (seconds < 0) {
            seconds = -seconds;
            negative = true;
        }
        String text = DateUtils.formatElapsedTime(mRecycle, seconds);
        text += String.format(Locale.getDefault(), ".%03d", milliseconds);
        if (negative) {
            text = getResources().getString(R.string.negative_duration, text);
        }
        setText(text);
    }

    private void updateRunning() {
        boolean running = mVisible && mStarted && isShown();
        if (running != mRunning) {
            if (running) {
                updateText(SystemClock.elapsedRealtime());
                postOnAnimation(mTickRunnable);
            } else {
                removeCallbacks(mTickRunnable);
            }
            mRunning = running;
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return android.widget.Chronometer.class.getName();
    }
}
