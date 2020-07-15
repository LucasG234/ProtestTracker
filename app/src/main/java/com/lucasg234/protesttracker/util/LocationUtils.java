package com.lucasg234.protesttracker.util;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.lucasg234.protesttracker.permissions.LocationPermissions;
import com.parse.ParseGeoPoint;

import java.util.Locale;

public class LocationUtils {
    private static final String TAG = "LocationUtils";

    // Returns the last known location of the user
    public static Location getCurrentLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!LocationPermissions.checkLocationPermission(context)) {
            Log.e(TAG, "No location permissions");
            return null;
        }
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    public static String relativeLocation(Context context, ParseGeoPoint targetGeoPoint) {
        Location currentLocation = getCurrentLocation(context);
        // Provide empty string because there is no provider
        Location targetLocation = new Location("");
        targetLocation.setLatitude(targetGeoPoint.getLatitude());
        targetLocation.setLongitude(targetGeoPoint.getLongitude());

        float metersBetween = currentLocation.distanceTo(targetLocation);

        return metersToImperialString(metersBetween) + " away";
    }

    private static String metersToImperialString(float meters) {
        float miles = meters * 0.000621371f;
        String out;
        if(miles < 0.1) {
            // If less than .1 miles use feet instead
            int feet = Math.round(miles * 5280f);
            out = String.format(Locale.getDefault(), "%d feet", feet);
        } else if (miles < 100) {
            // If under 100 miles, add one decimal place
            out = String.format(Locale.getDefault(), "%.1f miles", miles);
        } else {
            // If over 100 miles, only display whole numbers
            out = String.format(Locale.getDefault(), "%.0f miles", miles);
        }
        return out;
    }
}
