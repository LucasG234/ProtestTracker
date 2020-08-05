package com.lucasg234.protesttracker.mainactivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.ItemFeedPostBinding;
import com.lucasg234.protesttracker.detailactivity.PostDetailActivity;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.lucasg234.protesttracker.permissions.LocationPermissions;
import com.lucasg234.protesttracker.util.DateUtils;
import com.lucasg234.protesttracker.util.ImageUtils;
import com.lucasg234.protesttracker.util.LocationUtils;
import com.lucasg234.protesttracker.util.ParseUtils;
import com.parse.FunctionCallback;
import com.parse.ParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter used by the FeedFragment
 * Holds all posts queried from Parse
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private static final String TAG = "FeedAdapter";

    private MainActivity mParent;
    private RecyclerView mParentRecyclerView;
    // This list holds posts as they are sorted in the view
    private List<Post> mPosts;

    public FeedAdapter(MainActivity parentActivity, RecyclerView recyclerView) {
        this.mParent = parentActivity;
        this.mParentRecyclerView = recyclerView;
        this.mPosts = new ArrayList<>();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mParent);
        ItemFeedPostBinding postBinding = ItemFeedPostBinding.inflate(inflater, parent, false);
        return new FeedViewHolder(postBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        holder.bind(mPosts.get(position));
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    // Helper method to add new posts to RecyclerView
    public void addAll(List<Post> newPosts) {
        // Add all new posts in the sorting order they were given
        mPosts.addAll(newPosts);
        notifyDataSetChanged();
    }

    // Helper method to clear the RecyclerView
    public void clear() {
        mPosts.clear();
        notifyDataSetChanged();
    }

    public Post getPost(int position) {
        return mPosts.get(position);
    }

    // Returns the last post in the adapter, according to its current sorting
    public Post getLastPost() {
        return mPosts.get(mPosts.size() - 1);
    }

    public void ignorePost(Post post) {
        int position = mPosts.indexOf(post);
        // indexOf returns -1 if the object was not found in the list
        if (position != -1) {
            mPosts.remove(post);
            notifyItemRemoved(position);
        }
    }

    public void switchPostLiked(Post post) {
        int position = mPosts.indexOf(post);
        if (position != -1) {
            FeedViewHolder viewHolder = (FeedViewHolder) mParentRecyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                viewHolder.switchLiked();
            }
        }
    }

    private Bundle getTransitionToDetailView(FeedAdapter.FeedViewHolder postViewHolder) {
        ItemFeedPostBinding postBinding = postViewHolder.getBinding();

        Pair<View, String> pairUsername = Pair.create((View) postBinding.postUsername, mParent.getString(R.string.transition_username));
        Pair<View, String> pairCreatedAt = Pair.create((View) postBinding.postCreatedAt, mParent.getString(R.string.transition_created_at));
        Pair<View, String> pairText = Pair.create((View) postBinding.postText, mParent.getString(R.string.transition_text));
        Pair<View, String> pairProfilePicture = Pair.create((View) postBinding.postProfilePicture,
                mParent.getString(R.string.transition_profile_picture));

        ActivityOptionsCompat transitionOptions;

        // Only include the image in the transition if it is visible
        if (postBinding.postImage.getVisibility() == View.VISIBLE) {
            Pair<View, String> pairImage = Pair.create((View) postBinding.postImage, mParent.getString(R.string.transition_image));
            transitionOptions = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) mParent,
                    pairUsername, pairCreatedAt, pairText, pairProfilePicture, pairImage);
        } else {
            transitionOptions = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) mParent,
                    pairUsername, pairCreatedAt, pairText, pairProfilePicture);
        }

        return transitionOptions.toBundle();
    }

    class FeedViewHolder extends RecyclerView.ViewHolder {

        private ItemFeedPostBinding mBinding;
        private boolean mLiked;

        public FeedViewHolder(@NonNull ItemFeedPostBinding binding) {
            super(binding.getRoot());

            this.mBinding = binding;
        }

        public void bind(final Post post) {
            mBinding.postText.setText(post.getText());
            mBinding.postUsername.setText(post.getAuthor().getUsername());

            String relativeCreationTime = DateUtils.dateToRelative(post.getCreatedAt());
            mBinding.postCreatedAt.setText(relativeCreationTime);

            if (LocationPermissions.checkLocationPermission(mParent)) {
                String relativeLocation = LocationUtils.toRelativeLocation(mParent, post.getLocation());
                if (relativeLocation == null) {
                    mBinding.postLocation.setText(R.string.error_location);
                } else {
                    mBinding.postLocation.setText(relativeLocation);
                }
            } else {
                mBinding.postLocation.setText(null);
            }

            // Load post image
            if (post.getImage() != null) {
                mBinding.postImage.setVisibility(View.VISIBLE);
                Log.i(TAG, post.getImage().getUrl());
                mParent.addProcess();
                Glide.with(mParent)
                        .load(post.getImage().getUrl())
                        .addListener(new ImageUtils.ImageRequestListener(mParent))
                        .into(mBinding.postImage);
            } else {
                mBinding.postImage.setVisibility(View.GONE);
            }

            // Load profile image with center crop
            ParseUtils.loadProfilePicture(post.getAuthor(), mBinding.postProfilePicture, true);


            // Set a listener for single and double clicks
            mBinding.postTouchHolder.setOnTouchListener(createItemTouchListener(post, this));

            // Set alternate listeners on the like and ignore buttons
            mBinding.postLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mParent.saveLikeChange(post);
                }
            });

            mBinding.postIgnore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mParent.saveIgnore(post);
                }
            });

            // Liked state defaults to false but may switch after it is checked
            setLikeVisuals();
            initialLikeCheck(post);
        }

        // This method is public so that other classes can affect the like status of the view holder to reflect the Parse server
        public void switchLiked() {
            mLiked = !mLiked;
            setLikeVisuals();
        }

        public ItemFeedPostBinding getBinding() {
            return mBinding;
        }

        private View.OnTouchListener createItemTouchListener(Post post, FeedViewHolder viewHolder) {
            final GestureDetector gestureDetector = new GestureDetector(mParent, new FeedGestureListener(post, viewHolder));

            View.OnTouchListener touchListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    Log.i(TAG, "Touched");
                    mBinding.postTouchHolder.performClick();
                    return gestureDetector.onTouchEvent(motionEvent);
                }
            };

            return touchListener;
        }

        private void initialLikeCheck(final Post post) {
            FunctionCallback<Boolean> likedCallback = new FunctionCallback<Boolean>() {
                @Override
                public void done(Boolean liked, ParseException e) {
                    if (e != null) {
                        Log.e(TAG, "Error in determining if post liked on ViewHolder bind", e);
                        return;
                    }

                    // Assumed state is false before the initial check
                    if (liked != mLiked) {
                        switchLiked();
                    }
                }
            };
            ParseUtils.getUserLikes((User) User.getCurrentUser(), post, likedCallback);
        }

        // Private helper method used to set the visuals depending on current liked status
        private void setLikeVisuals() {
            Drawable toReplace;
            if (mLiked) {
                toReplace = mParent.getDrawable(R.drawable.baseline_star_accent_24);
            } else {
                toReplace = mParent.getDrawable(R.drawable.outline_star_24);
            }
            mBinding.postLike.setImageDrawable(toReplace);
        }
    }

    /**
     * Class which listens to all gestures made on the RecyclerView
     * Responds only to double taps and confirmed single taps
     */
    private class FeedGestureListener extends GestureDetector.SimpleOnGestureListener {

        private Post mPost;
        private FeedViewHolder mViewHolder;

        public FeedGestureListener(Post post, FeedViewHolder viewHolder) {
            this.mPost = post;
            this.mViewHolder = viewHolder;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            MainActivity parent = (MainActivity) mParent;
            parent.saveLikeChange(mPost);
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Intent detailIntent = new Intent(mParent, PostDetailActivity.class);
            detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_POST, mPost);
            detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_LIKED, mViewHolder.mLiked);

            Bundle transitionBundle = getTransitionToDetailView(mViewHolder);

            Activity parent = (Activity) mParent;
            parent.startActivityForResult(detailIntent, PostDetailActivity.REQUEST_CODE_POST_DETAIL, transitionBundle);
            return true;
        }
    }
}

