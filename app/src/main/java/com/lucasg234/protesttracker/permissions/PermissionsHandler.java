package com.lucasg234.protesttracker.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

/**
 * Utility class with methods which handle permission for accessing fine and coarse location
 */
public class PermissionsHandler {
    public static final int REQUEST_CODE_LOCATION_PERMISSIONS = 12;
    public static final int REQUEST_CODE_STORAGE_PERMISSIONS = 13;

    private static final String TAG = "PermissionsHandler";

    // Checks whether location permissions are available
    public static boolean checkLocationPermission(Context context) {
        return checkPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    // Asks the user to give permission for location services if not already enabled
    public static void requestLocationPermission(Fragment parent) {
        requestPermissions(parent, REQUEST_CODE_LOCATION_PERMISSIONS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    // Checks whether external storage permissions are available
    public static boolean checkStoragePermissions(Context context) {
        return checkPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    // Asks the user to give permission for external storage services if not already enabled
    public static void requestStoragePermissions(Fragment parent) {
        requestPermissions(parent, REQUEST_CODE_STORAGE_PERMISSIONS, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private static boolean checkPermissions(Context context, String... permissions) {
        boolean allPermissionsGranted = true;
        for (String s : permissions) {
            allPermissionsGranted = allPermissionsGranted && ActivityCompat.checkSelfPermission(context, s) == PackageManager.PERMISSION_GRANTED;
        }
        return allPermissionsGranted;
    }

    private static void requestPermissions(Fragment parent, int requestCode, String... permissions) {
        if (checkPermissions(parent.getContext(), permissions)) {
            // Ignore request if needed permissions already given
            return;
        }
        parent.requestPermissions(permissions, requestCode);
    }
}
