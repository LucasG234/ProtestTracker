package com.lucasg234.protesttracker.mainactivity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.detailactivity.PostDetailActivity;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.lucasg234.protesttracker.util.LocationUtils;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * This listener is notified whenever the user moves the camera
 * It handles the turning Parse posts into Markers on the map
 */
public class MapListener implements GoogleMap.OnCameraMoveListener, GoogleMap.OnInfoWindowClickListener {
    private static final String TAG = "MapListener";

    private Context mContext;
    private GoogleMap mMap;
    // Set used to hold all posts found efficiently without order
    private SearchablePostSet mVisiblePosts;
    private Set mIgnoredPosts;

    public MapListener(Context context, GoogleMap map) {
        this.mContext = context;
        this.mMap = map;
        this.mVisiblePosts = new SearchablePostSet();
        this.mIgnoredPosts = new TreeSet();
    }

    // Called on camera movement
    // Finds the current visibleBounds and query for posts within them
    @Override
    public void onCameraMove() {
        queryPostsInBounds(mMap.getProjection().getVisibleRegion().latLngBounds);
    }

    // Called when the info window of a marker is clicked
    // Launches a PostDetailActivity
    @Override
    public void onInfoWindowClick(Marker marker) {
        Post markerPost = mVisiblePosts.getPostById((String) marker.getTag());

        Intent detailIntent = new Intent(mContext, PostDetailActivity.class);
        detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_POST, markerPost);
        mContext.startActivity(detailIntent);
    }

    // Adds all new posts within current visible bounds to mStoredPosts and calls addMarkers
    public void queryPostsInBounds(LatLngBounds visibleBounds) {
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);

        ParseGeoPoint southwest = new ParseGeoPoint(visibleBounds.southwest.latitude, visibleBounds.southwest.longitude);
        ParseGeoPoint northeast = new ParseGeoPoint(visibleBounds.northeast.latitude, visibleBounds.northeast.longitude);
        query.whereWithinGeoBox(Post.KEY_LOCATION, southwest, northeast);

        query.addDescendingOrder(Post.KEY_CREATED_AT);
        query.setLimit(Post.QUERY_LIMIT);
        query.include(Post.KEY_AUTHOR);
        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error querying posts for markers");
                    Toast.makeText(mContext, mContext.getString(R.string.error_load), Toast.LENGTH_SHORT).show();
                }
                checkNewPosts(posts);
                Log.i(TAG, "Total viewable posts collected: " + mVisiblePosts.size());
                Log.i(TAG, "Total ignored posts collected: " + mIgnoredPosts.size());
            }
        });
    }

    // Separates only new posts from query, then adds them to mIgnoredPosts or mVisiblePosts
    // All posts in mVisiblePosts will have a marker on the screen
    private void checkNewPosts(List<Post> newPosts) {
        // Remove all duplicated posts
        newPosts.removeAll(mVisiblePosts);
        newPosts.removeAll(mIgnoredPosts);

        // Check whether new posts should be ignored
        for (final Post post : newPosts) {
            ParseQuery<User> ignoredQuery = post.getIgnoredBy().getQuery();
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
                        mIgnoredPosts.add(post);
                    } else {
                        mVisiblePosts.add(post);
                        addPostMarker(post);
                    }
                }
            });
        }
    }

    // Adds markers to map for each new post
    private void addPostMarker(Post newPost) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(LocationUtils.toLatLng(newPost.getLocation()));
        markerOptions.title(newPost.getAuthor().getUsername());
        markerOptions.snippet(newPost.getText());

        Marker marker = mMap.addMarker(markerOptions);
        // Tag is used to identify which post this marker represents
        marker.setTag(newPost.getObjectId());
    }

    private class SearchablePostSet extends TreeSet<Post> {
        public Post getPostById(String objectId) {
            Iterator<Post> iter = this.descendingIterator();
            while (iter.hasNext()) {
                Post post = iter.next();
                if (post.getObjectId().equals(objectId))
                    return post;
            }
            return null;
        }
    }
}
