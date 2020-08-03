package com.lucasg234.protesttracker.mainactivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import static android.app.Activity.RESULT_OK;

/**
 * Fragment where user can create posts
 * Saves posts to Parse
 */
public class ComposeFragment extends Fragment {
    private static final String TAG = "ComposeFragment";

    private FragmentComposeBinding mBinding;
    private File mInternalImageStorage;
    private boolean mSaving;

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
                if (validatePost() && !mSaving) {
                    savePost();
                }
            }
        });

        mBinding.composeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create the modal overlay to select an image
                ImageDialogFragment editNameDialogFragment = ImageDialogFragment.newInstance();
                editNameDialogFragment.show(getActivity().getSupportFragmentManager(), ImageDialogFragment.class.getSimpleName());
            }
        });
    }

    // Used to receive photos after camera or gallery usage
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Toast.makeText(getContext(), R.string.error_receive_image, Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap takenImage;

        switch (requestCode) {
            case ImageUtils.ACTIVITY_REQUEST_CODE_CAMERA:
                Log.i(TAG, "received photo from camera");
                takenImage = ImageUtils.decodeInternalImage(Uri.fromFile(mInternalImageStorage));
                break;
            case ImageUtils.ACTIVITY_REQUEST_CODE_GALLERY:
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

    // Configures file object to store taken image into whenever user open the camera or gallery
    private void configureTempImageStorage() {
        try {
            mInternalImageStorage = ImageUtils.configureTempImageStorage(ComposeFragment.this);
        } catch (IOException e) {
            Log.e(TAG, "Could not generate internal image storage", e);
            Toast.makeText(getContext(), R.string.error_file_generation, Toast.LENGTH_SHORT).show();
        }
    }

    public void onCameraClick() {
        // Configure internal storage for the image if not already done, then open the camera to take it
        if (mInternalImageStorage == null) {
            configureTempImageStorage();
        }
        ImageUtils.openCameraForResult(ComposeFragment.this, mInternalImageStorage);
    }

    public void onGalleryClick() {
        // Configure internal storage for the image if not already done, then open the gallery to find it
        if (mInternalImageStorage == null) {
            configureTempImageStorage();
        }
        ImageUtils.openGalleryForResult(ComposeFragment.this);
    }

    // Determines whether the current composeEditText and composeImagePreview represent a valid post
    // Outputs an error message in a Toast if false
    private boolean validatePost() {
        String text = mBinding.composeEditText.getText().toString();

        // Ensure text is not empty or only whitespace
        if (text.trim().isEmpty()) {
            Toast.makeText(getContext(), R.string.error_post_no_text, Toast.LENGTH_SHORT).show();
            return false;
        }
        // Impose maximum text length
        else if (text.length() > Post.MAXIMUM_LENGTH) {
            Toast.makeText(getContext(), R.string.error_post_over_maximum, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    // Constructs Post object and saves it to the Parse server
    private void savePost() {
        // Mark that the fragment is currently saving a post
        mSaving = true;

        Post.Builder postBuilder = new Post.Builder();

        // Ensure location permissions before attempting to make post
        if (!LocationPermissions.checkLocationPermission(getContext())) {
            Log.i(TAG, "Cancelling post save to ask for permissions");
            LocationPermissions.requestLocationPermission(this);
            return;
        }

        // Next ensure current location can be found
        Location currentLocation = LocationUtils.getCurrentLocation(getContext());
        if (currentLocation != null) {
            postBuilder.setLocation(new ParseGeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
        } else {
            Log.e(TAG, "Couldn't find a location");
            Toast.makeText(getContext(), R.string.error_location, Toast.LENGTH_SHORT).show();
            return;
        }

        postBuilder.setText(mBinding.composeEditText.getText().toString());
        postBuilder.setAuthor((User) User.getCurrentUser());

        // Check if there is currently a previewed image
        if (mBinding.composeImagePreview.getDrawable() != null) {
            // If there is, the current image will be stored within the the temp image storage
            postBuilder.setImage(new ParseFile(mInternalImageStorage));
        }

        postBuilder.createModel().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                // Mark that saving is complete, even if there was an error
                mSaving = false;

                if (e != null) {
                    Log.e(TAG, "Error saving post", e);
                    Toast.makeText(getContext(), R.string.error_save, Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.i(TAG, "Saved post successfully");
                mBinding.composeEditText.setText("");
                mBinding.composeImagePreview.setImageBitmap(null);

                // Delete the take image from internal storage if there is one
                if (mInternalImageStorage != null) {
                    mInternalImageStorage.delete();
                }
                mInternalImageStorage = null;
            }
        });
    }

    // Ensure no floating storage left on fragment deletion
    @Override
    public void onDestroy() {
        if (mInternalImageStorage != null) {
            mInternalImageStorage.delete();
            mInternalImageStorage = null;
        }
        super.onDestroy();
    }
}