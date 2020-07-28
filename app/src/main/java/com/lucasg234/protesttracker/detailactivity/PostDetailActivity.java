package com.lucasg234.protesttracker.detailactivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.ActivityPostDetailBinding;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.util.DateUtils;
import com.lucasg234.protesttracker.util.LocationUtils;

/**
 * Fragment which displays additional details about a given post
 * Created in FeedFragment when posts are clicked by users
 */
public class PostDetailActivity extends AppCompatActivity {

    public static final String KEY_INTENT_EXTRA_POST = "input_post";
    public static final String KEY_INTENT_EXTRA_LIKED = "input_liked";
    public static final String KEY_RESULT_LIKED_CHANGED = "output_likeChanged";
    public static final String KEY_RESULT_IGNORED = "output_wasIgnored";
    public static final String KEY_RESULT_POST = "output_post";
    public static final int REQUEST_CODE_POST_DETAIL = 406;

    private static final String TAG = "PostDetailActivity";

    private ActivityPostDetailBinding mBinding;
    private Post mPost;
    private boolean mInitialLikedStatus;
    private boolean mCurrentLikedStatus;
    private boolean mWasIgnored;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        Bundle extras = getIntent().getExtras();
        mPost = extras.getParcelable(KEY_INTENT_EXTRA_POST);
        mInitialLikedStatus = extras.getBoolean(KEY_INTENT_EXTRA_LIKED);
        mCurrentLikedStatus = mInitialLikedStatus;

        configureVisualElements(mPost);
        configureButtons();
    }

    @Override
    public void finish() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(KEY_RESULT_LIKED_CHANGED, mInitialLikedStatus != mCurrentLikedStatus);
        returnIntent.putExtra(KEY_RESULT_IGNORED, mWasIgnored);
        returnIntent.putExtra(KEY_RESULT_POST, mPost);
        setResult(RESULT_OK, returnIntent);
        super.finish();
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

        changeLikeVisually();
    }

    private void changeLikeVisually() {
        Drawable toReplace;
        if (mCurrentLikedStatus) {
            toReplace = getDrawable(R.drawable.baseline_star_accent_24);
        } else {
            toReplace = getDrawable(R.drawable.outline_star_24);
        }
        mBinding.detailLike.setImageDrawable(toReplace);
    }

    private void configureButtons() {
        mBinding.detailLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentLikedStatus = !mCurrentLikedStatus;
                changeLikeVisually();
            }
        });

        mBinding.detailIgnore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWasIgnored = true;
                // If a post is ignored, immediately close the DetailView
                finish();
            }
        });
    }
}