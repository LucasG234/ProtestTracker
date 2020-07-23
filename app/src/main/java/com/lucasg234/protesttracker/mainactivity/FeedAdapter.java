package com.lucasg234.protesttracker.mainactivity;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.ItemFeedPostBinding;
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

    public FeedAdapter(Context context) {
        this.mContext = context;
        this.mChronologicalPosts = new ArrayList<>();
        this.mOrderedPosts = new ArrayList<>();
    }

    public void setParentRecyclerView(RecyclerView parent) {
        this.mParentRecyclerView = parent;
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
        int position = mChronologicalPosts.indexOf(post);
        if (position != -1) {
            FeedViewHolder viewHolder = (FeedViewHolder) mParentRecyclerView.findViewHolderForAdapterPosition(position);
            viewHolder.switchLiked();
        }
    }

    class FeedViewHolder extends RecyclerView.ViewHolder {

        private ItemFeedPostBinding mBinding;
        private boolean mLiked;

        public FeedViewHolder(@NonNull ItemFeedPostBinding binding) {
            super(binding.getRoot());

            this.mBinding = binding;
        }

        public void bind(Post post) {
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

            // Liked state defaults to false and may switched after it is checked
            mLiked = false;
            checkLiked(post);
        }

        private void checkLiked(final Post post) {
            FunctionCallback<Boolean> likedCallback = new FunctionCallback<Boolean>() {
                @Override
                public void done(Boolean liked, ParseException e) {
                    if (e != null) {
                        Log.e(TAG, "Error in determining if post liked on ViewHolder bind", e);
                        return;
                    }
                    if (liked) {
                        switchLiked();
                    }
                }
            };
            PostUtils.getUserLikes((User) User.getCurrentUser(), post, likedCallback);
        }

        public void switchLiked() {
            mLiked = !mLiked;
            int backgroundColorCode = mLiked ? mContext.getResources().getColor(R.color.colorPrimary)
                    : mContext.getResources().getColor(R.color.colorNone);
            mBinding.getRoot().setBackgroundColor(backgroundColorCode);
        }

        public ItemFeedPostBinding getBinding() {
            return mBinding;
        }
    }
}
