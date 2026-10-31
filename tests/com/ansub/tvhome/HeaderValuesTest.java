package com.ansub.tvhome;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class HeaderValuesTest {
    public static void main(String[] args) {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar time = Calendar.getInstance(utc);
        time.clear();
        time.set(2026, Calendar.OCTOBER, 31, 23, 30);
        equal("Sabato 31 Ottobre 2026", HeaderValues.date(time.getTimeInMillis(), Locale.ITALIAN, utc));
        equal("Domenica 1 Novembre 2026", HeaderValues.date(time.getTimeInMillis(), Locale.ITALIAN,
                TimeZone.getTimeZone("Europe/Rome")));
        equal("Saturday 31 October 2026", HeaderValues.date(time.getTimeInMillis(), Locale.UK, utc));
        equal(0, HeaderValues.percent(10, 0));
        equal(0, HeaderValues.percent(-1, 100));
        equal(100, HeaderValues.percent(200, 100));
        equal(50, HeaderValues.percent(5, 10));
        equal(1, HeaderValues.brightness(0));
        equal(255, HeaderValues.brightness(100));
        equal(255, HeaderValues.brightness(999));
        System.out.println("HeaderValues: 10 checks passed");
    }
    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
