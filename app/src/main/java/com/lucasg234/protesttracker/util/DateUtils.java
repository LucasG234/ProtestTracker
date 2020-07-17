package com.lucasg234.protesttracker.util;

import java.util.Date;

/**
 * Utility class which holds useful methods modifying date objects
 */
public class DateUtils {

    // Converts a Date object to a relative string based off of the current time
    public static String dateToRelative(Date date) {
        return String.valueOf(android.text.format.DateUtils.getRelativeTimeSpanString(date.getTime(),
                System.currentTimeMillis(), 0L, android.text.format.DateUtils.FORMAT_ABBREV_TIME));
    }
}
