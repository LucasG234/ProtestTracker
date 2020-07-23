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

    private static final double milliSecondsToMinutes = 1.66667e-5;

    private final Context mContext;

    public FeedComparator(Context context) {
        super();
        this.mContext = context;
    }

    // Compare method considers both time since posted and distance from poster
    @Override
    public int compare(Post post1, Post post2) {
        Location currentLocation = LocationUtils.getCurrentLocation(mContext);
        double metersTo1 = currentLocation.distanceTo(LocationUtils.toLocation(post1.getLocation()));
        double metersTo2 = currentLocation.distanceTo(LocationUtils.toLocation(post2.getLocation()));

        Date currentTime = new Date();
        double minutesSince1 = (currentTime.getTime() - post1.getCreatedAt().getTime()) * milliSecondsToMinutes;
        double minutesSince2 = (currentTime.getTime() - post2.getCreatedAt().getTime()) * milliSecondsToMinutes;

        return Double.compare(metersTo1 + minutesSince1, metersTo2 + minutesSince2);
    }

}
