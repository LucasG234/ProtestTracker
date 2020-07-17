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
import com.lucasg234.protesttracker.util.LocationUtils;
import com.lucasg234.protesttracker.util.Utils;
import com.parse.CountCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * RecyclerView Adapter used by the FeedFragment
 * Holds all posts queried from Parse
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private static final String TAG = "FeedAdapter";

    public static final int NUMBER_POSTS_VISIBLE = 5;

    private Context mContext;
    private PostInteractionListener mInteractionListener;
    private List<Post> mVisiblePosts;
    private SortedSet<Post> mIgnoredPosts;

    public FeedAdapter(Context context, PostInteractionListener interactionListener) {
        this.mContext = context;
        this.mInteractionListener = interactionListener;

        this.mVisiblePosts = new ArrayList<>();
        this.mIgnoredPosts = new TreeSet<>();
    }


    // This interface handles interaction with the FeedFragment MainActivity on interactions
    // This can be extended to include double taps, long holds, swipes, etc.
    public interface PostInteractionListener {
        void onPostClicked(Post post);

        void onIgnoreClicked(Post post);

        // Requires the ViewHolder as a parameter to make visual changes
        void onRecommendClicked(Post post, FeedViewHolder feedViewHolder);
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
    public void addAll(List<Post> newPosts) {
        // Store old number of posts
        int oldSize = getItemCount();
        // Add new posts, but do not update visually yet
        mVisiblePosts.addAll(newPosts);
        // If there are few other posts, check whether these are ignored immediately
        if (oldSize < NUMBER_POSTS_VISIBLE) {
            checkIgnoredInForeground(oldSize, NUMBER_POSTS_VISIBLE - oldSize);
        }
        // Allow the adapter to start loading in posts, then check all posts in background
        notifyDataSetChanged();
        checkIgnoredInBackground(oldSize);
    }

    // Helper method to clear the RecyclerView
    public void clear() {
        // Clears only visible posts. Ignored posts are retained to speed up repeat loading
        mVisiblePosts.clear();
        notifyDataSetChanged();
    }

    // Returns the oldest post known, considering visible and ignored list
    public Post getOldestPost() {
        Post oldestVisible = mVisiblePosts.get(mVisiblePosts.size() - 1);
        Post oldestIgnored = mIgnoredPosts.last();
        return oldestVisible.compareTo(oldestIgnored) < 0 ? oldestVisible : oldestIgnored;
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


    // This method checks numberVisible posts starting from positionStart (inclusive)
    // If it determines that they are ignored, and removes them from the visible posts list if they are
    // Will continue checking posts until it finds numberVisible posts which are not ignored
    // Should be used for all posts which will be initially visible
    private void checkIgnoredInForeground(int positionStart, int numberVisible) {
        ListIterator<Post> iter = mVisiblePosts.listIterator(positionStart);
        while (iter.hasNext()) {
            final Post currPost = iter.next();
            if (mIgnoredPosts.contains(currPost)) {
                iter.remove();
                continue;
            }
            ParseQuery<User> ignoredQuery = currPost.getIgnoredBy().getQuery();
            ignoredQuery.whereEqualTo(User.KEY_OBJECT_ID, User.getCurrentUser().getObjectId());
            boolean ignored;
            try {
                ignored = ignoredQuery.count() > 0;
            } catch (ParseException e) {
                // On an error case, we will assume the post is not ignored and allow the user to continue scrolling
                Log.e(TAG, "Error checking ignored status", e);
                ignored = false;
            }

            if (ignored) {
                // Can't call ignore post because adapter has not been informed of any of the new data yet
                iter.remove();
                mIgnoredPosts.add(currPost);
            } else {
                // Use numberVisible variable as a counter for how many non-ignored posts we have found
                numberVisible--;
                if (numberVisible == 0) {
                    return;
                }
            }
        }
    }

    // This method checks all posts from positionStart to the end of the adapter
    // If it determines that they are ignored, it removes them from the visible posts list if they are
    // Should be used for all posts which are not initially visible
    private void checkIgnoredInBackground(int positionStart) {
        ListIterator<Post> iter = mVisiblePosts.listIterator(positionStart);
        // Generate all queries and store in map
        // Queries are stored as keys associated with their posts
        Map<ParseQuery, Post> queries = new LinkedHashMap<>();
        while (iter.hasNext()) {
            final Post currPost = iter.next();
            ParseQuery<User> ignoredQuery = currPost.getIgnoredBy().getQuery();
            ignoredQuery.whereEqualTo(User.KEY_OBJECT_ID, User.getCurrentUser().getObjectId());
            queries.put(ignoredQuery, currPost);
        }

        // Send out all queries at the same time to avoid errors
        for (ParseQuery ignoredQuery : queries.keySet()) {
            Post post = queries.get(ignoredQuery);
            if (mIgnoredPosts.contains(post)) {
                ignorePost(post);
            } else {
                ignoredQuery.countInBackground(new IgnoredBackgroundCallback(post));
            }
        }
    }

    class IgnoredBackgroundCallback implements CountCallback {

        private Post mPost;

        public IgnoredBackgroundCallback(Post post) {
            this.mPost = post;
        }

        @Override
        public void done(int count, ParseException e) {
            if (e != null) {
                // On an error case, we will assume the post is not ignored and allow the user to continue scrolling
                Log.e(TAG, "Error checking ignored status", e);
                return;
            }
            if (count > 0) {
                ignorePost(mPost);
            }
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
                    mInteractionListener.onRecommendClicked(mVisiblePosts.get(getAdapterPosition()), FeedViewHolder.this);
                }
            });
        }

        public void setLiked(boolean liked) {
            if (liked) {
                mBinding.getRoot().setBackgroundColor(mContext.getResources().getColor(R.color.colorPrimary));
            } else {
                // 0 represents no background color
                mBinding.getRoot().setBackgroundColor(mContext.getResources().getColor(R.color.colorNone));
            }
        }
    }
}
