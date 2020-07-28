package com.lucasg234.protesttracker.mainactivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.detailactivity.PostDetailActivity;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.lucasg234.protesttracker.util.LocationUtils;
import com.lucasg234.protesttracker.util.PostUtils;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * This listener is notified whenever the user moves the camera
 * It handles the turning Parse posts into Markers on the map
 */
public class MapListener implements GoogleMap.OnCameraMoveListener, GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnMyLocationButtonClickListener {

    private static final int FASTEST_INTERVAL_MS = 100;
    private static final String TAG = "MapListener";

    private Context mContext;
    private MapFragment mParent;
    private GoogleMap mMap;
    // Set used to hold objects efficiently without order
    private SearchableSet<Post> mPosts;
    private SearchableSet<Marker> mMarkers;
    private Date lastQuery;
    private float mLikedHue;
    private float mUnlikedHue;

    public MapListener(MapFragment parent, GoogleMap map) {
        this.mParent = parent;
        this.mContext = parent.getContext();
        this.mMap = map;
        this.mPosts = new SearchableSet<>(new SearchableSet.Accessor<Post>() {
            @Override
            public int compare(Post post1, Post post2) {
                return post1.compareTo(post2);
            }

            @Override
            public String accessId(Post post) {
                return post.getObjectId();
            }
        });
        this.mMarkers = new SearchableSet<>(new SearchableSet.Accessor<Marker>() {
            @Override
            public int compare(Marker m1, Marker m2) {
                return Integer.compare(m1.hashCode(), m2.hashCode());
            }

            @Override
            public String accessId(Marker marker) {
                return marker == null ? null : (String) marker.getTag();
            }
        });

        calculateMapHues();
    }

    // Called on camera movement
    // Finds the current visibleBounds and query for posts within them
    @Override
    public void onCameraMove() {
        // If the last query was within FASTEST_INTERVAL_MS, ignore this method call
        if (lastQuery != null && new Date().getTime() - lastQuery.getTime() < FASTEST_INTERVAL_MS) {
            return;
        }
        lastQuery = new Date();
        queryPostsInBounds(mMap.getProjection().getVisibleRegion().latLngBounds);
    }

    // Called when the info window of a marker is clicked
    // Launches a PostDetailActivity
    @Override
    public void onInfoWindowClick(Marker marker) {
        Post markerPost = mPosts.getObjectById((String) marker.getTag());

        final Intent detailIntent = new Intent(mContext, PostDetailActivity.class);
        detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_POST, markerPost);

        FunctionCallback<Boolean> likedCallback = new FunctionCallback<Boolean>() {
            @Override
            public void done(Boolean liked, ParseException e) {
                Log.i(TAG, "" + liked);
                detailIntent.putExtra(PostDetailActivity.KEY_INTENT_EXTRA_LIKED, liked);
                mParent.getActivity().startActivityForResult(detailIntent, PostDetailActivity.REQUEST_CODE_POST_DETAIL);
            }
        };

        PostUtils.getUserLikes((User) User.getCurrentUser(), markerPost, likedCallback);
    }

    // Call when a camera movement is started
    // If the movement was started by gesture, turn off automatic camera movements
    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            mParent.setFollowUser(false);
        }
    }

    // Called when the my-location button is clicked
    // When user focuses back to their location, turn on the automatic camera movements
    @Override
    public boolean onMyLocationButtonClick() {
        mParent.setFollowUser(true);
        return false;
    }

    // Adds all new posts within current visible bounds to mStoredPosts and calls addMarkers
    public void queryPostsInBounds(LatLngBounds visibleBounds) {
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);

        ParseGeoPoint southwest = new ParseGeoPoint(visibleBounds.southwest.latitude, visibleBounds.southwest.longitude);
        ParseGeoPoint northeast = new ParseGeoPoint(visibleBounds.northeast.latitude, visibleBounds.northeast.longitude);
        query.whereWithinGeoBox(Post.KEY_LOCATION, southwest, northeast);

        // Removes posts which are ignored
        query.whereNotEqualTo(Post.KEY_IGNORED_BY, User.getCurrentUser());

        query.addDescendingOrder(Post.KEY_CREATED_AT);
        query.setLimit(Post.QUERY_LIMIT);
        query.include(Post.KEY_AUTHOR);
        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error querying posts for markers");
                    Toast.makeText(mContext, R.string.error_load, Toast.LENGTH_SHORT).show();
                }
                // Remove all duplicates before adding markers
                if (posts != null) {
                    posts.removeAll(mPosts);
                    Log.i(TAG, "new posts collected: " + posts.size());
                    addPostMarkers(posts);

                    mPosts.addAll(posts);
                }
                Log.i(TAG, "Total viewable posts collected: " + mPosts.size());
            }
        });
    }

    // Public method used by MapFragment when a post is ignored
    public void removeMarker(Post post) {
        Marker marker = mMarkers.getObjectById(post.getObjectId());
        if (marker == null) {
            Log.e(TAG, "Attempted to remove null marker");
            return;
        }

        marker.remove();
        mMarkers.remove(marker);
    }

    // Public method used by MapFragment when the like status of a post changes
    public void changeMarkerLikeStatus(Post post) {
        Marker marker = mMarkers.getObjectById(post.getObjectId());
        if (marker == null) {
            Log.e(TAG, "Attempted to remove change like status of a null marker");
            return;
        }

        setVisualLikeStatus(post, marker);
    }

    // Calculates the hues for map colors on initialization
    private void calculateMapHues() {
        float[] hsvHolder = new float[3];

        int likedColor = ContextCompat.getColor(mContext, R.color.colorAccent);
        Color.RGBToHSV(Color.red(likedColor), Color.green(likedColor), Color.blue(likedColor), hsvHolder);
        mLikedHue = hsvHolder[0];

        int unlikedColor = ContextCompat.getColor(mContext, R.color.colorComplementary);
        Color.RGBToHSV(Color.red(unlikedColor), Color.green(unlikedColor), Color.blue(unlikedColor), hsvHolder);
        mUnlikedHue = hsvHolder[0];
    }

    // Adds markers to map for each new post
    private void addPostMarkers(List<Post> newPosts) {
        for (Post newPost : newPosts) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(LocationUtils.toLatLng(newPost.getLocation()));
            markerOptions.title(newPost.getAuthor().getUsername());
            markerOptions.snippet(newPost.getText());

            Marker marker = mMap.addMarker(markerOptions);
            // Tag is used to identify which post this marker represents
            marker.setTag(newPost.getObjectId());
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(mUnlikedHue));
            mMarkers.add(marker);

            // Query for liked status after adding the marker
            setVisualLikeStatus(newPost, marker);
        }
    }

    // Checks a post's like status and changes its visuals to match
    private void setVisualLikeStatus(final Post post, final Marker marker) {
        FunctionCallback<Boolean> likedCallback = new FunctionCallback<Boolean>() {
            @Override
            public void done(Boolean liked, ParseException e) {
                if (e != null) {
                    // Silent error here, otherwise many error messages will appear at once
                    Log.e(TAG, "Error when querying post like status from map");
                    return;
                }

                if (liked) {
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(mLikedHue));
                } else {
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(mUnlikedHue));
                }
            }
        };

        PostUtils.getUserLikes((User) User.getCurrentUser(), post, likedCallback);
    }

    /**
     * Custom TreeSet which stores objects so they can be efficiently retrieved by Object Id
     */
    static private class SearchableSet<K> implements Set<K> {

        public interface Accessor<K> extends Comparator<K> {
            String accessId(K object);
        }

        // Nodes used within Tree structure
        private class Node {
            K object;
            Node left;
            Node right;
            Node parent;

            Node(K object) {
                this.object = object;
            }
        }

        private Accessor<K> mAccessor;
        private Node mHead;
        private int mSize;

        public SearchableSet(Accessor<K> accessor) {
            this.mAccessor = accessor;
            this.mHead = null;
            this.mSize = 0;
        }

        public K getObjectById(String objectId) {
            Iterator<K> iter = this.iterator();
            while (iter.hasNext()) {
                K object = iter.next();
                String id = mAccessor.accessId(object);
                if (id != null && id.equals(objectId))
                    return object;
            }
            return null;
        }

        @Override
        public int size() {
            return mSize;
        }

        @Override
        public boolean isEmpty() {
            return mSize == 0;
        }

        // Currently unimplemented
        @Override
        public boolean contains(@Nullable Object o) {
            return false;
        }

        // Currently unimplemented
        @NonNull
        @Override
        public Iterator<K> iterator() {
            return new SearchIterator();
        }

        // Currently unimplemented
        @NonNull
        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        // Currently unimplemented
        @NonNull
        @Override
        public <T> T[] toArray(@NonNull T[] ts) {
            return null;
        }

        @Override
        public boolean add(K object) {
            if (mHead == null) {
                mHead = new Node(object);
                mSize++;
                return true;
            }
            Node currNode = mHead;
            do {
                // Do not add duplicates into the set
                if (object.equals(currNode.object)) {
                    return false;
                } else if (mAccessor.compare(object, currNode.object) < 0) {
                    // Left case
                    if (currNode.left == null) {
                        Node newLeft = new Node(object);
                        newLeft.parent = currNode;
                        currNode.left = newLeft;
                        mSize++;
                        return true;
                    }
                    currNode = currNode.left;
                } else {
                    // Right case
                    if (currNode.right == null) {
                        Node newRight = new Node(object);
                        newRight.parent = currNode;
                        currNode.right = newRight;
                        mSize++;
                        return true;
                    }
                    currNode = currNode.right;
                }
            } while (currNode != null);
            // Loop exited only in error cases
            return false;
        }

        // Currently unimplemented
        @Override
        public boolean remove(@Nullable Object o) {
            return false;
        }

        // Currently unimplemented
        @Override
        public boolean containsAll(@NonNull Collection<?> collection) {
            return false;
        }

        @Override
        public boolean addAll(@NonNull Collection<? extends K> collection) {
            boolean changed = false;
            for (K k : collection) {
                changed = changed || add(k);
            }
            return changed;
        }

        // Currently unimplemented
        @Override
        public boolean retainAll(@NonNull Collection<?> collection) {
            return false;
        }

        // Currently unimplemented
        @Override
        public boolean removeAll(@NonNull Collection<?> collection) {
            return false;
        }

        @Override
        public void clear() {
            mHead = null;
            mSize = 0;
        }

        private class SearchIterator implements Iterator<K> {
            private Stack<Node> mStack;

            public SearchIterator() {
                mStack = new Stack<>();
                Node currNode = mHead;

                while (currNode != null) {
                    mStack.push(currNode);
                    currNode = currNode.left;
                }
            }

            @Override
            public boolean hasNext() {
                return !mStack.isEmpty();
            }

            @Override
            public K next() {
                Node currNode = mStack.pop();
                K object = currNode.object;
                if (currNode.right != null) {
                    currNode = currNode.right;
                    while (currNode != null) {
                        mStack.push(currNode);
                        currNode = currNode.left;
                    }
                }
                return object;
            }
        }

    }
}



