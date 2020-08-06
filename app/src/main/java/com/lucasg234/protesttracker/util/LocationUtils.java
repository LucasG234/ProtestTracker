package com.lucasg234.protesttracker.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.permissions.PermissionsHandler;
import com.parse.ParseGeoPoint;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Utility class which deals with Location, ParseGeoPoint, and LatLng objects
 */
public class LocationUtils {

    private static final String TAG = "LocationUtils";
    public static final double MILES_TO_FEET = 5280;

    // Converts a Location object to the equivalent LatLng object
    public static LatLng toLatLng(Location location) {
        return location == null ? null : new LatLng(location.getLatitude(), location.getLongitude());
    }

    // Converts a ParseGeoPoint object to the equivalent LatLng object
    public static LatLng toLatLng(ParseGeoPoint location) {
        return location == null ? null : new LatLng(location.getLatitude(), location.getLongitude());
    }

    // Converts a ParseGeoPoint object to the equivalent Location object
    public static Location toLocation(ParseGeoPoint geoPoint) {
        if (geoPoint == null) {
            return null;
        }
        // Provide empty string because there is no provider
        Location location = new Location("");
        location.setLatitude(geoPoint.getLatitude());
        location.setLongitude(geoPoint.getLongitude());
        return location;
    }

    public static ParseGeoPoint toParseGeoPoint(Location location) {
        return location == null ? null : new ParseGeoPoint(location.getLatitude(), location.getLongitude());
    }

    // Returns the last known location of the user
    public static Location getCurrentLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!PermissionsHandler.checkLocationPermission(context)) {
            Log.e(TAG, "No location permissions");
            return null;
        }
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    // Returns a string displays the relative location of a post from the user's current location
    public static String toRelativeLocation(Context context, ParseGeoPoint targetGeoPoint) {
        if (targetGeoPoint == null) {
            return null;
        }
        Location currentLocation = getCurrentLocation(context);
        // Provide empty string because there is no provider
        Location targetLocation = new Location("");
        targetLocation.setLatitude(targetGeoPoint.getLatitude());
        targetLocation.setLongitude(targetGeoPoint.getLongitude());

        float metersBetween = currentLocation.distanceTo(targetLocation);

        return String.format(context.getString(R.string.distance_format), metersToImperialString(metersBetween));
    }

    // Converts the ParseGeoPoint given into a String representing its address
    public static String toAddress(Context context, ParseGeoPoint targetGeoPoint) {
        if (targetGeoPoint == null) {
            return null;
        }
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addressList;
        try {
            // maxResults represents how many nearby addresses to return
            // Set to 1 to get only the closest address
            addressList = geocoder.getFromLocation(targetGeoPoint.getLatitude(), targetGeoPoint.getLongitude(), 1);
        } catch (IOException e) {
            Log.e(TAG, "Error generating address list from location", e);
            return context.getString(R.string.error_address_generation);
        }

        if (addressList.size() >= 1) {
            return addressList.get(0).getAddressLine(0);
        } else {
            Log.e(TAG, "No address for location");
            return context.getString(R.string.error_address_generation);
        }
    }

    // Helper method to convert meters to human readable imperial units
    private static String metersToImperialString(float meters) {
        float miles = meters * 0.000621371f;
        String out;
        if (miles < 0.1) {
            // If less than .1 miles use feet instead
            int feet = (int) Math.round(miles * MILES_TO_FEET);
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
