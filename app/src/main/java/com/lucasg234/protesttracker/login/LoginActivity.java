package com.lucasg234.protesttracker.login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.lucasg234.protesttracker.mainactivity.MainActivity;
import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.ActivityLoginBinding;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip this activity if there is already an active user
        if(ParseUser.getCurrentUser() != null) {
            navigateToMainActivity();
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

        mBinding.loginRegistrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Register button clicked");
                String username = mBinding.loginUsernameText.getText().toString();
                String password = mBinding.loginPasswordText.getText().toString();
                registerUser(username, password);
            }
        });
    }

    private void loginUser(String username, String password) {
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if(e != null) {
                    Log.e(TAG, "Error with login", e);
                    //TODO: possibly improve to be more specific
                    Toast.makeText(LoginActivity.this, getString(R.string.error_login), Toast.LENGTH_SHORT).show();
                    return;
                }
                navigateToMainActivity();
                Toast.makeText(LoginActivity.this, getString(R.string.login_completed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(String username, String password) {
        ParseUser newUser = new ParseUser();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if(e != null) {
                    Log.e(TAG, "Error with login", e);
                    //TODO: possibly improve to be more specific
                    Toast.makeText(LoginActivity.this, getString(R.string.error_registration), Toast.LENGTH_SHORT).show();
                    return;
                }
                navigateToMainActivity();
                Toast.makeText(LoginActivity.this, getString(R.string.account_created), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMainActivity() {
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }
}