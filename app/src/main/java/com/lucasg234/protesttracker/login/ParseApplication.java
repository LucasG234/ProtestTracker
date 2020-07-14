package com.lucasg234.protesttracker.login;

import android.app.Application;

import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.models.Post;
import com.lucasg234.protesttracker.models.User;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseUser;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Application class which configures Parse upon app initialization
 */
public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Register model subclasses
        ParseObject.registerSubclass(Post.class);
        ParseUser.registerSubclass(User.class);

        //TODO
        // Use for troubleshooting -- remove this line for production
        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

        // Use for monitoring Parse OkHttp traffic
        // Can be Level.BASIC, Level.HEADERS, or Level.BODY
        // See http://square.github.io/okhttp/3.x/logging-interceptor/ to see the options.
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.networkInterceptors().add(httpLoggingInterceptor);

        // set applicationId, and server server based on the values in the Heroku settings.
        // clientKey is not needed unless explicitly configured
        // any network interceptors must be added with the Configuration Builder given this syntax
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.app_id)) // should correspond to APP_ID env variable
                .clientKey(getString(R.string.master_key))  // set explicitly unless clientKey is explicitly configured on Parse server
                .clientBuilder(builder)
                .server(getString(R.string.server_url)).build());
    }
}
