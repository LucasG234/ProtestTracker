package com.lucasg234.protesttracker.detailactivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.lucasg234.protesttracker.databinding.ActivityPostDetailBinding;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.util.DateUtils;
import com.lucasg234.protesttracker.util.LocationUtils;

/**
 * Fragment which displays additional details about a given post
 * Created in FeedFragment when posts are clicked by users
 */
public class PostDetailActivity extends AppCompatActivity {

    public static final String KEY_INTENT_EXTRA_POST = "parcelable_post";
    public static final int REQUEST_CODE_POST_DETAIL = 406;

    private static final String TAG = "PostDetailActivity";

    private ActivityPostDetailBinding mBinding;
    private Intent mReturnIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        Bundle extras = getIntent().getExtras();
        Post post = extras.getParcelable(KEY_INTENT_EXTRA_POST);

        configureVisualElements(post);
        configureButtons(post);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setResult(RESULT_OK, mReturnIntent);
    }

    private void configureVisualElements(Post post) {
        mBinding.detailText.setText(post.getText());
        mBinding.detailUsername.setText(post.getAuthor().getUsername());

        String relativeCreationTime = DateUtils.dateToRelative(post.getCreatedAt());
        mBinding.detailCreatedAt.setText(relativeCreationTime);

        String address = LocationUtils.toAddress(this, post.getLocation());
        mBinding.detailLocation.setText(address);

        if (post.getImage() != null) {
            mBinding.detailImage.setVisibility(View.VISIBLE);
            Log.i(TAG, post.getImage().getUrl());
            Glide.with(this)
                    .load(post.getImage().getUrl())
                    .into(mBinding.detailImage);
        } else {
            mBinding.detailImage.setVisibility(View.GONE);
        }
    }

    private void configureButtons(final Post post) {
        mBinding.detailRecommendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRecommendClicked();
            }
        });

        mBinding.detailIgnoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onIgnoreClicked();
            }
        });
    }

    private void onIgnoreClicked() {
        Log.i(TAG, "DetailActivity found ignore click");
    }

    private void onRecommendClicked() {
        Log.i(TAG, "DetailActivity found recommend click");
    }
}