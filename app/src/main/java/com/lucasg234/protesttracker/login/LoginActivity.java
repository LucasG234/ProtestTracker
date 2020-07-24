package com.lucasg234.protesttracker.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.ActivityLoginBinding;
import com.lucasg234.protesttracker.mainactivity.MainActivity;
import com.lucasg234.protesttracker.models.User;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.facebook.ParseFacebookUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Launcher activity which allows users to log into existing accounts
 * Contains button to access RegistrationActivity
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding mBinding;
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip this activity if there is already an active user
        if (User.getCurrentUser() != null) {
            navigateToActivity(MainActivity.class);
        }

        mBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.loginSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Login button clicked");
                String username = mBinding.loginUsernameText.getText().toString();
                String password = mBinding.loginPasswordText.getText().toString();
                loginUser(username, password);
            }
        });

        mBinding.loginToRegistrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Login to registration button clicked");
                navigateToActivity(RegistrationActivity.class);
            }
        });

        configureFacebookLogin();
    }

    private void configureFacebookLogin() {
        mCallbackManager = CallbackManager.Factory.create();
        mBinding.loginFacebookButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                Log.i(TAG, "Success");
                // Currently not requesting any permissions from Facebook
                ArrayList<String> permissions = new ArrayList<>();

                ParseFacebookUtils.logInWithReadPermissionsInBackground(LoginActivity.this, permissions, new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e) {
                        if (e != null) {
                            Log.e(TAG, "Error in ParseFacebookUtils.logIn", e);
                            Toast.makeText(LoginActivity.this, R.string.error_login_facebook, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (user.isNew()) {
                            Log.i(TAG, "New lol");
                            setFacebookUsername(loginResult.getAccessToken(), (User) user);
                        }
                        navigateToActivity(MainActivity.class);
                    }
                });
            }

            @Override
            public void onCancel() {
                Log.i(TAG, "User canceled Facebook login");
                Toast.makeText(LoginActivity.this, R.string.error_receive_facebook, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, "Error on Facebook login");
                Toast.makeText(LoginActivity.this, R.string.error_login_facebook, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Attempts to login to a Parse account with the given credentials
    private void loginUser(String username, String password) {
        User.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error with login", e);
                    //TODO: possibly improve to be more specific
                    Toast.makeText(LoginActivity.this, R.string.error_login, Toast.LENGTH_SHORT).show();
                    return;
                }
                navigateToActivity(MainActivity.class);
                Toast.makeText(LoginActivity.this, R.string.login_completed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setFacebookUsername(AccessToken accessToken, final User user) {
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        if (response.getError() != null) {
                            Log.e(TAG, "Error with GraphRequest for username", response.getError().getException());
                        }
                        try {
                            String name = object.getString("name");
                            user.setUsername(name);
                            user.saveInBackground();
                        } catch (JSONException e) {
                            Log.i(TAG, "Error setting username from GraphRequest JSON", e);
                            e.printStackTrace();
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "name");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void navigateToActivity(Class activityClass) {
        Intent intent = new Intent(LoginActivity.this, activityClass);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}