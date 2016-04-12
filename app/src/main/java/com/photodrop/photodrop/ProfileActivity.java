package com.photodrop.photodrop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import android.widget.GridView;

import java.net.URL;
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {
    private GridView imageGrid;
    private ArrayList<Bitmap> bitmapList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.profileToolbar);
        setSupportActionBar(toolbar);

        // Adds the back button to the toolbar since we're adding it dynamically
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        this.imageGrid = (GridView) findViewById(R.id.profileActivityGridview);
        this.bitmapList = new ArrayList<Bitmap>();

        try {
            System.out.print(">_<!\n\n");
            for(int i = 0; i < 10; i++) {
                this.bitmapList.add(urlImageToBitmap("http://placehold.it/150x150"));
                System.out.print(">_<!!\n\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.imageGrid.setAdapter(new ImageAdapter(this, this.bitmapList));

    }
    private Bitmap urlImageToBitmap(String imageUrl) throws Exception {
        Bitmap result = null;
        URL url = new URL(imageUrl);
        if(url != null) {
            result = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            //result = BitmapFactory.decodeStream();
        }
        return result;
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
