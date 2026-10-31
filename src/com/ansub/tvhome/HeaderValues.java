package com.ansub.tvhome;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Formatting shared by the status bar and its controls; no Android dependency. */
final class HeaderValues {
    static int percent(int value, int max) {
        return max <= 0 ? 0 : Math.round(Math.max(0, Math.min(max, value)) * 100f / max);
    }

    static int brightness(int percent) {
        // Never turn the panel fully black, even at the left end of the slider.
        return Math.max(1, Math.min(255, Math.round(percent * 255f / 100)));
    }

    static String date(long now, Locale locale, TimeZone zone) {
        Date date = new Date(now);
        return part("EEEE", date, locale, zone, true) + " " + part("d", date, locale, zone, false)
                + " " + part("MMMM", date, locale, zone, true) + " " + part("yyyy", date, locale, zone, false);
    }

    private static String part(String pattern, Date date, Locale locale, TimeZone zone, boolean capital) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        format.setTimeZone(zone);
        String text = format.format(date);
        return capital && !text.isEmpty() ? text.substring(0, 1).toUpperCase(locale) + text.substring(1) : text;
    }
}
