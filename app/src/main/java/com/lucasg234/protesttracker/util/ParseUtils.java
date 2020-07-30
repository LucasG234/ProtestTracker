package com.lucasg234.protesttracker.util;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseRelation;

import java.util.HashMap;

public class ParseUtils {

    private static final String TAG = "ParseUtils";

    // This helper method adds the given User to the ignoredBy of the given post
    public static void addIgnoredBy(User user, Post post) {
        ParseRelation<User> ignoredBy = post.getIgnoredBy();
        ignoredBy.add(user);
        post.saveInBackground();
    }

    // This helper method determines if a User has liked this post object
    // Uses the callback after querying in the background
    public static void getUserLikes(User user, Post post, FunctionCallback<Boolean> callback) {
        HashMap<String, String> params = new HashMap<>();
        params.put(Post.class.getSimpleName(), post.getObjectId());
        params.put(User.class.getSimpleName(), user.getObjectId());
        ParseCloud.callFunctionInBackground(Post.KEY_FUNCTION_USER_LIKES, params, callback);
    }

    // This helper method loads the current profile picture into the specified view
    public static void loadProfilePicture(User user, ImageView view, boolean circleCrop) {
        if (user.getProfilePicture() != null) {
            RequestBuilder<Drawable> imageLoad = Glide.with(view.getContext()).load(user.getProfilePicture().getUrl());
            if (circleCrop) {
                imageLoad = imageLoad.circleCrop();
            }
            imageLoad.into(view);
        } else if (user.getFacebookPictureUrl() != null) {
            RequestBuilder<Drawable> imageLoad = Glide.with(view.getContext()).load(user.getFacebookPictureUrl());
            if (circleCrop) {
                imageLoad = imageLoad.circleCrop();
            }
            imageLoad.into(view);
        } else {
            view.setImageResource(R.drawable.default_user);
        }
    }
}

