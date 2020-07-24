package com.lucasg234.protesttracker.permissions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lucasg234.protesttracker.databinding.FragmentNoPermissionBinding;

/**
 * Fragment used whenever location permissions are not granted
 * Allows user to request permissions without causing errors from other fragments
 */
public class NoPermissionsFragment extends Fragment {

    private static final String TAG = "NoPermissionsFragment";

    private FragmentNoPermissionBinding mBinding;

    public NoPermissionsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of NoPermissionsFragment
     */
    public static NoPermissionsFragment newInstance() {
        return new NoPermissionsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return FragmentNoPermissionBinding.inflate(inflater, container, false).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding = FragmentNoPermissionBinding.bind(view);
    }
}