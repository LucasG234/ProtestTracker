package com.lucasg234.protesttracker.mainactivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

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

/**
 * Central activity which holds the FeedFragment, MapFragment, ComposeFragment, and SettingsFragment
 * Handles navigation between the Fragments
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    private ActivityMainBinding mBinding;
    private Fragment mCurrentFragment;

    private final FeedFragment mFeed;
    private final ComposeFragment mCompose;
    private final MapFragment mMap;
    private final SettingsFragment mSettings;

    public MainActivity() {
        // Fragments constructed only once
        mFeed = FeedFragment.newInstance();
        mCompose = ComposeFragment.newInstance();
        mMap = MapFragment.newInstance();
        mSettings = SettingsFragment.newInstance();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

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

        // Add all initial fragments
        fragmentManager.beginTransaction().add(R.id.fragmentHolder, mCompose, ComposeFragment.class.getSimpleName()).hide(mCompose).commit();
        fragmentManager.beginTransaction().add(R.id.fragmentHolder, mFeed, FeedFragment.class.getSimpleName()).hide(mFeed).commit();
        fragmentManager.beginTransaction().add(R.id.fragmentHolder, mSettings, SettingsFragment.class.getSimpleName()).hide(mSettings).commit();
        // Map fragment not hidden because it is the default
        fragmentManager.beginTransaction().add(R.id.fragmentHolder, mMap, MapFragment.class.getSimpleName()).commit();

        mCurrentFragment = mFeed;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // No need to check resultCode because both RESULT_OK and RESULT_CANCELED are accepted

        switch (requestCode) {
            case PostDetailActivity.REQUEST_CODE_POST_DETAIL:
                Post post = data.getParcelableExtra(PostDetailActivity.KEY_RESULT_POST);
                if (data.getBooleanExtra(PostDetailActivity.KEY_RESULT_LIKED, false)) {
                    mFeed.changePostLiked(post);
                    mMap.changePostLiked(post);
                }
                if (data.getBooleanExtra(PostDetailActivity.KEY_RESULT_IGNORED, false)) {
                    mFeed.ignorePost(post);
                    mMap.ignorePost(post);
                }
                break;
            default:
                Log.e(TAG, "Received onActivityResult with unused request code: " + requestCode);
                return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}