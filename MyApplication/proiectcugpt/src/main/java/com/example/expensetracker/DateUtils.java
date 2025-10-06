package com.example.expensetracker;

import java.util.Calendar;

public class DateUtils {
    public static long startOfDay(long timeMs) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMs);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long endOfDay(long timeMs) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMs);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    public static long[] getTodayRange() {
        long now = System.currentTimeMillis();
        return new long[]{startOfDay(now), endOfDay(now)};
    }

    public static long[] getThisWeekRange() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        long start = startOfDay(c.getTimeInMillis());
        c.add(Calendar.DAY_OF_WEEK, 6);
        long end = endOfDay(c.getTimeInMillis());
        return new long[]{start, end};
    }

    public static long[] getThisMonthRange() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        long start = startOfDay(c.getTimeInMillis());
        c.add(Calendar.MONTH, 1);
        c.add(Calendar.DAY_OF_MONTH, -1);
        long end = endOfDay(c.getTimeInMillis());
        return new long[]{start, end};
    }
}
