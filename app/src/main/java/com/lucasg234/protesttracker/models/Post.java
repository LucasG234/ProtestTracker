package com.lucasg234.protesttracker.models;

import androidx.annotation.Nullable;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseRelation;

/**
 * Parse object which stores user posts
 * All fields are represented by key constants
 */
@ParseClassName("Post")
public class Post extends ParseObject implements Comparable<Post> {
    // Keys for all Parse fields
    public static final String KEY_OBJECT_ID = "objectId";
    public static final String KEY_CREATED_AT = "createdAt";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_TEXT = "text";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_LIKED_BY = "likedBy";
    public static final String KEY_IGNORED_BY = "ignoredBy";

    // Additional constants
    public static final int QUERY_LIMIT = 20;
    public static final int MAXIMUM_LENGTH = 300;

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Post && this.getObjectId().equals(((Post) obj).getObjectId());
    }

    @Override
    public int compareTo(Post post) {
        return this.getObjectId().compareTo(post.getObjectId());
    }

    public User getAuthor() {
        return (User) getParseUser(KEY_AUTHOR);
    }

    public void setAuthor(User author) {
        put(KEY_AUTHOR, author);
    }

    public String getText() {
        return getString(KEY_TEXT);
    }

    public void setText(String text) {
        put(KEY_TEXT, text);
    }

    public ParseFile getImage() {
        return getParseFile(KEY_IMAGE);
    }

    public void setImage(ParseFile image) {
        put(KEY_IMAGE, image);
    }

    public ParseGeoPoint getLocation() {
        return getParseGeoPoint(KEY_LOCATION);
    }

    public void setLocation(ParseGeoPoint location) {
        put(KEY_LOCATION, location);
    }

    // Relations do not have separate setters because this operation is done on the returned object
    public ParseRelation<User> getLikedBy() {
        return getRelation(KEY_LIKED_BY);
    }

    public ParseRelation<User> getIgnoredBy() {
        return getRelation(KEY_IGNORED_BY);
    }

}
