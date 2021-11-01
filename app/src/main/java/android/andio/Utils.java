package android.andio;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Utils
{
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    public static String formatDateTime(final long dateTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime),
                                       ZoneId.systemDefault()).format(dateTimeFormatter);
    }

    public static String formatDuration(final int duration) {
        final int seconds = duration / 1000;
        final int minutes = seconds / 60;
        final int hours = minutes / 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private Utils() {}
}
