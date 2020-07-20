package com.lucasg234.protesttracker.detailactivity;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.lucasg234.protesttracker.databinding.ActivityPostDetailBinding;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.util.LocationUtils;
import com.lucasg234.protesttracker.util.DateUtils;

/**
 * Fragment which displays additional details about a given post
 * Created in FeedFragment when posts are clicked by users
 */
public class PostDetailActivity extends AppCompatActivity {

    public static final String KEY_INTENT_EXTRA_POST = "parcelable_post";
    private static final String TAG = "PostDetailActivity";
    private ActivityPostDetailBinding mBinding;

    // This interface handles interaction with the FeedFragment and MapFragment
    public interface PostDetailInteractionListener extends Parcelable {
        void onIgnoreClicked(Post post);

        void onRecommendClicked(Post post);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        Post post = getIntent().getExtras().getParcelable(KEY_INTENT_EXTRA_POST);

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
}