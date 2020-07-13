package com.lucasg234.protesttracker.models;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;

@ParseClassName("Post")
public class Post extends ParseObject {
    // Keys for all Parse fields
    public static final String KEY_OBJECT_ID = "objectId";
    public static final String KEY_CREATED_AT = "createdAt";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_TEXT = "text";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_LIKED_BY = "likedBy";
    public static final String KEY_IGNORED_BY = "ignoredBy";

    public User getAuthor() {
        return (User)getParseUser(KEY_AUTHOR);
    }

    public String getText() {
        return getString(KEY_TEXT);
    }

    public ParseFile getImage() {
        return getParseFile(KEY_IMAGE);
    }

    public ParseGeoPoint getLocation() {
        return getParseGeoPoint(KEY_LOCATION);
    }

    public void setAuthor(User author) {
        put(KEY_AUTHOR, author);
    }

    public void setText(String text) {
        put(KEY_TEXT, text);
    }

    public void setImage(ParseFile image) {
        put(KEY_IMAGE, image);
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
