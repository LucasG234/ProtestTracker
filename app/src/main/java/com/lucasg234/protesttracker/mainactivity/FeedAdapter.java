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
import com.lucasg234.protesttracker.permissions.LocationPermissions;
import com.lucasg234.protesttracker.util.LocationUtils;
import com.lucasg234.protesttracker.util.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView Adapter used by the FeedFragment
 * Holds all posts queried from Parse
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private static final String TAG = "FeedAdapter";

    private Context mContext;
    private PostInteractionListener mInteractionListener;
    private List<Post> mPosts;
    private Set<Post> mIgnored;

    public FeedAdapter(Context context, PostInteractionListener interactionListener) {
        this.mContext = context;
        this.mInteractionListener = interactionListener;

        this.mPosts = new ArrayList<>();
        this.mIgnored = new HashSet<>();
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
        holder.bind(mPosts.get(position));
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    // Helper method to add new posts to RecyclerView
    public void addAll(List<Post> posts) {
        mPosts.addAll(posts);
        notifyDataSetChanged();
    }

    // Helper method to clear the RecyclerView
    public void clear() {
        mPosts.clear();
        notifyDataSetChanged();
    }

    // Helper method allowing other classes to get Posts from the RecyclerView
    public List<Post> getPosts() {
        return mPosts;
    }

    public void ignorePost(Post post) {
        int position = mPosts.indexOf(post);
        // indexOf returns -1 if the object was not found in the list
        if (position != -1) {
            mPosts.remove(post);
            mIgnored.add(post);
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
                    mInteractionListener.onPostClicked(mPosts.get(getAdapterPosition()));
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
                    mInteractionListener.onRecommendClicked(mPosts.get(getAdapterPosition()));
                }
            });
        }
    }
}
