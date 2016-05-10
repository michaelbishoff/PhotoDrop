package com.photodrop.photodrop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
    private View imageProgressView;

    // Firebase Objects
    private Firebase images;
    private String imageKey;

    // The amount to change the number of likes and flags
    private static int INCREASE_BY_ONE = 1;
    private static int DECREASE_BY_ONE = -1;

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
        imageProgressView = findViewById(R.id.image_progress);

        // Sets the font of the like text
        Typeface font = Typeface.createFromAsset(this.getAssets(), "fonts/ValterStd-Thin.ttf");
        numLikesText.setTypeface(font);

        // If the user has liked the image
        if(SharedPrefUtil.getCurrentUsersLike(getApplicationContext(),imageKey)) {
            likeButton.setImageResource(R.drawable.icon_like_teal);
        }

        // If the user has flagged the image
        if(SharedPrefUtil.getCurrentUsersFlag(getApplicationContext(), imageKey)) {
            flagButton.setImageResource(R.drawable.icon_flag_red);
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

                // Hides the progress bar
                showProgress(false);

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

                // If the user previously liked the photo, un-like it
                if(SharedPrefUtil.getCurrentUsersLike(getApplicationContext(), imageKey)) {
                    // Starts a thread safe decrementation of the number of likes
                    images.child(imageKey + MainActivity.LIKES_URL).runTransaction(new AdjustLikesTransaction(DECREASE_BY_ONE));
                    // Update the UI
                    likeButton.setImageResource(R.drawable.icon_like_white);
                    // Mark the image as liked locally
                    SharedPrefUtil.saveCurrentUsersLike(getApplicationContext(), imageKey, false);

                    // The user likes this photo
                } else {
                    // Starts a thread safe incrementation of the number of likes
                    images.child(imageKey + MainActivity.LIKES_URL).runTransaction(new AdjustLikesTransaction(INCREASE_BY_ONE));
                    // Update the UI
                    likeButton.setImageResource(R.drawable.icon_like_teal);
                    // Mark the image as liked locally
                    SharedPrefUtil.saveCurrentUsersLike(getApplicationContext(), imageKey, true);
                }
                break;


            case R.id.commentButton:
                // Opens the CommentActivity
                Intent commentIntent = new Intent(ImageActivity.this, CommentActivity.class);
                // Passes this image's key to the CommentActivity
                commentIntent.putExtra(MapsActivity.IMAGE_KEY, imageKey);
                startActivity(commentIntent);
                break;

            case R.id.flagButton:
                // Check if the image has been flagged
                // If the user previously flagged the photo, un-flag it
                if(SharedPrefUtil.getCurrentUsersFlag(getApplicationContext(), imageKey)) {
                    // Starts a thread safe decrementation of the number of flag
                    images.child(imageKey + MainActivity.FLAGS_URL).runTransaction(new AdjustFlagsTransaction(DECREASE_BY_ONE));
                    // Update the UI
                    flagButton.setImageResource(R.drawable.icon_flag_white);
                    // Mark the image as flagged locally
                    SharedPrefUtil.saveCurrentUsersFlag(getApplicationContext(), imageKey, false);

                    // The user flagged this photo
                } else {
                    // Starts a thread safe incrementation of the number of flag
                    images.child(imageKey + MainActivity.FLAGS_URL).runTransaction(new AdjustFlagsTransaction(INCREASE_BY_ONE));
                    // Update the UI
                    flagButton.setImageResource(R.drawable.icon_flag_red);
                    // Mark the image as flagged locally
                    SharedPrefUtil.saveCurrentUsersFlag(getApplicationContext(), imageKey, true);

                    Toast.makeText(ImageActivity.this, "Flagged", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            imageView.setVisibility(show ? View.GONE : View.VISIBLE);
            imageView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    imageView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            imageProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            imageProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    imageProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            imageProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            imageView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    /* Firebase Transaction
     * Transactions are used when concurrent modifications could corrupt the data
     */

    /**
     * Adjusts the number of likes at the specified url (should end in /likes)
     */
    private class AdjustLikesTransaction implements Transaction.Handler {
        private int difference;

        public AdjustLikesTransaction(int difference) {
            super();
            this.difference = difference;
        }

        /**
         * The Transaction we want to run on the specified data.
         * If there is no value, set it. If there is a value, increment it.
         */
        @Override
        public Transaction.Result doTransaction(MutableData mutableData) {
            if (mutableData.getValue() == null) {
                mutableData.setValue(1);
            } else {
                mutableData.setValue((long) mutableData.getValue() + difference);
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

    /**
     * Adjusts the number of flags at the specified url (should end in /flags)
     */
    private class AdjustFlagsTransaction implements Transaction.Handler {
        private int difference;

        public AdjustFlagsTransaction(int difference) {
            super();
            this.difference = difference;
        }

        /**
         * The Transaction we want to run on the specified data.
         * If there is no value, set it. If there is a value, increment it.
         */
        @Override
        public Transaction.Result doTransaction(MutableData mutableData) {
            if (mutableData.getValue() == null) {
                mutableData.setValue(1);
            } else {
                mutableData.setValue((long) mutableData.getValue() + difference);
            }

            return Transaction.success(mutableData); // we can also abort by calling Transaction.abort()
        }

        /**
         * This method will be called once with the results of the transaction.
         * Updates the UI with the new like count.
         */
        @Override
        public void onComplete(FirebaseError firebaseError, boolean commited, DataSnapshot dataSnapshot) {
            Log.d("ME", "Updating to " + dataSnapshot.getValue() + " num flags");
        }
    }
}
