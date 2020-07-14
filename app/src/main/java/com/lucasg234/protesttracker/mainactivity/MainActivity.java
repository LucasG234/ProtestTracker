package com.lucasg234.protesttracker.mainactivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.ActivityMainBinding;

/**
 * Central activity which holds the FeedFragment, MapFragment, ComposeFragment, and SettingsFragment
 * Handles navigation between the Fragments
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        final FragmentManager fragmentManager = getSupportFragmentManager();

        mBinding.mainBottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment;
                switch (item.getItemId()) {
                    case R.id.bottomNavigationFeed:
                        fragment = FeedFragment.newInstance();
                        break;
                    case R.id.bottomNavigationCompose:
                        fragment = ComposeFragment.newInstance();
                        break;
                    case R.id.bottomNavigationMap:
                        fragment = MapFragment.newInstance("", "");
                        break;
                    case R.id.bottomNavigationSettings:
                        fragment = SettingsFragment.newInstance();
                        break;
                    default:
                        Log.i(TAG, "Bottom Navigation View selected unknown icon : " + item.toString());
                        return false;
                }
                fragmentManager.beginTransaction().replace(R.id.fragmentHolder, fragment).commit();
                return true;
            }
        });

        // Set default selection
        mBinding.mainBottomNavigation.setSelectedItemId(R.id.bottomNavigationFeed);
    }
}