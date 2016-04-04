package com.photodrop.photodrop;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

// TODO: Use this to make transparent action bar: http://stackoverflow.com/questions/26505632/how-to-make-toolbar-transparent
// http://stackoverflow.com/questions/25723331/display-and-hide-navigationbar-and-actionbar-onclickandroid
public class ImageActivity extends AppCompatActivity implements ValueEventListener, View.OnClickListener {

    // UI Elements
    private ImageView imageView;
    private Button like, comment, flag;

    // Firebase Objects
    private Firebase images;
    private String imageKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        // Gets the Image View
        imageView = (ImageView) findViewById(R.id.imageView);

        // Initializes the Firebase reference
        images = new Firebase(MainActivity.IMAGES_URL);

        // Sets the image with the corresponding image key that was passed to this activity
        imageKey = getIntent().getStringExtra(MapsActivity.IMAGE_KEY);
        setImage(imageKey);

        // Gets the UI elements
        like = (Button) findViewById(R.id.likeButton);
        comment = (Button) findViewById(R.id.commentButton);
        flag = (Button) findViewById(R.id.flagButton);
    }

    @Override
    protected void onResume() {
        super.onResume();

        like.setOnClickListener(this);
        comment.setOnClickListener(this);
        flag.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        like.setOnClickListener(null);
        comment.setOnClickListener(null);
        flag.setOnClickListener(null);

        // TODO: Could write the bitmap to a file so we can bring it back up later without connecting to Firebase and save on memory

        // This doesn't seem to be freeing up memeory
        imageView.setImageBitmap(null);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.likeButton:
                // TODO: Make this go to Firebase
                Toast.makeText(ImageActivity.this, "Like", Toast.LENGTH_SHORT).show();
                break;

            case R.id.commentButton:
                Intent commentIntent = new Intent(ImageActivity.this, CommentActivity.class);
                // Passes this image's key to the CommentActivity
                commentIntent.putExtra(MapsActivity.IMAGE_KEY, imageKey);
                startActivity(commentIntent);

                break;

            case R.id.flagButton:
                // TODO: Make this go to Firebase
                Toast.makeText(ImageActivity.this, "Flagged", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
