package com.photodrop.photodrop;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class ImageActivity extends AppCompatActivity implements ValueEventListener {

    // UI Elements
    private ImageView imageView;

    // Firebase Objects
    private Firebase images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        // Gets the UI elements
        imageView = (ImageView) findViewById(R.id.imageView);

        // Initializes the Firebase reference
        images = new Firebase(MainActivity.IMAGES_URL);

        // Sets the image with the corresponding image key that was passed to this activity
        setImage(getIntent().getStringExtra(MapsActivity.IMAGE_KEY));
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

    /**
     * Sets the image view with the image with the corresponding key in Firebase
     */
    public void setImage(String key) {
        Log.d("ME", "Adding listener to: " + images.child(key).getPath().toString());

        // TODO: Should probably save the image locally and check if the image is saved before accessing the DB again

        // Adds a listener then removes it once it's triggered so that the image view is set
        images.child(key).addListenerForSingleValueEvent(this);
    }

    /*
     * Firebase ValueEventListener callbacks used to set the image view.
     * The listener is set in setImage(String key)
     */

    /**
     * Sets the image view with the encoded image from Firebase
     */
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        Log.d("ME", "Getting image from: " + dataSnapshot.getRef().getPath());

        // Sets the image
        imageView.setImageBitmap(ImageUtil.getBitmapFromEncodedImage((String) dataSnapshot.getValue()));
    }

    @Override
    public void onCancelled(FirebaseError firebaseError) { }
}
