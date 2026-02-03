package pos.modetest.utils;

import static pos.modetest.utils.Constants.TAG_PREFIX;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import pos.modetest.R;

public class HelperUtils {
    private static final String TAG = TAG_PREFIX + "HelperUtils";
    private static final String PREF_KEY_SHORTCUT = "addshortcut";
    private static final String ID_SHORTCUT_MAIN = "id1";
    private static final long EXEC_TIMEOUT_MS = 1000;

    public static void requestAddShortcut(Context context) {
        //noinspection deprecation
        var prefs = context.getSharedPreferences(
                PreferenceManager.getDefaultSharedPreferencesName(context), Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_KEY_SHORTCUT, false)) {
            return;
        }
        ShortcutManager sm = context.getSystemService(ShortcutManager.class);
        ShortcutInfo shortcut = new ShortcutInfo.Builder(context, ID_SHORTCUT_MAIN)
                .setShortLabel(context.getString(R.string.app_name))
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_app))
                .setIntent(IntentUtils.createShortcutLauncherIntent(context))
                .build();
        sm.requestPinShortcut(shortcut, null);
        prefs.edit().putBoolean(PREF_KEY_SHORTCUT, true).apply();
    }

    public static boolean checkFreqBand(double freqHz, double startFreqHz, double stopFreqHz,
                                        double tolerance) {
        return freqHz >= (startFreqHz - tolerance) && freqHz <= (stopFreqHz + tolerance);
    }

    public static boolean checkFreqBand(double freqHz, double centerFreqHz, double tolerance) {
        return Math.copySign(freqHz - centerFreqHz, 1.0) <= tolerance
                || (freqHz == centerFreqHz)
                || (Double.isNaN(freqHz) && Double.isNaN(centerFreqHz));
    }

    public static boolean checkPermissions(Context context, String[] permissions) {
        for (var p : permissions) {
            boolean granted = ContextCompat.checkSelfPermission(context, p)
                    == PackageManager.PERMISSION_GRANTED;
            if (granted)
                continue;
            return false;
        }
        return true;
    }

    public static String execCommandSync(String[] args) {
        StringBuilder buf = new StringBuilder();
        StringBuilder bufErr = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder(args);

        try {
            Process p = processBuilder.start();
            BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            p.waitFor(EXEC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            String line;
            while ((line = out.readLine()) != null) {
                buf.append(line).append("\n");
            }
            while ((line = err.readLine()) != null) {
                bufErr.append(line).append("\n");
            }
        } catch (Exception e) {
            Log.e(TAG, "execCommandSync Error: " + e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
        if (bufErr.length() > 0) {
            Log.w(TAG, "execCommandSync Error Stream not empty: " + bufErr);
            return "Error: " + bufErr;
        }
        return buf.toString();
    }
}
