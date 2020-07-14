package com.lucasg234.protesttracker.mainactivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
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
import java.io.IOException;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment where user can create posts
 * Saves posts to Parse
 */
public class ComposeFragment extends Fragment {

    private static final String TAG = "ComposeFragment";

    public static final int ACTIVITY_REQUEST_CODE_CAMERA = 635;
    public static final int ACTIVITY_REQUEST_CODE_GALLERY = 321;
    public static final String TEMP_PHOTO_NAME = "ProtestTrackerTemp.jpg";

    private FragmentComposeBinding mBinding;
    private File mTempImageStorage;

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

        mBinding.composeCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
            }
        });

        mBinding.composeSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePost();
            }
        });

        configureTempImageStorage();
    }

    // Used to receive photos after camera or gallery usage
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != RESULT_OK) {
            Toast.makeText(getContext(), getString(R.string.error_receive_image), Toast.LENGTH_SHORT).show();
            return;
        }

        switch(requestCode) {
            case ACTIVITY_REQUEST_CODE_CAMERA:
                Log.i(TAG, "received photo");
                // Does necessary resizing and rotations
                Bitmap takenImage = receiveImage();
                // Load the taken image into the preview space
                mBinding.composeImagePreview.setImageBitmap(takenImage);
                break;
        }
    }

    private void configureTempImageStorage() {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
            Log.d(TAG, "failed to create directory");
        }

        // Create the file target for camera taken images based on the constant file name
        mTempImageStorage = new File(mediaStorageDir.getPath() + File.separator + TEMP_PHOTO_NAME);
    }


    // Constructs Post object and saves it to the Parse server
    private void savePost() {
        Post post = new Post();
        post.setText(mBinding.composeEditText.getText().toString());
        post.setAuthor((User) User.getCurrentUser());
        // The current image will be stored within the the temp image storage
        post.setImage(new ParseFile(mTempImageStorage));
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
        Uri fileProvider = FileProvider.getUriForFile(getContext(), "com.lucas.fileprovider", mTempImageStorage);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);

        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
        // So as long as the result is not null, it's safe to use the intent.
        if (cameraIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(cameraIntent, ACTIVITY_REQUEST_CODE_CAMERA);
        }
    }

    private Bitmap receiveImage() {
        // Create and configure BitmapFactory
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mTempImageStorage.getAbsolutePath(), bounds);
        BitmapFactory.Options opts = new BitmapFactory.Options();

        // Decode the image in temp storage to a bitmap
        Bitmap imageBitmap = BitmapFactory.decodeFile(mTempImageStorage.getAbsolutePath(), opts);

        // Attempt to read EXIF Data about the image
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(mTempImageStorage.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error getting EXIF data", e);
            // If there is no EXIF data available, return the original Bitmap
            return imageBitmap;
        }

        // Find the correct orientation for the image from the EXIF data
        String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
        int rotationAngle;
        switch(orientation) {
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
        Bitmap rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);

        // Return resulting image
        return rotatedBitmap;
    }
}