package com.lucasg234.protesttracker.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility class which holds methods useful for storing and receiving images
 * From internal and external memory
 */
public class ImageUtils {

    private static final String TAG = "ImageUtils";

    // Saves an image to an internal storage destination
    public static void saveImageToInternalStorage(Bitmap bitmapImage, File saveDestination) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(saveDestination);
            // Quality of 100 tells Compressor to maintain original quality
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
        } catch (Exception e) {
            Log.e(TAG, "Error writing image to internal storage", e);
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing FileOutputStream", e);
            }
        }
    }

    // Receives a bitmap from the device's gallery
    public static Bitmap decodeExternalImage(ContentResolver contentResolver, Uri externalUri) {
        Bitmap imageBitmap = null;
        try {
            // check version of Android on device
            if (Build.VERSION.SDK_INT > 27) {
                // on newer versions of Android, use the new decodeBitmap method
                ImageDecoder.Source source = ImageDecoder.createSource(contentResolver, externalUri);
                imageBitmap = ImageDecoder.decodeBitmap(source);
            } else {
                // support older versions of Android by using getBitmap
                imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, externalUri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageBitmap;
    }

    // Correctly decodes images which are internally stored
    // More work must be done because we don't get to piggyback off of the gallery here
    public static Bitmap decodeInternalImage(Uri internalUri) {
        // Decode the image in temp storage to a bitmap
        Bitmap imageBitmap = BitmapFactory.decodeFile(internalUri.getPath());

        // Attempt to read EXIF Data about the image
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(internalUri.getPath());
        } catch (IOException e) {
            Log.e(TAG, "Error getting EXIF data", e);
            // If there is no EXIF data available, return the original decoded Bitmap
            return imageBitmap;
        }

        // Find the correct orientation for the image from the EXIF data
        String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
        int rotationAngle;
        switch (orientation) {
            default:
                rotationAngle = 0;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotationAngle = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotationAngle = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotationAngle = 270;
                break;
        }

        // Rotate the Bitmap
        Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle, (float) imageBitmap.getWidth() / 2, (float) imageBitmap.getHeight() / 2);
        Bitmap rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);

        // Return resulting image
        return rotatedBitmap;
    }

}
