package com.lucasg234.protesttracker.mainactivity;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.lucasg234.protesttracker.util.LocationUtils;

/**
 * Fragment containing a Google MapView
 * Users can see recent posts geographically
 */
public class MapFragment extends Fragment {

    private static final int DEFAULT_ZOOM_LEVEL = 17;
    private static final long UPDATE_INTERVAL = 60000;  /* 60 secs */
    private static final long FASTEST_INTERVAL = 5000; /* 5 secs */

    private static final String TAG = "MapFragment";

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

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    loadMap(googleMap);
                }
            });
        }
    }

    private void loadMap(GoogleMap map) {
        // Configure initial settings
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        LatLng currentLocation = LocationUtils.toLatLng(LocationUtils.getCurrentLocation(getContext()));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM_LEVEL));

        subscribeToLocationRequests();
    }

    private void subscribeToLocationRequests() {
        // Create a LocationRequest
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Call onLocationChange method when new Location is found
        LocationServices.getFusedLocationProviderClient(getContext())
                .requestLocationUpdates(locationRequest, new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                onLocationChange(locationResult.getLastLocation());
                            }
                        },
                        Looper.myLooper());
    }

    private void onLocationChange(Location lastLocation) {
        // This is where post markers will be changed
        Log.i(TAG, "Location changed to: " + lastLocation.toString());
    }
}