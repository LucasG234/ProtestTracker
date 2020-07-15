package com.lucasg234.protesttracker.detailactivity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.lucasg234.protesttracker.databinding.ActivityPostDetailBinding;
import com.lucasg234.protesttracker.models.Post;

/**
 * Fragment which displays additional details about a given post
 */
public class PostDetailActivity extends AppCompatActivity {

    private static final String TAG = "PostDetailActivity";
    
    private ActivityPostDetailBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

    }
}