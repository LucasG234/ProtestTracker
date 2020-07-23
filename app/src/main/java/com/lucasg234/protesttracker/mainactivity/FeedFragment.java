package com.lucasg234.protesttracker.mainactivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.FragmentFeedBinding;
import com.lucasg234.protesttracker.databinding.ItemFeedPostBinding;
import com.lucasg234.protesttracker.detailactivity.PostDetailActivity;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.lucasg234.protesttracker.permissions.LocationPermissions;
import com.lucasg234.protesttracker.util.PostUtils;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
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
        mAdapter = new FeedAdapter(getContext());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mEndlessScrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                queryAdditionalPosts();
            }
        };

        RecyclerView.OnItemTouchListener itemTouchListener = createItemTouchListener();

        mBinding.feedRecyclerView.setAdapter(mAdapter);
        mBinding.feedRecyclerView.setLayoutManager(layoutManager);
        mBinding.feedRecyclerView.addOnScrollListener(mEndlessScrollListener);
        mBinding.feedRecyclerView.addOnItemTouchListener(itemTouchListener);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(mAdapter));
        itemTouchHelper.attachToRecyclerView(mBinding.feedRecyclerView);
        mAdapter.setParentRecyclerView(mBinding.feedRecyclerView);

        // Setup refresh listener which triggers new data loading
        mBinding.feedSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                queryInitialPosts();
            }
        });
    }

    private RecyclerView.OnItemTouchListener createItemTouchListener() {
        final GestureDetector gestureDetector = new GestureDetector(new FeedGestureListener());

        RecyclerView.OnItemTouchListener touchListener = new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                return gestureDetector.onTouchEvent(e);
            }
        };

        return touchListener;
    }

    // Removes all posts within the FeedAdapter and replaces them with the result of a new query
    private void queryInitialPosts() {
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        query.addDescendingOrder(Post.KEY_CREATED_AT);
        query.setLimit(Post.QUERY_LIMIT);
        query.include(Post.KEY_AUTHOR);

        // Removes posts which are ignored
        query.whereNotEqualTo(Post.KEY_IGNORED_BY, User.getCurrentUser());

        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error querying initial posts", e);
                    Toast.makeText(getContext(), R.string.error_load, Toast.LENGTH_SHORT).show();
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

        // Removes posts which are ignored
        query.whereNotEqualTo(Post.KEY_IGNORED_BY, User.getCurrentUser());

        // For new posts, only get posts older [with date less than] the last post
        Date oldestPostDate = mAdapter.getOldestPost().getCreatedAt();
        query.whereLessThan(Post.KEY_CREATED_AT, oldestPostDate);

        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error querying additional posts", e);
                    Toast.makeText(getContext(), R.string.error_load, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Add new posts, but do not clear old ones
                mAdapter.addAll(posts);
            }
        });
    }

    public void ignorePost(Post post) {
        PostUtils.addIgnoredBy((User) User.getCurrentUser(), post);
        post.saveInBackground();

        mAdapter.ignorePost(post);
    }

    public void changePostLiked(final Post post) {
        final ParseRelation<User> likedBy = post.getLikedBy();
        FunctionCallback<Boolean> likedCallback = new FunctionCallback<Boolean>() {
            @Override
            public void done(final Boolean liked, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error in determining if post liked on ViewHolder bind", e);
                    Toast.makeText(getContext(), R.string.error_liking, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (liked) {
                    likedBy.remove((User) User.getCurrentUser());
                } else {
                    likedBy.add((User) User.getCurrentUser());
                }

                // Reverse visual liked state immediately
                mAdapter.switchPostLiked(post);

                post.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Log.e(TAG, "Error saving change to like status", e);
                            Toast.makeText(getContext(), R.string.error_liking, Toast.LENGTH_SHORT).show();
                            // Change liked state back in rare case of failure
                            mAdapter.switchPostLiked(post);
                        }
                    }
                });
            }
        };

        PostUtils.getUserLikes((User) User.getCurrentUser(), post, likedCallback);
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


    private Bundle getTransitionToDetailView(FeedAdapter.FeedViewHolder postViewHolder) {
        ItemFeedPostBinding postBinding = postViewHolder.getBinding();

        Pair<View, String> pair1 = Pair.create((View) postBinding.postUsername, getString(R.string.transition_username));
        Pair<View, String> pair2 = Pair.create((View) postBinding.postCreatedAt, getString(R.string.transition_created_at));
        Pair<View, String> pair3 = Pair.create((View) postBinding.postText, getString(R.string.transition_text));

        ActivityOptionsCompat transitionOptions;

        // Only include the image in the transition if it is visible
        if (postBinding.postImage.getVisibility() == View.VISIBLE) {
            Pair<View, String> pair4 = Pair.create((View) postBinding.postImage, getString(R.string.transition_image));
            transitionOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), pair1, pair2, pair3, pair4);
        } else {
            transitionOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), pair1, pair2, pair3);
        }

        return transitionOptions.toBundle();
    }

    /**
     * Class which listens to all gestures made on the RecyclerView
     * Responds only to double taps and confirmed single taps
     */
    private class FeedGestureListener extends GestureDetector.SimpleOnGestureListener {
        // Measurements made in pixels and pixels / second
        private static final int SWIPE_MIN_HORIZONTAL_DISTANCE = 200;
        private static final int SWIPE_MAX_VERTICAL_DISTANCE = 100;
        private static final int SWIPE_MAX_VERTICAL_VELOCITY = 1000;
        private static final int SWIPE_MIN_HORIZONTAL_VELOCITY = 2000;

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            int position = getEventPosition(e);
            changePostLiked(mAdapter.getPost(position));
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            int position = getEventPosition(e);
            Post post = mAdapter.getPost(position);
            Intent detailIntent = new Intent(getContext(), PostDetailActivity.class);
            detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_POST, post);

            FeedAdapter.FeedViewHolder postViewHolder = (FeedAdapter.FeedViewHolder) mBinding.feedRecyclerView.findViewHolderForAdapterPosition(position);
            Bundle transitionBundle = getTransitionToDetailView(postViewHolder);

            getActivity().startActivityForResult(detailIntent, PostDetailActivity.REQUEST_CODE_POST_DETAIL, transitionBundle);
            return true;
        }

        private int getEventPosition(MotionEvent e) {
            View childView = mBinding.feedRecyclerView.findChildViewUnder(e.getX(), e.getY());
            return mBinding.feedRecyclerView.getChildLayoutPosition(childView);
        }
    }

    /**
     * Class which handles RecyclerView items being swiped to the sides
     */
    public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private FeedAdapter mAdapter;

        public SwipeToDeleteCallback(FeedAdapter adapter) {
            // 0 value indicates no support for dragging items up and down
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            this.mAdapter = adapter;
        }

        // No additional functionality when items are moved
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        // Whenever a post is swiped off of the screen, ignore it
        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            mAdapter.ignorePost(mAdapter.getPost(position));
        }
    }
}