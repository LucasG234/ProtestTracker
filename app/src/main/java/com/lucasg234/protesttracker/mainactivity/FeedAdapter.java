package com.lucasg234.protesttracker.mainactivity;

import android.app.Activity;
import android.content.Context;
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
import com.lucasg234.protesttracker.util.LocationUtils;
import com.lucasg234.protesttracker.util.PostUtils;
import com.parse.FunctionCallback;
import com.parse.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RecyclerView Adapter used by the FeedFragment
 * Holds all posts queried from Parse
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private static final String TAG = "FeedAdapter";

    private Context mContext;
    private RecyclerView mParentRecyclerView;
    // This list holds posts in their chronological order
    private List<Post> mChronologicalPosts;
    // This list holds posts in their custom sorted order
    private List<Post> mOrderedPosts;

    public FeedAdapter(Context context, RecyclerView parent) {
        this.mContext = context;
        this.mParentRecyclerView = parent;
        this.mChronologicalPosts = new ArrayList<>();
        this.mOrderedPosts = new ArrayList<>();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ItemFeedPostBinding postBinding = ItemFeedPostBinding.inflate(inflater, parent, false);
        return new FeedViewHolder(postBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        holder.bind(mOrderedPosts.get(position));
    }

    @Override
    public int getItemCount() {
        return mOrderedPosts.size();
    }

    // Helper method to add new posts to RecyclerView
    public void addAll(List<Post> newPosts) {
        // Add all new posts to the unordered list in the chronological order they are given
        mChronologicalPosts.addAll(newPosts);

        // Sort the new posts before adding them to the end of the sorted list
        // This ensures the order of the existing posts will not be changed
        Collections.sort(newPosts, new FeedComparator(mContext));
        mOrderedPosts.addAll(newPosts);

        notifyDataSetChanged();
    }

    // Helper method to clear the RecyclerView
    public void clear() {
        mChronologicalPosts.clear();
        mOrderedPosts.clear();
        notifyDataSetChanged();
    }

    public Post getPost(int position) {
        return mChronologicalPosts.get(position);
    }

    // Returns the oldest post known
    public Post getOldestPost() {
        return mChronologicalPosts.get(mChronologicalPosts.size() - 1);
    }

    public void ignorePost(Post post) {
        int position = mChronologicalPosts.indexOf(post);
        // indexOf returns -1 if the object was not found in the list
        if (position != -1) {
            mChronologicalPosts.remove(post);
            notifyItemRemoved(position);
        }
    }

    public void switchPostLiked(Post post) {
        int position = mOrderedPosts.indexOf(post);
        if (position != -1) {
            FeedViewHolder viewHolder = (FeedViewHolder) mParentRecyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                viewHolder.switchLiked();
            }
        }
    }

    private Bundle getTransitionToDetailView(FeedAdapter.FeedViewHolder postViewHolder) {
        ItemFeedPostBinding postBinding = postViewHolder.getBinding();

        Pair<View, String> pair1 = Pair.create((View) postBinding.postUsername, mContext.getString(R.string.transition_username));
        Pair<View, String> pair2 = Pair.create((View) postBinding.postCreatedAt, mContext.getString(R.string.transition_created_at));
        Pair<View, String> pair3 = Pair.create((View) postBinding.postText, mContext.getString(R.string.transition_text));

        ActivityOptionsCompat transitionOptions;

        // Only include the image in the transition if it is visible
        if (postBinding.postImage.getVisibility() == View.VISIBLE) {
            Pair<View, String> pair4 = Pair.create((View) postBinding.postImage, mContext.getString(R.string.transition_image));
            transitionOptions = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) mContext, pair1, pair2, pair3, pair4);
        } else {
            transitionOptions = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) mContext, pair1, pair2, pair3);
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

            if (LocationPermissions.checkLocationPermission(mContext)) {
                String relativeLocation = LocationUtils.toRelativeLocation(mContext, post.getLocation());
                mBinding.postLocation.setText(relativeLocation);
            } else {
                mBinding.postLocation.setText(null);
            }

            if (post.getImage() != null) {
                mBinding.postImage.setVisibility(View.VISIBLE);
                Log.i(TAG, post.getImage().getUrl());
                Glide.with(mContext)
                        .load(post.getImage().getUrl())
                        //.centerCrop()
                        .into(mBinding.postImage);
            } else {
                mBinding.postImage.setVisibility(View.GONE);
            }

            // Set a listener for single and double clicks
            mBinding.postTouchHolder.setOnTouchListener(createItemTouchListener(post, this));

            // Set alternate listeners on the like and ignore buttons
            mBinding.postLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity parent = (MainActivity) mContext;
                    parent.saveLikeChange(post);
                }
            });

            mBinding.postIgnore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity parent = (MainActivity) mContext;
                    parent.saveIgnore(post);
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
            final GestureDetector gestureDetector = new GestureDetector(mContext, new FeedGestureListener(post, viewHolder));

            View.OnTouchListener touchListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    Log.i(TAG, "Touched");
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
            PostUtils.getUserLikes((User) User.getCurrentUser(), post, likedCallback);
        }

        // Private helper method used to set the visuals depending on current liked status
        private void setLikeVisuals() {
            Drawable toReplace;
            if (mLiked) {
                toReplace = mContext.getDrawable(R.drawable.baseline_star_accent_24);
            } else {
                toReplace = mContext.getDrawable(R.drawable.outline_star_24);
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
            MainActivity parent = (MainActivity) mContext;
            parent.saveLikeChange(mPost);
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Intent detailIntent = new Intent(mContext, PostDetailActivity.class);
            detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_POST, mPost);
            detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_LIKED, mViewHolder.mLiked);

            Bundle transitionBundle = getTransitionToDetailView(mViewHolder);

            Activity parent = (Activity) mContext;
            parent.startActivityForResult(detailIntent, PostDetailActivity.REQUEST_CODE_POST_DETAIL, transitionBundle);
            return true;
        }
    }
}
