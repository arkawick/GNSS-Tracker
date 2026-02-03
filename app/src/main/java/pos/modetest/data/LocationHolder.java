package pos.modetest.data;

import static pos.modetest.utils.Constants.EMPTY_TEXT_3C;

import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import java.util.Locale;

import pos.modetest.utils.FormatUtils;

public class LocationHolder {

    @Nullable
    private Location mLocation;
    @Nullable
    private Long mSysElapsedRealTime;

    public LocationHolder(Location location) {
        setLocation(location);
    }

    public void setLocation(@Nullable Location location) {
        mLocation = location;
        mSysElapsedRealTime = location == null ? null : SystemClock.elapsedRealtimeNanos();
    }

    public Location getLocation() {
        return mLocation;
    }

    public String getLatitude() {
        if (mLocation == null) return EMPTY_TEXT_3C;
        return String.format(Locale.getDefault(), "%.08f", mLocation.getLatitude());
    }

    public String getLongitude() {
        if (mLocation == null) return EMPTY_TEXT_3C;
        return String.format(Locale.getDefault(), "%.08f", mLocation.getLongitude());
    }

    public String getAltitude() {
        if (mLocation == null || !mLocation.hasAltitude()) return EMPTY_TEXT_3C;
        return String.format(Locale.getDefault(), "%.02f", mLocation.getAltitude());
    }

    public String getAccuracy() {
        if (mLocation == null || !mLocation.hasAccuracy()) return EMPTY_TEXT_3C;
        return String.format(Locale.getDefault(), "%.04f", mLocation.getAccuracy());
    }

    public String getVerticalAccuracy() {
        if (mLocation == null || !mLocation.hasVerticalAccuracy()) return EMPTY_TEXT_3C;
        return String.format(Locale.getDefault(), "%.04f", mLocation.getVerticalAccuracyMeters());
    }

    public String getProvider() {
        if (mLocation == null) return EMPTY_TEXT_3C;
        Bundle extras = mLocation.getExtras();
        String extra = mLocation.getProvider();
        if (extras != null) {
            String type = extras.getString("networkLocationType", "");
            int svCount = extras.getInt("satellites", -1);
            if (!type.isEmpty()) {
                extra += "(" + type + ")";
            }
            if (svCount != -1) {
                extra += "(" + svCount + ")";
            }
        }
        return extra;
    }

    public String getSpeed() {
        if (mLocation == null || !mLocation.hasSpeed()) return EMPTY_TEXT_3C;
        return String.format(Locale.getDefault(), "%.04f",
                mLocation.getSpeed());
    }

    public String getBearing() {
        if (mLocation == null || !mLocation.hasBearing()) return EMPTY_TEXT_3C;
        return String.format(Locale.getDefault(), "%.02f",
                mLocation.getBearing());
    }

    public String getBearingAccuracy() {
        if (mLocation == null || !mLocation.hasBearingAccuracy()) return EMPTY_TEXT_3C;
        return String.format(Locale.getDefault(), "%.02f",
                mLocation.getBearingAccuracyDegrees());
    }

    public String getRealTime() {
        if (mLocation == null) return EMPTY_TEXT_3C;
        return FormatUtils.formatTimeDurationNanos(mLocation.getElapsedRealtimeNanos());
    }

    public String getSysRealTime() {
        if (mSysElapsedRealTime == null) return EMPTY_TEXT_3C;
        return FormatUtils.formatTimeDurationNanos(mSysElapsedRealTime);
    }

    public String getTime() {
        if (mLocation == null) return EMPTY_TEXT_3C;
        return FormatUtils.formatDateTime(mLocation.getTime());
    }
}
