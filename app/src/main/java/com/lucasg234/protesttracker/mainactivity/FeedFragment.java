package com.lucasg234.protesttracker.mainactivity;

import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.lucasg234.protesttracker.models.User;
import com.lucasg234.protesttracker.permissions.LocationPermissions;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.SaveCallback;

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

        // Ask for location permissions before loading posts into the feed
        // If they are not given, posts will load without relative positions
        if (!LocationPermissions.checkLocationPermission(getContext())) {
            LocationPermissions.requestLocationPermission(this);
        }
        queryInitialPosts();
    }

    private void configureRecyclerView() {
        FeedAdapter.PostInteractionListener interactionListener = new FeedInteractionListener();
        mAdapter = new FeedAdapter(getContext(), interactionListener);

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

        // For new posts, only get posts older [with date less than] the last post
        Date oldestPostDate = mAdapter.getOldestPost().getCreatedAt();
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

    private void ignorePost(Post post) {
        ParseRelation<User> ignoredBy = post.getIgnoredBy();
        ignoredBy.add((User) User.getCurrentUser());
        post.saveInBackground();

        mAdapter.ignorePost(post);
    }

    public void recommendPost(final Post post, final int position) {
        final ParseRelation<User> likedBy = post.getLikedBy();
        ParseQuery likedByQuery = likedBy.getQuery();
        likedByQuery.whereEqualTo(User.KEY_OBJECT_ID, User.getCurrentUser().getObjectId());
        likedByQuery.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error in determining if post liked on recommend button clicked", e);
                    Toast.makeText(getContext(), getString(R.string.error_liking), Toast.LENGTH_SHORT).show();
                    return;
                }
                final boolean liked = count > 0;
                if (liked) {
                    likedBy.remove((User) User.getCurrentUser());
                    post.saveInBackground();
                } else {
                    likedBy.add((User) User.getCurrentUser());
                    post.saveInBackground();
                }
                post.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Log.e(TAG, "Error saving change to like status", e);
                            Toast.makeText(getContext(), getString(R.string.error_liking), Toast.LENGTH_SHORT).show();
                        }

                        // Reverse liked due to change
                        mAdapter.notifyItemChanged(position, !liked);
                    }
                });
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // No need to check resultCode because both RESULT_OK and RESULT_CANCELED are accepted

        switch (requestCode) {
            case PostDetailActivity.REQUEST_CODE_POST_DETAIL:
                Post post = data.getParcelableExtra(PostDetailActivity.KEY_RESULT_POST);
                if (data.getBooleanExtra(PostDetailActivity.KEY_RESULT_RECOMMENDED, false)) {
                    int position = data.getIntExtra(PostDetailActivity.KEY_RESULT_POSITION, -1);
                    recommendPost(post, position);
                }
                if (data.getBooleanExtra(PostDetailActivity.KEY_RESULT_IGNORED, false)) {
                    ignorePost(post);
                }
                break;
            default:
                Log.e(TAG, "Received onActivityResult with unknown request code:" + requestCode);
                return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // If permission was just granted to allow location services, then notify the adapter
        // This will rebind the currently viewed posts with the location information
        if (requestCode == LocationPermissions.REQUEST_CODE_LOCATION_PERMISSIONS && permissions.length >= 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private class FeedInteractionListener implements FeedAdapter.PostInteractionListener {

        // Open a PostDetailActivity on the clicked post
        @Override
        public void onPostClicked(Post post, int position) {
            Intent detailIntent = new Intent(getContext(), PostDetailActivity.class);
            detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_POST, post);
            detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_POSITION, position);
            startActivityForResult(detailIntent, PostDetailActivity.REQUEST_CODE_POST_DETAIL);
        }

        // Add the current user to the ignoredBy relation for the post
        // Then remove it from the adapter
        @Override
        public void onIgnoreClicked(Post post) {
            ignorePost(post);
        }

        // First check whether the post is already liked by the User
        // Then like or unlike it through the adapter
        @Override
        public void onRecommendClicked(final Post post, final int position) {
            recommendPost(post, position);
        }
    }
}