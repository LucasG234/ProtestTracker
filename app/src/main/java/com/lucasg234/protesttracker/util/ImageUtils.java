package com.lucasg234.protesttracker.util;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.mainactivity.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class which holds methods useful for storing and receiving images
 * From internal and external memory
 */
public class ImageUtils {

    public static final int ACTIVITY_REQUEST_CODE_CAMERA = 635;
    public static final int ACTIVITY_REQUEST_CODE_GALLERY = 321;

    private static final String TAG = "ImageUtils";

    // Method which generates a File object for images to be stored into
    public static File configureTempImageStorage(Fragment parent) throws IOException {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(parent.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "failed to create directory");
        }

        // Generate photo name based upon current time
        String timeStamp = new SimpleDateFormat(parent.getString(R.string.image_storage_date_format)).format(new Date());
        File tempStorage = File.createTempFile(
                timeStamp,  /* prefix */
                ".jpg",         /* suffix */
                mediaStorageDir      /* directory */
        );

        return tempStorage;
    }

    // Starts an activity which opens the camera to take a picture
    // Result can be captured using onActivityResult
    public static void openCameraForResult(Fragment parent, File internalImageStorage) {
        // create Intent to take a picture and return control to the calling application
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // wrap this our target File object into a content provider
        // required for API >= 24
        Uri fileProvider = FileProvider.getUriForFile(parent.getContext(), "com.lucas.fileprovider", internalImageStorage);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);

        // Ensure there is an app which can handle the intent before calling it
        if (cameraIntent.resolveActivity(parent.getContext().getPackageManager()) != null) {
            parent.startActivityForResult(cameraIntent, ACTIVITY_REQUEST_CODE_CAMERA);
        } else {
            Toast.makeText(parent.getContext(), R.string.error_camera_missing, Toast.LENGTH_SHORT).show();
        }
    }

    // Starts an activity which opens the gallery to take a select
    // Result can be captured using onActivityResult
    public static void openGalleryForResult(Fragment parent) {
        // Create intent for picking a photo from the gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Ensure there is an app which can handle the intent before calling it
        if (intent.resolveActivity(parent.getContext().getPackageManager()) != null) {
            parent.startActivityForResult(intent, ACTIVITY_REQUEST_CODE_GALLERY);
        } else {
            Toast.makeText(parent.getContext(), R.string.error_gallery_missing, Toast.LENGTH_SHORT).show();
        }
    }

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

    /**
     * Glide listener which will notify the MainActivity that the process has ended
     */
    public static class ImageRequestListener implements RequestListener {

        private MainActivity mParent;

        public ImageRequestListener(MainActivity parent) {
            this.mParent = parent;
        }

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
            mParent.subtractProcess();
            return false;
        }

        @Override
        public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
            mParent.subtractProcess();
            return false;
        }
    }
}
