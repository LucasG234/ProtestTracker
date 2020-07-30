package com.lucasg234.protesttracker.mainactivity;

import android.app.Activity;
import android.content.Intent;
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
import com.lucasg234.protesttracker.databinding.FragmentSettingsBinding;
import com.lucasg234.protesttracker.login.LoginActivity;
import com.lucasg234.protesttracker.models.User;
import com.lucasg234.protesttracker.util.ImageUtils;
import com.parse.ParseFile;

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
                User.logOut();
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
                if(mInternalImageStorage == null) {
                    try {
                        mInternalImageStorage = ImageUtils.configureTempImageStorage(SettingsFragment.this);
                    } catch (IOException e) {
                        Log.e(TAG, "Could not generate internal image storage", e);
                        Toast.makeText(getContext(), R.string.error_file_generation, Toast.LENGTH_SHORT).show();
                    }
                }
                ImageUtils.openCameraForResult(SettingsFragment.this, mInternalImageStorage);
            }
        });

        mBinding.settingsProfilePictureGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Changing profile picture through gallery");
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

        switch (requestCode) {
            case ImageUtils.ACTIVITY_REQUEST_CODE_CAMERA:
                Log.i(TAG, "received photo from camera");
                User currentUser = (User)User.getCurrentUser();
                currentUser.setProfilePicture(new ParseFile(mInternalImageStorage));
                currentUser.saveInBackground();
                break;
            default:
                Log.e(TAG, "Received onActivityResult with unknown request code:" + requestCode);
                return;
        }
    }
}