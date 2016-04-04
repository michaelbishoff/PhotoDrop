package com.photodrop.photodrop;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.profileToolbar);
        setSupportActionBar(toolbar);

        // Adds the back button to the toolbar since we're adding it dynamically
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Associates the menu_profile with this activity's menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
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
            case R.id.home:
                // TODO: May want to do this instead of this.finish(). this.finish() is only used
                // when it is guaranteed that Activity B started from Activity A. This is not
                // always true is Activity B is opened from a notification.
                // This calls onCreateView() again (in MapsActivity)
//                NavUtils.navigateUpFromSameTask(this);
                this.finish();
                return true;

            // TODO: Change this to a button in the menu bar so it's one less click
            case R.id.action_settings:
                Intent settingsIntent = new Intent(ProfileActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
