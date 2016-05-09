package com.photodrop.photodrop;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.firebase.client.Firebase;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private Button changePasswordButton, logoutButton;
    private UserAuth userAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Firebase Context for logout
        Firebase.setAndroidContext(this);
        userAuth = new UserAuth();

        changePasswordButton = (Button) findViewById(R.id.changePasswordButton);
        logoutButton = (Button) findViewById(R.id.logoutButton);

        Typeface fontThin = Typeface.createFromAsset(this.getAssets(), "fonts/ValterStd-Thin.ttf");
        changePasswordButton.setTypeface(fontThin);
        logoutButton.setTypeface(fontThin);
    }

    @Override
    protected void onResume() {
        super.onResume();

        changePasswordButton.setOnClickListener(this);
        logoutButton.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        changePasswordButton.setOnClickListener(null);
        logoutButton.setOnClickListener(null);
    }

    /**
     * Handles the onClick event when the back button is selected. Returns to the MapsActivity.
     * @return true - we handled the item selected event
     *         false (default) - will call the item's Runnable or send a message to
     *         its Handler as appropriate
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.changePasswordButton:
                Intent changePasswordIntent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
                startActivity(changePasswordIntent);
                break;

            case R.id.logoutButton:
                // TODO: Delete this because I'm testing it right now
                userAuth.isLoggedIn();

                // Logs the user out
                userAuth.logOut();

                // Changes to the logout screen and removes the back stack
                Intent logoutIntent = new Intent(this, LoginActivity.class);
                logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(logoutIntent);

                break;
        }
    }
}
