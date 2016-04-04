package com.photodrop.photodrop;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    Button changePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        changePassword = (Button) findViewById(R.id.changePasswordButton);
    }

    @Override
    protected void onResume() {
        super.onResume();

        changePassword.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        changePassword.setOnClickListener(null);
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
        }
    }
}
