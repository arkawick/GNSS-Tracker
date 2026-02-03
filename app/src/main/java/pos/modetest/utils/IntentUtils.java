package pos.modetest.utils;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.provider.Settings;

import java.util.Locale;

import pos.modetest.MainActivity;

public class IntentUtils {
    private static final String TAG = TAG_PREFIX + "IntentUtils";

    public static Intent createShortcutLauncherIntent(Context context) {
        return new Intent()
                .setAction(Intent.ACTION_MAIN)
                .setClass(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                .addCategory(Intent.CATEGORY_LAUNCHER);
    }

    public static Intent createAppDetailsSettingsIntent(Context context) {
        return new Intent()
                .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.getPackageName(), null))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent createNfwDetailsSettingsIntent(Context context) {
        String pkg = ConfigUtils.getNfwLocationPackage(context);
        return new Intent()
                .setAction(Intent.ACTION_APPLICATION_PREFERENCES)
                .setPackage(pkg)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent createAirplaneModeSettingsIntent() {
        return new Intent()
                .setAction(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent createLocationSettingsIntent() {
        return new Intent()
                .setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent createLocationViewingIntent(Location location) {
        return new Intent()
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(String.format(Locale.getDefault(),
                        "geo:%1$f,%2$f?q=%1$f,%2$f&z=17",
                        location.getLatitude(), location.getLongitude())))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent createLogalongLauncherIntent() {
        final String LOGALONG_PKG = "com.sonymobile.logalong";
        final String LOGALONG_MAIN_CLS = ".ui.MainActivity";
        return new Intent()
                .setAction(Intent.ACTION_MAIN)
                .setClassName(LOGALONG_PKG, LOGALONG_PKG + LOGALONG_MAIN_CLS)
                .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
    }

    public static Intent createRadioInfoSettingsIntent() {
        final String PHONE_PKG = "com.android.phone";
        final String RADIO_INFO_CLS = ".settings.RadioInfo";
        return new Intent()
                .setAction(Intent.ACTION_MAIN)
                .setClassName(PHONE_PKG, PHONE_PKG + RADIO_INFO_CLS)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent createGmsPlaystoreIntent() {
        final String PLAYSTORE_LINK = "https://play.google.com/store/apps/details";
        final String GMS_PKG = "com.google.android.gms";
        final String PKG_KEY = "id";

        final Uri uri = Uri.parse(PLAYSTORE_LINK).buildUpon()
                .appendQueryParameter(PKG_KEY, GMS_PKG)
                .build();

        return new Intent()
                .setAction(Intent.ACTION_VIEW)
                .setData(uri);
    }
}
