package pos.modetest.utils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.StringJoiner;

public class FormatUtils {
    public static String formatTimeDurationNanos(@IntRange(from = 0) long nanos) {
        Duration d = Duration.ofNanos(nanos);
        long days = d.toDaysPart();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        long millis = d.toMillisPart();
        StringJoiner b = new StringJoiner(" ");
        if (days > 0) {
            b.add(String.format(Locale.getDefault(), "%dd", days));
        }
        if (hours > 0) {
            b.add(String.format(Locale.getDefault(), "%dh", hours));
        }
        if (minutes > 0) {
            b.add(String.format(Locale.getDefault(), "%dm", minutes));
        }
        if (seconds > 0) {
            b.add(String.format(Locale.getDefault(), "%ds", seconds));
        }
        if (millis > 0) {
            b.add(String.format(Locale.getDefault(), "%dms", millis));
        }
        return b.toString();
    }

    public static String formatDateTime(@IntRange(from = 0) long millis) {
        return formatDateTime(new Date(millis));
    }

    public static String formatDateTime(@NonNull Date date) {
        String DATE_FMT = "yyyy/MM/dd HH:mm:ss Z";
        DateFormat fmt = new SimpleDateFormat(DATE_FMT, Locale.getDefault());
        return fmt.format(date);
    }
}
