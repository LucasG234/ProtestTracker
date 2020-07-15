package com.lucasg234.protesttracker.mainactivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.FragmentComposeBinding;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.SaveCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment where user can create posts
 * Saves posts to Parse
 */
public class ComposeFragment extends Fragment {

    public static final int ACTIVITY_REQUEST_CODE_CAMERA = 635;
    public static final int ACTIVITY_REQUEST_CODE_GALLERY = 321;
    public static final String TEMP_PHOTO_NAME = "ProtestTrackerTemp.jpg";
    private static final String TAG = "ComposeFragment";
    private FragmentComposeBinding mBinding;
    private File mTempInternalImageStorage;

    public ComposeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of ComposeFragment
     */
    public static ComposeFragment newInstance() {
        return new ComposeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return FragmentComposeBinding.inflate(inflater, container, false).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding = FragmentComposeBinding.bind(view);

        mBinding.composeSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePost();
            }
        });

        mBinding.composeCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
            }
        });

        mBinding.composeGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        configureTempImageStorage();
    }

    // Used to receive photos after camera or gallery usage
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Toast.makeText(getContext(), getString(R.string.error_receive_image), Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap takenImage;

        switch (requestCode) {
            case ACTIVITY_REQUEST_CODE_CAMERA:
                Log.i(TAG, "received photo from camera");
                // receiveImage handles necessary resizing and rotations
                takenImage = decodeInternalImage(Uri.fromFile(mTempInternalImageStorage));
                break;
            case ACTIVITY_REQUEST_CODE_GALLERY:
                Log.i(TAG, "received photo from gallery");
                Uri photoUri = data.getData();
                takenImage = receiveExternalImage(photoUri);
                saveToInternalStorage(takenImage);
                break;
            default:
                Log.e(TAG, "Received onActivityResult with unknown request code:" + requestCode);
                return;
        }
        // Load the taken image into the preview space
        mBinding.composeImagePreview.setImageBitmap(takenImage);
    }

    private void configureTempImageStorage() {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "failed to create directory");
        }

        // Create the file target for camera taken images based on the constant file name
        mTempInternalImageStorage = new File(mediaStorageDir.getPath() + File.separator + TEMP_PHOTO_NAME);
    }


    // Constructs Post object and saves it to the Parse server
    private void savePost() {
        Post post = new Post();
        post.setText(mBinding.composeEditText.getText().toString());
        post.setAuthor((User) User.getCurrentUser());
        // The current image will be stored within the the temp image storage
        post.setImage(new ParseFile(mTempInternalImageStorage));
        post.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error saving post", e);
                    Toast.makeText(getContext(), getString(R.string.error_save), Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.i(TAG, "Saved post successfully");
                mBinding.composeEditText.setText("");
                mBinding.composeImagePreview.setImageBitmap(null);
            }
        });
    }

    private void openCamera() {
        // create Intent to take a picture and return control to the calling application
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // wrap this our target File object into a content provider
        // required for API >= 24
        Uri fileProvider = FileProvider.getUriForFile(getContext(), "com.lucas.fileprovider", mTempInternalImageStorage);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);

        // Ensure there is an app which can handle the intent before calling it
        if (cameraIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(cameraIntent, ACTIVITY_REQUEST_CODE_CAMERA);
        } else {
            Toast.makeText(getContext(), getString(R.string.error_camera_missing), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        // Create intent for picking a photo from the gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Ensure there is an app which can handle the intent before calling it
        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(intent, ACTIVITY_REQUEST_CODE_GALLERY);
        } else {
            Toast.makeText(getContext(), getString(R.string.error_gallery_missing), Toast.LENGTH_SHORT).show();
        }
    }

    // Correctly decodes images which are internally stored
    private Bitmap decodeInternalImage(Uri internalUri) {
        // Decode the image in temp storage to a bitmap
        Bitmap imageBitmap = BitmapFactory.decodeFile(internalUri.getPath());

        // Attempt to read EXIF Data about the image
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(internalUri.getPath());
        } catch (IOException e) {
            Log.e(TAG, "Error getting EXIF data", e);
            // If there is no EXIF data available, return the original Bitmap
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

    // Receives bitmap from the gallery
    private Bitmap receiveExternalImage(Uri externalUri) {
        Bitmap imageBitmap = null;
        try {
            // check version of Android on device
            if (Build.VERSION.SDK_INT > 27) {
                // on newer versions of Android, use the new decodeBitmap method
                ImageDecoder.Source source = ImageDecoder.createSource(getContext().getContentResolver(), externalUri);
                imageBitmap = ImageDecoder.decodeBitmap(source);
            } else {
                // support older versions of Android by using getBitmap
                imageBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), externalUri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageBitmap;
    }

    private void saveToInternalStorage(Bitmap bitmapImage) {

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(mTempInternalImageStorage);
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
}