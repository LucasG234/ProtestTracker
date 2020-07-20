package com.lucasg234.protesttracker.detailactivity;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.lucasg234.protesttracker.models.Post;

public class FeedPostDetailListener implements PostDetailActivity.PostDetailInteractionListener {

    private static final String TAG = "FeedPostDetailListener";

    @Override
    public void onIgnoreClicked(Post post) {
        Log.i(TAG, "Feed listener found detail ignore click");
    }

    @Override
    public void onRecommendClicked(Post post) {
        Log.i(TAG, "Feed listener found detail recommend click");
    }

    // Normal constructor
    public FeedPostDetailListener() {

    }

    // Constructor used by Parcelable CREATOR
    private FeedPostDetailListener(Parcel in) {

    }

    // Required method for Parcelable
    @Override
    public void writeToParcel(Parcel out, int flags) {

    }

    // Required method for Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    // Parcelable.Creator<MyParcelable> CREATOR constant for our class required for Parcelable
    public static final Parcelable.Creator<FeedPostDetailListener> CREATOR
            = new Parcelable.Creator<FeedPostDetailListener>() {

        // Creates an object from a serialized Parcel
        @Override
        public FeedPostDetailListener createFromParcel(Parcel in) {
            return new FeedPostDetailListener(in);
        }

        // Creates an array which holds objects of the FeedPostDetailListener class
        @Override
        public FeedPostDetailListener[] newArray(int size) {
            return new FeedPostDetailListener[size];
        }
    };
}
