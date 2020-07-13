package com.lucasg234.protesttracker.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;

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
}
