package com.lucasg234.protesttracker.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lucasg234.protesttracker.R;
import com.lucasg234.protesttracker.databinding.ActivityRegistrationBinding;
import com.lucasg234.protesttracker.mainactivity.MainActivity;
import com.lucasg234.protesttracker.models.User;
import com.parse.ParseException;
import com.parse.SignUpCallback;

/**
 * Activity which allows users to log to create new accounts
 * Accessed from LoginActivity, with a button to return to it
 */
public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";

    private ActivityRegistrationBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.registrationSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Register button clicked");
                String username = mBinding.registrationUsernameText.getText().toString();
                String password = mBinding.registrationPasswordText.getText().toString();
                String confirmPassword = mBinding.registrationPasswordConfirmText.getText().toString();
                if (validateRegistration(username, password, confirmPassword)) {
                    registerUser(username, password);
                }
            }
        });

        mBinding.registrationToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Register to login button clicked");
                navigateToActivity(LoginActivity.class);
            }
        });
    }

    // Ensure username and password meet all rules
    private boolean validateRegistration(String username, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.error_password_match, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    // Attempts to create new Parse user (and log into it)
    private void registerUser(String username, String password) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error with login", e);
                    //TODO: possibly improve to be more specific
                    Toast.makeText(RegistrationActivity.this, R.string.error_registration, Toast.LENGTH_SHORT).show();
                    return;
                }
                navigateToActivity(MainActivity.class);
                Toast.makeText(RegistrationActivity.this, R.string.account_created, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToActivity(Class activityClass) {
        Intent intent = new Intent(RegistrationActivity.this, activityClass);
        startActivity(intent);
        finish();
    }
}