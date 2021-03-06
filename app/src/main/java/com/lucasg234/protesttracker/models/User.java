package com.lucasg234.protesttracker.models;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseUser;

/**
 * Parse object which stores represents users, extending default ParseUser
 * All fields are represented by key constants
 */
@ParseClassName("_User")
public class User extends ParseUser {
    // Keys for all custom fields
    public static final String KEY_PROFILE_PICTURE = "profilePicture";
    public static final String KEY_FACEBOOK_PICTURE_URL = "facebookProfilePictureUrl";

    public User(String username, String password, ParseFile profilePicture, String facebookPictureUrl) {
        super();
        setUsername(username);
        setPassword(password);
        if (profilePicture != null) {
            setProfilePicture(profilePicture);
        }
        if (facebookPictureUrl != null) {
            setFacebookPictureUrl(facebookPictureUrl);
        }
    }

    // Required empty constructor
    public User() {

    }

    public ParseFile getProfilePicture() {
        return getParseFile(KEY_PROFILE_PICTURE);
    }

    public String getFacebookPictureUrl() {
        return getString(KEY_FACEBOOK_PICTURE_URL);
    }

    public void setProfilePicture(ParseFile profilePicture) {
        put(KEY_PROFILE_PICTURE, profilePicture);
    }

    public void setFacebookPictureUrl(String url) {
        put(KEY_FACEBOOK_PICTURE_URL, url);
    }

    public static class Builder {
        private String mUsername;
        private String mPassword;
        private ParseFile mProfilePicture;
        private String mFacebookPictureUrl;

        public void setUsername(String username) {
            this.mUsername = username;
        }

        public void setPassword(String password) {
            this.mPassword = password;
        }

        public void setProfilePicture(ParseFile profilePicture) {
            this.mProfilePicture = profilePicture;
        }

        public void setFacebookPictureUrl(String url) {
            this.mFacebookPictureUrl = url;
        }

        public User createModel() {
            return new User(mUsername, mPassword, mProfilePicture, mFacebookPictureUrl);
        }
    }
}
