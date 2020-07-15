package com.lucasg234.protesttracker.mainactivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.FragmentFeedBinding;
import com.lucasg234.protesttracker.detailactivity.PostDetailActivity;
import com.lucasg234.protesttracker.models.Post;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fragment containing a RecyclerView using FeedAdapter
 * Queries for and displays post from Parse
 */
public class FeedFragment extends Fragment {

    private static final String TAG = "FeedFragment";

    private FragmentFeedBinding mBinding;
    private FeedAdapter mAdapter;
    private EndlessRecyclerViewScrollListener mEndlessScrollListener;

    public FeedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of FeedFragment
     */
    public static FeedFragment newInstance() {
        return new FeedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return FragmentFeedBinding.inflate(inflater, container, false).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding = FragmentFeedBinding.bind(view);

        configureRecyclerView();
        queryInitialPosts();
    }

    private void configureRecyclerView() {
        FeedAdapter.PostInteractionListener interactionListener = new FeedAdapter.PostInteractionListener() {
            @Override
            public void onPostClicked(int position) {
                Intent detailIntent = new Intent(getContext(), PostDetailActivity.class);
                detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_POST, mAdapter.getPosts().get(position));
                startActivity(detailIntent);
            }
        };
        mAdapter = new FeedAdapter(getContext(), new ArrayList<Post>(), interactionListener);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mEndlessScrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                queryAdditionalPosts();
            }
        };

        mBinding.feedRecyclerView.setAdapter(mAdapter);
        mBinding.feedRecyclerView.setLayoutManager(layoutManager);
        mBinding.feedRecyclerView.addOnScrollListener(mEndlessScrollListener);

        // Setup refresh listener which triggers new data loading
        mBinding.feedSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                queryInitialPosts();
            }
        });
    }

    // Removes all posts within the FeedAdapter and replaces them with the result of a new query
    private void queryInitialPosts() {
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        query.addDescendingOrder(Post.KEY_CREATED_AT);
        query.setLimit(Post.QUERY_LIMIT);
        query.include(Post.KEY_AUTHOR);
        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error querying initial posts", e);
                    Toast.makeText(getContext(), getString(R.string.error_load), Toast.LENGTH_SHORT).show();
                    return;
                }
                // Clear any existing posts and add new ones
                mAdapter.clear();
                mAdapter.addAll(posts);
                mBinding.feedSwipeContainer.setRefreshing(false);
                mEndlessScrollListener.resetState();
            }
        });
    }

    // Adds the result of a new query to the end of the FeedAdapter
    private void queryAdditionalPosts() {
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        query.addDescendingOrder(Post.KEY_CREATED_AT);
        query.setLimit(Post.QUERY_LIMIT);
        query.include(Post.KEY_AUTHOR);

        // For new posts, only get posts older [with date less than]
        // the last post in the list
        List<Post> posts = mAdapter.getPosts();
        Date oldestPostDate = posts.get(posts.size() - 1).getCreatedAt();
        query.whereLessThan(Post.KEY_CREATED_AT, oldestPostDate);

        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error querying additional posts", e);
                    Toast.makeText(getContext(), getString(R.string.error_load), Toast.LENGTH_SHORT).show();
                    return;
                }
                // Add new posts, but do not clear old ones
                mAdapter.addAll(posts);
            }
        });
    }
}