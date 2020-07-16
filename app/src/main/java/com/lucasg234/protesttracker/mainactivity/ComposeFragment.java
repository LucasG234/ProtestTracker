package com.lucasg234.protesttracker.mainactivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
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
import com.lucasg234.protesttracker.permissions.LocationPermissions;
import com.lucasg234.protesttracker.util.ImageUtils;
import com.lucasg234.protesttracker.util.LocationUtils;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.SaveCallback;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment where user can create posts
 * Saves posts to Parse
 */
public class ComposeFragment extends Fragment {

    public static final int ACTIVITY_REQUEST_CODE_CAMERA = 635;
    public static final int ACTIVITY_REQUEST_CODE_GALLERY = 321;

    private static final String TAG = "ComposeFragment";

    private FragmentComposeBinding mBinding;
    private File mInternalImageStorage;

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
                // Configure internal storage for the image if not already done, then open the camera to take it
                if(mInternalImageStorage != null) {
                    configureTempImageStorage();
                }
                openCamera();
            }
        });

        mBinding.composeGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });
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
                takenImage = ImageUtils.decodeInternalImage(Uri.fromFile(mInternalImageStorage));
                break;
            case ACTIVITY_REQUEST_CODE_GALLERY:
                Log.i(TAG, "received photo from gallery");
                Uri photoUri = data.getData();
                takenImage = ImageUtils.decodeExternalImage(getContext().getContentResolver(), photoUri);
                ImageUtils.saveImageToInternalStorage(takenImage, mInternalImageStorage);
                break;
            default:
                Log.e(TAG, "Received onActivityResult with unknown request code:" + requestCode);
                return;
        }
        // Load the taken image into the preview space
        mBinding.composeImagePreview.setImageBitmap(takenImage);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // If permission was just granted to allow location services, then restart saving the image again
        if (requestCode == LocationPermissions.REQUEST_CODE_LOCATION_PERMISSIONS && permissions.length >= 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            savePost();
        }
    }

    // Method which generates a File object for the camera to store images into
    // Called only whenever the user opens
    private void configureTempImageStorage() {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "failed to create directory");
        }

        // Generate photo name based upon current time
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        try {
            mInternalImageStorage = File.createTempFile(
                    timeStamp,  /* prefix */
                    ".jpg",         /* suffix */
                    mediaStorageDir      /* directory */
            );
        } catch (IOException e) {
            Log.e(TAG, "Could not generate internal image storage", e);
            Toast.makeText(getContext(), getString(R.string.error_file_generation), Toast.LENGTH_SHORT).show();
        }

    }

    // Constructs Post object and saves it to the Parse server
    private void savePost() {
        Post post = new Post();

        // Ensure location permissions before attempting to make post
        if (!LocationPermissions.checkLocationPermission(getContext())) {
            Log.i(TAG, "Cancelling post save to ask for permissions");
            LocationPermissions.requestLocationPermission(this);
            return;
        }

        // Next ensure current location can be found
        Location currentLocation = LocationUtils.getCurrentLocation(getContext());
        if (currentLocation != null) {
            post.setLocation(new ParseGeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
        } else {
            Log.e(TAG, "Couldn't find a location");
            Toast.makeText(getContext(), getString(R.string.error_location), Toast.LENGTH_SHORT).show();
            return;
        }

        post.setText(mBinding.composeEditText.getText().toString());
        post.setAuthor((User) User.getCurrentUser());

        // Check if there is currently a previewed image
        if (mBinding.composeImagePreview.getDrawable() != null) {
            // If there is, the current image will be stored within the the temp image storage
            post.setImage(new ParseFile(mInternalImageStorage));
        }

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
                // Delete the take image from internal storage
                mInternalImageStorage.delete();
                mInternalImageStorage = null;
            }
        });
    }

    private void openCamera() {
        // create Intent to take a picture and return control to the calling application
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // wrap this our target File object into a content provider
        // required for API >= 24
        Uri fileProvider = FileProvider.getUriForFile(getContext(), "com.lucas.fileprovider", mInternalImageStorage);
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
}