package com.lucasg234.protesttracker.mainactivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lucasg234.protesttracker.databinding.FragmentSettingsBinding;
import com.lucasg234.protesttracker.login.LoginActivity;
import com.lucasg234.protesttracker.models.User;

/**
 * Fragment containing settings user can control
 * Currently only setting is the ability to log out
 */
public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    private FragmentSettingsBinding mBinding;

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

        mBinding.settingProfilePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Changing profile picture");
            }
        });
    }
}