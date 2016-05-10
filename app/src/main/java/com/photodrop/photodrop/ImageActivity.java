package com.photodrop.photodrop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;

// TODO: Use this to make transparent action bar: http://stackoverflow.com/questions/26505632/how-to-make-toolbar-transparent
// http://stackoverflow.com/questions/25723331/display-and-hide-navigationbar-and-actionbar-onclickandroid
// TODO: Add a loading icon like the login page
public class ImageActivity extends AppCompatActivity implements ValueEventListener, View.OnClickListener {

    // UI Elements
    private ImageView imageView;
    private ImageButton likeButton, commentButton, flagButton;
    private TextView numLikesText;

    // Firebase Objects
    private Firebase images;
    private String imageKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        // Sets the UI flags so that the layout is fullscreen. This makes the
        // background picture full screen.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // Gets the Image View
        imageView = (ImageView) findViewById(R.id.imageView);

        // Initializes the Firebase reference
        images = new Firebase(MainActivity.FIREBASE_IMAGES_URL);

        // Sets the image with the corresponding image key that was passed to this activity
        imageKey = getIntent().getStringExtra(MapsActivity.IMAGE_KEY);
        setImageData();

        // Gets the UI elements
        likeButton = (ImageButton) findViewById(R.id.likeButton);
        commentButton = (ImageButton) findViewById(R.id.commentButton);
        flagButton = (ImageButton) findViewById(R.id.flagButton);
        numLikesText = (TextView) findViewById(R.id.numLikes);

        //if the user has not liked the image
        if(!SharedPrefUtil.getCurrentUsersLike(getApplicationContext(),imageKey))
        {
            SharedPrefUtil.saveCurrentUsersLike(getApplicationContext(), imageKey, false);
        }
        else
        {
            likeButton.setImageResource(R.drawable.icon_teal_like);
        }

        //if the user has not flagged the image
        if(!SharedPrefUtil.getCurrentUsersFlag(getApplicationContext(), imageKey))
        {
            SharedPrefUtil.saveCurrentUsersFlag(getApplicationContext(), imageKey, false);
        }
        else
        {
            flagButton.setImageResource(R.drawable.icon_red_flag);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        likeButton.setOnClickListener(this);
        commentButton.setOnClickListener(this);
        flagButton.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        likeButton.setOnClickListener(null);
        commentButton.setOnClickListener(null);
        flagButton.setOnClickListener(null);

        // TODO: Write the bitmap to a file so we can bring it back up later without wasting memory
        // or (what we don't do) re-connecting to Firebase download the photo again
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
    public void setImageData() {
        Log.d("ME", "Adding listener to: " + images.child(imageKey).getPath().toString());

        // TODO: Should probably save the image locally and check if the image is saved before accessing the DB again

        // Adds a listener then removes it once it's triggered so that the image view and num likes are set
        images.child(imageKey + MainActivity.IMAGE_URL).addListenerForSingleValueEvent(this);
        images.child(imageKey + MainActivity.LIKES_URL).addListenerForSingleValueEvent(this);
        //images.child(imageKey + MainActivity.COMMENTS_URL).addListenerForSingleValueEvent(this);
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
        Log.d("ME", "Key of this callback: " + dataSnapshot.getKey());

        // If there's a value in the
        if (dataSnapshot.getValue() != null) {

            String key = dataSnapshot.getKey();

            if (key.equals("image")) {
                Log.d("ME", "Getting image from: " + dataSnapshot.getRef().getPath());
                // Sets the image
                imageView.setImageBitmap(ImageUtil.getBitmapFromEncodedImage(
                        (String) dataSnapshot.getValue()));

            } else if (key.equals("likes")) {
                Log.d("ME", "Loading Likes: " + dataSnapshot.getValue());
                numLikesText.setText(String.format("%d Likes", (long) dataSnapshot.getValue()));

            }
            // TODO: Add num comments to the image view
//            else if (key.equals("num_comments")) { }
        }
    }

    @Override
    public void onCancelled(FirebaseError firebaseError) { }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.likeButton:

                //check if the image has been liked
                if(!SharedPrefUtil.getCurrentUsersLike(getApplicationContext(), imageKey)) {
                    // Starts a thread safe incrementation of the number of likes
                    images.child(imageKey + MainActivity.LIKES_URL).runTransaction(new IncrementLikesTransaction());
                    //disable the like functionality
                    likeButton.setImageResource(R.drawable.icon_teal_like);
                    likeButton.setEnabled(false);

                }
                SharedPrefUtil.saveCurrentUsersLike(getApplicationContext(),imageKey, true);
                break;


            case R.id.commentButton:
                // Opens the CommentActivity
                Intent commentIntent = new Intent(ImageActivity.this, CommentActivity.class);
                // Passes this image's key to the CommentActivity
                commentIntent.putExtra(MapsActivity.IMAGE_KEY, imageKey);
                startActivity(commentIntent);

                break;

            case R.id.flagButton:
                //check if the image has been liked
                if(!SharedPrefUtil.getCurrentUsersFlag(getApplicationContext(), imageKey)) {
                    // Starts a thread safe incrementation of the number of flag
                    images.child(imageKey + MainActivity.FLAGS_URL).runTransaction(new IncrementFlagsTransaction());
                    //disable the flag functionality
                    flagButton.setImageResource(R.drawable.icon_red_flag);
                    flagButton.setEnabled(false);
                }
                SharedPrefUtil.saveCurrentUsersFlag(getApplicationContext(),imageKey, true);
                //Toast.makeText(ImageActivity.this, "Flagged", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    /* Firebase Transaction
     * Transactions are used when concurrent modifications could corrupt the data
     */

    /**
     * Increments the number of likes at the specified url (should end in /likes)
     */
    private class IncrementLikesTransaction implements Transaction.Handler {

        /**
         * The Transaction we want to run on the specified data.
         * If there is no value, set it. If there is a value, increment it.
         */
        @Override
        public Transaction.Result doTransaction(MutableData mutableData) {
            if (mutableData.getValue() == null) {
                mutableData.setValue(1);
            } else {
                mutableData.setValue((long) mutableData.getValue() + 1);
            }

            return Transaction.success(mutableData); // we can also abort by calling Transaction.abort()
        }

        /**
         * This method will be called once with the results of the transaction.
         * Updates the UI with the new like count.
         */
        @Override
        public void onComplete(FirebaseError firebaseError, boolean commited, DataSnapshot dataSnapshot) {
            Log.d("ME", "Updating UI to " + dataSnapshot.getValue() + " num likes");
            numLikesText.setText(String.format("%d Likes", (long) dataSnapshot.getValue()));
        }
    }



    private class IncrementFlagsTransaction implements Transaction.Handler {

        /**
         * The Transaction we want to run on the specified data.
         * If there is no value, set it. If there is a value, increment it.
         */
        @Override
        public Transaction.Result doTransaction(MutableData mutableData) {
            if (mutableData.getValue() == null) {
                mutableData.setValue(1);
            } else {
                mutableData.setValue((long) mutableData.getValue() + 1);
            }

            return Transaction.success(mutableData); // we can also abort by calling Transaction.abort()
        }

        /**
         * This method will be called once with the results of the transaction.
         * Updates the UI with the new like count.
         */
        @Override
        public void onComplete(FirebaseError firebaseError, boolean commited, DataSnapshot dataSnapshot) {
            Log.d("ME", "Updating UI to " + dataSnapshot.getValue() + " num flags");

        }
    }
}
