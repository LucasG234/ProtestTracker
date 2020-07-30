package com.lucasg234.protesttracker.mainactivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
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

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.FragmentSettingsBinding;
import com.lucasg234.protesttracker.login.LoginActivity;
import com.lucasg234.protesttracker.models.User;
import com.lucasg234.protesttracker.util.ImageUtils;
import com.lucasg234.protesttracker.util.ParseUtils;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.SaveCallback;

import java.io.File;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment containing settings user can control
 * Currently only setting is the ability to log out
 */
public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    private FragmentSettingsBinding mBinding;
    private File mInternalImageStorage;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of SettingsFragment
     */
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return FragmentSettingsBinding.inflate(inflater, container, false).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding = FragmentSettingsBinding.bind(view);

        mBinding.settingsLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Parse logout
                User.logOut();
                // Facebook logout
                if (AccessToken.getCurrentAccessToken() != null) {
                    LoginManager.getInstance().logOut();
                }
                Activity currentParent = getActivity();
                Intent loginIntent = new Intent(currentParent, LoginActivity.class);
                currentParent.startActivity(loginIntent);
                currentParent.finish();
            }
        });

        mBinding.settingsProfilePictureCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Changing profile picture through camera");
                configureInternalStorage();
                ImageUtils.openCameraForResult(SettingsFragment.this, mInternalImageStorage);
            }
        });

        mBinding.settingsProfilePictureGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Changing profile picture through gallery");
                configureInternalStorage();
                ImageUtils.openGalleryForResult(SettingsFragment.this);
            }
        });

        // Load profile image with no cropping
        ParseUtils.loadProfilePicture((User) User.getCurrentUser(), mBinding.settingsProfileImage, false);
    }

    private void configureInternalStorage() {
        if (mInternalImageStorage == null) {
            try {
                mInternalImageStorage = ImageUtils.configureTempImageStorage(SettingsFragment.this);
            } catch (IOException e) {
                Log.e(TAG, "Could not generate internal image storage", e);
                Toast.makeText(getContext(), R.string.error_file_generation, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Used to receive photos after camera or gallery usage
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Toast.makeText(getContext(), R.string.error_receive_image, Toast.LENGTH_SHORT).show();
            return;
        }

        User currentUser = (User) User.getCurrentUser();
        Bitmap takenImage;

        switch (requestCode) {
            case ImageUtils.ACTIVITY_REQUEST_CODE_CAMERA:
                Log.i(TAG, "received photo from camera");
                // Take the bitmap out of internal storage to display immediately
                takenImage = ImageUtils.decodeInternalImage(Uri.fromFile(mInternalImageStorage));
                break;
            case ImageUtils.ACTIVITY_REQUEST_CODE_GALLERY:
                Log.i(TAG, "received photo from gallery");
                // Save the bitmap we already have into internal storage
                Uri photoUri = data.getData();
                takenImage = ImageUtils.decodeExternalImage(getContext().getContentResolver(), photoUri);
                ImageUtils.saveImageToInternalStorage(takenImage, mInternalImageStorage);
                break;
            default:
                Log.e(TAG, "Received onActivityResult with unknown request code:" + requestCode);
                return;
        }

        currentUser.setProfilePicture(new ParseFile(mInternalImageStorage));
        currentUser.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e != null) {
                    Log.e(TAG, "ParseException for currentUser save", e);
                    Toast.makeText(getContext(), R.string.error_save, Toast.LENGTH_SHORT).show();
                    return;
                }

                // After the save, reset the storage
                mInternalImageStorage.delete();
                mInternalImageStorage = null;
            }
        });

        mBinding.settingsProfileImage.setImageBitmap(takenImage);
    }

    // Ensure no floating storage left on fragment deletion
    @Override
    public void onDestroy() {
        if(mInternalImageStorage != null) {
            mInternalImageStorage.delete();
            mInternalImageStorage = null;
        }
        super.onDestroy();
    }
}