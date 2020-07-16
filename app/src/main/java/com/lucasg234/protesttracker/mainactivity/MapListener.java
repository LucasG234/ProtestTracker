package com.lucasg234.protesttracker.mainactivity;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.util.LocationUtils;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * This listener is notified whenever the user moves the camera
 * It handles the turning Parse posts into Markers on the map
 */
public class MapListener implements GoogleMap.OnCameraMoveListener {
    private static final String TAG = "MapListener";

    private Context mContext;
    private GoogleMap mMap;
    // Set used to hold all posts found efficiently without order
    private Set<Post> mStoredPosts;

    public MapListener(Context context, GoogleMap map) {
        this.mContext = context;
        this.mMap = map;
        this.mStoredPosts = new TreeSet<Post>();
    }

    // On Camera move, find the current visibleBounds and query for posts within them
    @Override
    public void onCameraMove() {
        queryPostsInBounds(mMap.getProjection().getVisibleRegion().latLngBounds);
    }

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
                addMarkers(posts);
                mStoredPosts.addAll(posts);
                Log.i(TAG, "Total posts collected: " + mStoredPosts.size());
            }
        });
    }

    private void addMarkers(List<Post> newPosts) {
        newPosts.removeAll(mStoredPosts);
        for (Post post : newPosts) {
            MarkerOptions marker = new MarkerOptions();
            marker.position(LocationUtils.toLatLng(post.getLocation()));
            marker.title(post.getAuthor().getUsername());
            marker.snippet(post.getText());
            mMap.addMarker(marker);
        }
    }
}
