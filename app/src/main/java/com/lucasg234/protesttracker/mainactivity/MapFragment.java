package com.lucasg234.protesttracker.mainactivity;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.FragmentMapBinding;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.permissions.PermissionsHandler;
import com.lucasg234.protesttracker.util.LocationUtils;

/**
 * Fragment containing a Google MapView
 * Users can see recent posts geographically
 */
public class MapFragment extends Fragment {

    // Default zoom level on the map
    private static final int DEFAULT_ZOOM_LEVEL = 17;
    // Minimum distance change between locations. Set to 10 meters
    // If user does not move this distance, no updates will be created.
    public static final float MINIMUM_DISPLACEMENT_METERS = 10;
    // Maximum time to wait for a LocationUpdate. Set to 60 seconds
    private static final int UPDATE_INTERVAL_MS = 60000;
    // Minimum time to wait for a locationUpdate. Set to 5 seconds
    private static final int FASTEST_INTERVAL_MS = 5000;

    private static final String TAG = "MapFragment";
    private MapListener mMapListener;
    private MainActivity mParent;
    // Represents whether the camera is currently follow the user's current position
    private boolean mFollowUser;

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of MapFragment
     */
    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return FragmentMapBinding.inflate(inflater, container, false).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mParent = (MainActivity) getActivity();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    loadMap(googleMap);
                }
            });
        } else {
            Log.e(TAG, "mapFragment was null on onViewCreated");
            Toast.makeText(mParent, R.string.error_map_load, Toast.LENGTH_SHORT).show();
        }
    }

    // Configures the GoogleMap
    private void loadMap(GoogleMap map) {
        if (!PermissionsHandler.checkLocationPermission(mParent)) {
            PermissionsHandler.requestLocationPermission(this);
            return;
        }
        // Map follows current user's location
        map.setMyLocationEnabled(true);
        // Adds zoom in and out buttons
        map.getUiSettings().setZoomControlsEnabled(true);
        // Adds a button that zooms the camera to the user
        map.getUiSettings().setMyLocationButtonEnabled(true);

        // Map follows user location until disabled with gestures
        setFollowUser(true);

        // Adds our listener to add markers
        mMapListener = new MapListener(this, map);
        map.setOnCameraMoveListener(mMapListener);
        map.setOnInfoWindowClickListener(mMapListener);
        map.setOnCameraMoveStartedListener(mMapListener);
        map.setOnMyLocationButtonClickListener(mMapListener);

        LatLng currentLocation = LocationUtils.toLatLng(LocationUtils.getCurrentLocation(mParent));
        if (currentLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM_LEVEL));
        }

        mMapListener.queryPostsInBounds(map.getProjection().getVisibleRegion().latLngBounds);

        subscribeToLocationRequests(map);
    }

    // Enables automatic location requests as the user moves
    private void subscribeToLocationRequests(final GoogleMap map) {
        // Create a LocationRequest
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL_MS);
        locationRequest.setFastestInterval(FASTEST_INTERVAL_MS);
        locationRequest.setSmallestDisplacement(MINIMUM_DISPLACEMENT_METERS);

        if (!PermissionsHandler.checkLocationPermission(mParent)) {
            PermissionsHandler.requestLocationPermission(this);
            return;
        }
        // Call onLocationChange method when new Location is found
        LocationServices.getFusedLocationProviderClient(mParent)
                .requestLocationUpdates(locationRequest, new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                onLocationChange(locationResult.getLastLocation(), map);
                            }
                        },
                        Looper.myLooper());
    }

    // Method called every FASTEST_INTERVAL_MS milliseconds as long as user has moved at least MINIMUM_DISPLACEMENT
    // Enables the camera to track the user, calling the onCameraMove() method on each change
    private void onLocationChange(Location lastLocation, GoogleMap map) {
        Log.i(TAG, "Location changed to: " + lastLocation.toString());
        if (mFollowUser) {
            map.animateCamera(CameraUpdateFactory.newLatLng(LocationUtils.toLatLng(lastLocation)));
        }
    }

    public void setFollowUser(boolean followUser) {
        this.mFollowUser = followUser;
    }

    public void ignorePost(Post post) {
        mMapListener.removeMarker(post);
    }

    public void changePostLiked(Post post) {
        mMapListener.changeMarkerLikeStatus(post);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // If permission was just granted to allow location services, then restart view loading
        if (requestCode == PermissionsHandler.REQUEST_CODE_LOCATION_PERMISSIONS && permissions.length >= 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            getFragmentManager()
                    .beginTransaction()
                    .detach(this)
                    .attach(this)
                    .commit();
        }
    }
}
