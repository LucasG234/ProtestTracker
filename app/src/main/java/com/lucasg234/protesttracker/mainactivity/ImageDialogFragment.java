package com.lucasg234.protesttracker.mainactivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.lucasg234.protesttracker.databinding.FragmentImageDialogBinding;

/**
 * This class is used as a modal overlay to allow the user to select where to upload images from
 * It is called from the ComposeFragment
 */
public class ImageDialogFragment extends DialogFragment {
    private static final String TAG = "ImageDialogFragment";

    private FragmentImageDialogBinding mBinding;

    public ImageDialogFragment() {
        // Required empty public constructor
    }

    // This factory method is used to create instances of the ImageDialogFragment
    public static ImageDialogFragment newInstance() {
        return new ImageDialogFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return FragmentImageDialogBinding.inflate(inflater, container, false).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding = FragmentImageDialogBinding.bind(view);
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        final ComposeFragment composeParent = (ComposeFragment) fragmentManager.findFragmentByTag(ComposeFragment.class.getSimpleName());

        mBinding.imageCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Configure internal storage for the image if not already done, then open the camera to take it
                composeParent.onCameraClick();
            }
        });

        mBinding.imageGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Configure internal storage for the image if not already done, then open the gallery to find it
                composeParent.onGalleryClick();
            }
        });
    }
}