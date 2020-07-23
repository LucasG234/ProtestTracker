package com.lucasg234.protesttracker.mainactivity;

import android.content.Context;
import android.location.Location;

import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.util.LocationUtils;

import java.util.Comparator;
import java.util.Date;

/**
 * This class is used to create a custom sorting for Post objects
 */
public class FeedComparator implements Comparator<Post> {

    private static final int milliSecondsToMinutes = 60000;

    private final Context mContext;

    public FeedComparator(Context context) {
        super();
        this.mContext = context;
    }

    // Compare method considers both time since posted and distance from poster
    @Override
    public int compare(Post post1, Post post2) {
        Location currentLocation = LocationUtils.getCurrentLocation(mContext);
        Float metersTo1 = currentLocation.distanceTo(LocationUtils.toLocation(post1.getLocation()));
        Float metersTo2 = currentLocation.distanceTo(LocationUtils.toLocation(post2.getLocation()));

        Date currentTime = new Date();
        long minutesSince1 = (currentTime.getTime() - post1.getCreatedAt().getTime()) * milliSecondsToMinutes;
        long minutesSince2 = (currentTime.getTime() - post2.getCreatedAt().getTime()) * milliSecondsToMinutes;

        // Current comparison = one meter is equivalent to one minute
        return Long.compare(metersTo1.longValue() + minutesSince1, metersTo2.longValue() + minutesSince2);
    }

}
