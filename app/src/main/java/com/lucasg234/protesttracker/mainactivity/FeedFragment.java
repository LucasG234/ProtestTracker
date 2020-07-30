package com.lucasg234.protesttracker.mainactivity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.FragmentFeedBinding;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.lucasg234.protesttracker.permissions.LocationPermissions;
import com.lucasg234.protesttracker.util.LocationUtils;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

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
    private int mFeetFilter;

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

        // Set up spinner for distance selection
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getContext(), R.array.spinner_options, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBinding.feedFilterSpinner.setAdapter(spinnerAdapter);

        // Spinner's onItemSelected will be called immediately when feed first opens
        mBinding.feedFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mFeetFilter = getResources().getIntArray(R.array.spinner_options_feet)[position];
                queryClearPosts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No work done here
            }
        });
    }

    private void configureRecyclerView() {
        mAdapter = new FeedAdapter(getContext(), mBinding.feedRecyclerView);

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

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(mAdapter));
        itemTouchHelper.attachToRecyclerView(mBinding.feedRecyclerView);

        // Setup refresh listener which triggers new data loading
        mBinding.feedSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                queryClearPosts();
            }
        });
    }

    // Removes all posts within the FeedAdapter and replaces them with the result of a new query
    private void queryClearPosts() {
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        query.addDescendingOrder(Post.KEY_CREATED_AT);
        query.setLimit(Post.QUERY_LIMIT);
        query.include(Post.KEY_AUTHOR);

        // Removes posts which are ignored
        query.whereNotEqualTo(Post.KEY_IGNORED_BY, User.getCurrentUser());

        // Only include posts with distance filter if one exists
        if (mFeetFilter > 0) {
            double miles = mFeetFilter / LocationUtils.MILES_TO_FEET;
            ParseGeoPoint currentLocation = LocationUtils.toParseGeoPoint(LocationUtils.getCurrentLocation(getContext()));
            query.whereWithinMiles(Post.KEY_LOCATION, currentLocation, miles);
        }

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
                mBinding.feedRecyclerView.scrollToPosition(0);
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

        // Only include posts with distance filter if one exists
        if (mFeetFilter > 0) {
            double miles = mFeetFilter / LocationUtils.MILES_TO_FEET;
            ParseGeoPoint currentLocation = LocationUtils.toParseGeoPoint(LocationUtils.getCurrentLocation(getContext()));
            query.whereWithinMiles(Post.KEY_LOCATION, currentLocation, miles);
        }

        // For new posts, only get posts older [with date less than] the last post
        Date oldestPostDate = mAdapter.getLastPost().getCreatedAt();
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
        mAdapter.ignorePost(post);
    }

    public void changePostLiked(final Post post) {
        mAdapter.switchPostLiked(post);
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

    public FragmentFeedBinding getBinding() {
        return mBinding;
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
            MainActivity parent = (MainActivity) getActivity();
            parent.saveIgnore(mAdapter.getPost(position));
        }
    }
}