package com.lucasg234.protesttracker.util;

import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseRelation;

import java.util.HashMap;

public class PostUtils {

    private static final String TAG = "PostUtils";

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
}
