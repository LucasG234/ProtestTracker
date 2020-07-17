package com.lucasg234.protesttracker.mainactivity;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lucasg234.protesttracker.databinding.ItemFeedPostBinding;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.lucasg234.protesttracker.permissions.LocationPermissions;
import com.lucasg234.protesttracker.util.LocationUtils;
import com.lucasg234.protesttracker.util.Utils;
import com.parse.CountCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * RecyclerView Adapter used by the FeedFragment
 * Holds all posts queried from Parse
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private static final String TAG = "FeedAdapter";

    private Context mContext;
    private PostInteractionListener mInteractionListener;
    private List<Post> mVisiblePosts;
    private Set<Post> mIgnoredPosts;

    public FeedAdapter(Context context, PostInteractionListener interactionListener) {
        this.mContext = context;
        this.mInteractionListener = interactionListener;

        this.mVisiblePosts = new ArrayList<>();
        this.mIgnoredPosts = new HashSet<>();
    }

    // This interface handles interaction with the FeedFragment MainActivity on interactions
    // This can be extended to include double taps, long holds, swipes, etc.
    public interface PostInteractionListener {
        void onPostClicked(Post post);

        void onIgnoreClicked(Post post);

        void onRecommendClicked(Post post);
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
        holder.bind(mVisiblePosts.get(position));
    }

    @Override
    public int getItemCount() {
        return mVisiblePosts.size();
    }

    // Helper method to add new posts to RecyclerView
    public void addAll(List<Post> posts) {
        mVisiblePosts.addAll(posts);
        notifyDataSetChanged();
    }

    // Helper method to clear the RecyclerView
    public void clear() {
        mVisiblePosts.clear();
        notifyDataSetChanged();
    }

    // Helper method allowing other classes to get Posts from the RecyclerView
    public List<Post> getPosts() {
        return mVisiblePosts;
    }

    public void ignorePost(Post post) {
        int position = mVisiblePosts.indexOf(post);
        // indexOf returns -1 if the object was not found in the list
        if (position != -1) {
            mVisiblePosts.remove(post);
            mIgnoredPosts.add(post);
            notifyItemRemoved(position);
        }
    }

    // This method checks all posts from positionStart to the end of the adapter
    // If it determines that they are ignored, it removes them from the visible posts list
    // Should be used for all posts which are not initially visible
    public void checkIgnoredInBackground(int positionStart) {
        ListIterator<Post> iter = mVisiblePosts.listIterator(positionStart);
        while (iter.hasNext()) {
            final Post currPost = iter.next();
            ParseQuery<User> ignoredQuery = currPost.getIgnoredBy().getQuery();
            ignoredQuery.whereEqualTo(User.KEY_OBJECT_ID, User.getCurrentUser().getObjectId());
            ignoredQuery.countInBackground(new CountCallback() {
                @Override
                public void done(int count, ParseException e) {
                    if (e != null) {
                        // On an error case, we will assume the post is not ignored and allow the user to continue scrolling
                        Log.e(TAG, "Error checking ignored status", e);
                        return;
                    }
                    if (count > 0) {
                        ignorePost(currPost);
                    }
                }
            });
        }
    }


    class FeedViewHolder extends RecyclerView.ViewHolder {

        private ItemFeedPostBinding mBinding;

        public FeedViewHolder(@NonNull ItemFeedPostBinding binding) {
            super(binding.getRoot());

            this.mBinding = binding;
        }

        public void bind(Post post) {
            mBinding.postText.setText(post.getText());
            mBinding.postUsername.setText(post.getAuthor().getUsername());

            String relativeCreationTime = Utils.dateToRelative(post.getCreatedAt());
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
                        .centerCrop()
                        .into(mBinding.postImage);
            } else {
                mBinding.postImage.setVisibility(View.GONE);
            }

            mBinding.postLayoutContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mInteractionListener.onPostClicked(mVisiblePosts.get(getAdapterPosition()));
                }
            });

            mBinding.postIgnoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mInteractionListener.onIgnoreClicked(mVisiblePosts.get(getAdapterPosition()));
                }
            });

            mBinding.postRecommendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mInteractionListener.onRecommendClicked(mVisiblePosts.get(getAdapterPosition()));
                }
            });
        }
    }
}
