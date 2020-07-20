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
import com.parse.CountCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseRelation;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter used by the FeedFragment
 * Holds all posts queried from Parse
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private static final String TAG = "FeedAdapter";

    private Context mContext;
    private PostInteractionListener mInteractionListener;
    private List<Post> mPosts;

    public FeedAdapter(Context context, PostInteractionListener interactionListener) {
        this.mContext = context;
        this.mInteractionListener = interactionListener;
        this.mPosts = new ArrayList<>();
    }

    // This interface handles interaction with the FeedFragment MainActivity on interactions
    // This can be extended to include double taps, long holds, swipes, etc.
    public interface PostInteractionListener {
        void onPostClicked(Post post, int position);

        void onIgnoreClicked(Post post);

        // Requires the position of the post as a parameter to make visual changes
        void onRecommendClicked(Post post, int position);
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
        holder.bind(mPosts.get(position));
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position, @NonNull List<Object> payloads) {
        // Custom payloads made in this format
        // All other calls to this method have empty payloads
        if (payloads.size() == 1 && payloads.get(0) instanceof Boolean) {
            holder.switchLiked((boolean) payloads.get(0));
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    // Helper method to add new posts to RecyclerView
    public void addAll(List<Post> newPosts) {
        mPosts.addAll(newPosts);
        notifyDataSetChanged();
    }

    // Helper method to clear the RecyclerView
    public void clear() {
        // Clears only visible posts. Ignored posts are retained to speed up repeat loading
        mPosts.clear();
        notifyDataSetChanged();
    }

    // Returns the oldest post known, considering visible and ignored list
    public Post getOldestPost() {
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

    class FeedViewHolder extends RecyclerView.ViewHolder {

        private ItemFeedPostBinding mBinding;

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
                        .centerCrop()
                        .into(mBinding.postImage);
            } else {
                mBinding.postImage.setVisibility(View.GONE);
            }

            mBinding.postLayoutContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mInteractionListener.onPostClicked(mPosts.get(getAdapterPosition()), getAdapterPosition());
                }
            });

            mBinding.postIgnoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mInteractionListener.onIgnoreClicked(mPosts.get(getAdapterPosition()));
                }
            });

            mBinding.postRecommendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mInteractionListener.onRecommendClicked(mPosts.get(getAdapterPosition()), getAdapterPosition());
                }
            });

            checkLiked(post);
        }

        private void checkLiked(final Post post) {
            final ParseRelation<User> likedBy = post.getLikedBy();
            ParseQuery likedByQuery = likedBy.getQuery();
            likedByQuery.whereEqualTo(User.KEY_OBJECT_ID, User.getCurrentUser().getObjectId());
            likedByQuery.countInBackground(new CountCallback() {
                @Override
                public void done(int count, ParseException e) {
                    if (e != null) {
                        Log.e(TAG, "Error in determining if post liked on ViewHolder bind", e);
                        return;
                    }
                    switchLiked(count > 0);
                }
            });
        }

        public void switchLiked(boolean liked) {
            int backgroundColorCode = liked ? mContext.getResources().getColor(R.color.colorPrimary)
                    : mContext.getResources().getColor(R.color.colorNone);
            mBinding.getRoot().setBackgroundColor(backgroundColorCode);
        }
    }
}
