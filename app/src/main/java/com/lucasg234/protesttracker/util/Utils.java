package com.lucasg234.protesttracker.util;

import android.text.format.DateUtils;

import java.util.Date;

/**
 * Utility class which holds generally useful methods
 */
public class Utils {

    // Converts a Date object to a relative string based off of the current time
    public static String dateToRelative(Date date) {
        return String.valueOf(DateUtils.getRelativeTimeSpanString(date.getTime(),
                System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_TIME));
    }
}
