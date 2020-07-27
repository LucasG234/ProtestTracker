package com.lucasg234.protesttracker.models;

import androidx.annotation.Nullable;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;

import java.util.Objects;

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
    public static final String KEY_FUNCTION_USER_LIKES = "getUserLikes";

    // Additional constants
    public static final int QUERY_LIMIT = 20;
    public static final int MAXIMUM_LENGTH = 300;

    public Post(User author, String text, ParseFile image, ParseGeoPoint location) {
        super();
        setAuthor(author);
        setText(text);
        setLocation(location);
        if (image != null) {
            setImage(image);
        }
    }

    // Required empty constructor
    public Post() {

    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Post && this.getObjectId().equals(((Post) obj).getObjectId());
    }

    // Returns 0 if equal. Otherwise, sorts by the time the posts were createdAt
    // Posts created at the same time sort by ObjectId
    @Override
    public int compareTo(Post post) {
        if (this.equals(post)) {
            return 0;
        } else if (this.getCreatedAt().compareTo(post.getCreatedAt()) == 0) {
            // Do not return 0 if only the createdAt times are equal
            // This would ruin consistency with equals
            return this.getObjectId().compareTo(post.getObjectId());
        } else {
            return this.getCreatedAt().compareTo(post.getCreatedAt());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getObjectId());
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

    // Return the entire relations to allow custom queries
    public ParseRelation<User> getLikedBy() {
        ParseQuery query = getRelation("").getQuery();
        return getRelation(KEY_LIKED_BY);
    }

    public ParseRelation<User> getIgnoredBy() {
        return getRelation(KEY_IGNORED_BY);
    }

    public static class Builder {
        private User mAuthor;
        private String mText;
        private ParseFile mImage;
        private ParseGeoPoint mLocation;

        public Builder setAuthor(User author) {
            mAuthor = author;
            return this;
        }

        public Builder setText(String text) {
            mText = text;
            return this;
        }

        public Builder setImage(ParseFile image) {
            this.mImage = image;
            return this;
        }

        public Builder setLocation(ParseGeoPoint location) {
            this.mLocation = location;
            return this;
        }

        public Post createModel() {
            return new Post(mAuthor, mText, mImage, mLocation);
        }
    }
}
