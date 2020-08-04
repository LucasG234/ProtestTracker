package com.lucasg234.protesttracker.mainactivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.ActivityMainBinding;
import com.lucasg234.protesttracker.detailactivity.PostDetailActivity;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.lucasg234.protesttracker.permissions.LocationPermissions;
import com.lucasg234.protesttracker.permissions.NoPermissionsFragment;
import com.lucasg234.protesttracker.util.ParseUtils;
import com.parse.FunctionCallback;
import com.parse.ParseException;
import com.parse.ParseRelation;
import com.parse.SaveCallback;

/**
 * Central activity which holds the FeedFragment, MapFragment, ComposeFragment, and SettingsFragment
 * Handles navigation between the Fragments
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    private ActivityMainBinding mBinding;
    private Fragment mCurrentFragment;
    private boolean mNavigationEnabled;
    private int numProcesses;

    private FeedFragment mFeed;
    private ComposeFragment mCompose;
    private MapFragment mMap;
    private SettingsFragment mSettings;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        numProcesses = 0;

        if (!LocationPermissions.checkLocationPermission(this)) {
            Log.i(TAG, "Found no location permissions");
            mNavigationEnabled = false;
            disableFragmentNavigation();
        } else {
            mNavigationEnabled = true;
            enableFragmentNavigation();
        }
    }

    // Method called when Activity is reopened from another program
    // Respond here in case user has changed their permission settings from another location
    @Override
    protected void onResume() {
        super.onResume();

        boolean locationPermissions = LocationPermissions.checkLocationPermission(this);

        if (mNavigationEnabled && !locationPermissions) {
            disableFragmentNavigation();
        } else if (!mNavigationEnabled && locationPermissions) {
            enableFragmentNavigation();
        }
    }

    // Enables the bottom navigation and all fragments connected to it
    public void enableFragmentNavigation() {
        // All fragments constructed only once when BottomNavigation enabled
        if (mFeed == null || mCompose == null || mMap == null || mSettings == null) {
            mFeed = FeedFragment.newInstance();
            mCompose = ComposeFragment.newInstance();
            mMap = MapFragment.newInstance();
            mSettings = SettingsFragment.newInstance();
        }

        final FragmentManager fragmentManager = getSupportFragmentManager();

        mBinding.mainBottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment newCurrentFragment;
                switch (item.getItemId()) {
                    case R.id.bottomNavigationFeed:
                        newCurrentFragment = mFeed;
                        break;
                    case R.id.bottomNavigationCompose:
                        newCurrentFragment = mCompose;
                        break;
                    case R.id.bottomNavigationMap:
                        newCurrentFragment = mMap;
                        break;
                    case R.id.bottomNavigationSettings:
                        newCurrentFragment = mSettings;
                        break;
                    default:
                        Log.i(TAG, "Bottom Navigation View selected unknown icon : " + item.toString());
                        return false;
                }

                fragmentManager.beginTransaction().hide(mCurrentFragment).show(newCurrentFragment).commit();
                mCurrentFragment = newCurrentFragment;
                return true;
            }
        });

        // If navigation was previously disabled, removed the NoPermissionsFragment
        if (!mNavigationEnabled) {
            fragmentManager.beginTransaction().remove(mCurrentFragment).commit();
        }

        // Add all initial fragments
        fragmentManager.beginTransaction().add(R.id.fragmentHolder, mCompose, ComposeFragment.class.getSimpleName()).hide(mCompose).commit();
        fragmentManager.beginTransaction().add(R.id.fragmentHolder, mFeed, FeedFragment.class.getSimpleName()).hide(mFeed).commit();
        fragmentManager.beginTransaction().add(R.id.fragmentHolder, mSettings, SettingsFragment.class.getSimpleName()).hide(mSettings).commit();
        // Map fragment not hidden because it is the default
        fragmentManager.beginTransaction().add(R.id.fragmentHolder, mMap, MapFragment.class.getSimpleName()).commit();

        mCurrentFragment = mMap;
        mNavigationEnabled = true;
    }

    // Disable the BottomNavigationView and change the fragment to NoPermissionsFragment
    public void disableFragmentNavigation() {
        // If navigation was previously disabled, hide the current fragment
        if (mNavigationEnabled) {
            getSupportFragmentManager().beginTransaction().hide(mCurrentFragment).commit();
        }
        // Create a NoPermissionsFragment as the current fragment
        NoPermissionsFragment noPermissionsFragment = NoPermissionsFragment.newInstance();
        getSupportFragmentManager().beginTransaction().add(R.id.fragmentHolder, noPermissionsFragment, NoPermissionsFragment.class.getSimpleName()).commit();
        mCurrentFragment = noPermissionsFragment;

        // Disable the onClickListener for the BottomNavigationView
        mBinding.mainBottomNavigation.setOnClickListener(null);

        mNavigationEnabled = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // No need to check resultCode because both RESULT_OK and RESULT_CANCELED are accepted

        switch (requestCode) {
            case PostDetailActivity.REQUEST_CODE_POST_DETAIL:
                Post post = data.getParcelableExtra(PostDetailActivity.KEY_RESULT_POST);
                if (data.getBooleanExtra(PostDetailActivity.KEY_RESULT_LIKED_CHANGED, false)) {
                    saveLikeChange(post);
                }
                if (data.getBooleanExtra(PostDetailActivity.KEY_RESULT_IGNORED, false)) {
                    saveIgnore(post);
                }
                break;
            default:
                Log.e(TAG, "Received onActivityResult with unused request code: " + requestCode);
                return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (LocationPermissions.checkLocationPermission(this)) {
            enableFragmentNavigation();
        }
    }

    public void saveLikeChange(final Post post) {
        final ParseRelation<User> likedBy = post.getLikedBy();
        FunctionCallback<Boolean> likedCallback = new FunctionCallback<Boolean>() {
            @Override
            public void done(final Boolean liked, ParseException e) {
                subtractProcess();

                if (e != null) {
                    Log.e(TAG, "Error in liking post", e);
                    Toast.makeText(MainActivity.this, R.string.error_liking, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (liked) {
                    likedBy.remove((User) User.getCurrentUser());
                } else {
                    likedBy.add((User) User.getCurrentUser());
                }

                mFeed.changePostLiked(post);
                mMap.changePostLiked(post);

                post.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Log.e(TAG, "Error saving change to like status", e);
                            Toast.makeText(MainActivity.this, R.string.error_liking, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };

        addProcess();
        ParseUtils.getUserLikes((User) User.getCurrentUser(), post, likedCallback);
    }

    public void saveIgnore(final Post post) {
        ParseUtils.addIgnoredBy((User) User.getCurrentUser(), post);
        addProcess();
        post.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                subtractProcess();

                if (e != null) {
                    Log.e(TAG, "Error in ignoring post", e);
                    Toast.makeText(MainActivity.this, R.string.error_ignoring, Toast.LENGTH_SHORT).show();
                    return;
                }

                mFeed.ignorePost(post);
                mMap.ignorePost(post);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_progress_bar, menu);
        return true;
    }

    public void addProcess() {
        numProcesses++;
    }

    public void subtractProcess() {
        numProcesses--;
    }
}