package com.lucasg234.protesttracker.mainactivity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lucasg234.protesttracker.databinding.ItemFeedPostBinding;
import com.lucasg234.protesttracker.models.Post;

import java.util.List;

/**
 * RecyclerView Adapter used by the FeedFragment
 * Holds all posts queried from Parse
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private static final String TAG = "FeedAdapter";

    private Context mContext;
    private List<Post> mPosts;

    public FeedAdapter(Context context, List<Post> posts) {
        this.mContext = context;
        this.mPosts = posts;
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

    class FeedViewHolder extends RecyclerView.ViewHolder {

        private ItemFeedPostBinding mBinding;

        public FeedViewHolder(@NonNull ItemFeedPostBinding binding) {
            super(binding.getRoot());

            this.mBinding = binding;
        }

        public void bind(Post post) {
            mBinding.postText.setText(post.getText());
            mBinding.postUsername.setText(post.getAuthor().getUsername());
        }
    }
}
