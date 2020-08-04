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
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 20;

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
                    mBinding.registrationSubmitButton.setText(R.string.register_button_in_progress);
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
        } else if (password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, R.string.error_credentials_empty, Toast.LENGTH_SHORT).show();
        } else if (password.contains("\\s")) {
            Toast.makeText(this, R.string.error_password_whitespace, Toast.LENGTH_SHORT).show();
        } else if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            String lengthError = String.format(getString(R.string.error_password_length), PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH);
            Toast.makeText(this, lengthError, Toast.LENGTH_SHORT).show();
        } else {
            // If username and password pass all tests, then they are valid
            return true;
        }
        // If any cases fail, then return false
        return false;
    }

    // Attempts to create new Parse user (and log into it)
    private void registerUser(String username, String password) {
        User.Builder userBuilder = new User.Builder();
        userBuilder.setUsername(username);
        userBuilder.setPassword(password);
        userBuilder.createModel().signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error with login", e);
                    Toast.makeText(RegistrationActivity.this, R.string.error_registration, Toast.LENGTH_SHORT).show();
                    mBinding.registrationSubmitButton.setText(R.string.register_button_resting);
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
