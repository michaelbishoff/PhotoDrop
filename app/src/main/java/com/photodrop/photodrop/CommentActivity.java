package com.photodrop.photodrop;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;

public class CommentActivity extends AppCompatActivity {

    // Firebase Objects
    public Firebase comments;
    public Firebase numCommentsRef;
    private String imageKey;
    private String userComment = null;
    private long initialNumComments;
    private long numComments;

    // UI Elements
    private TextView noCommentsText;
    private ListView mListView;
    private Button sendButton;
    private EditText mEditText;

    // Comments Data
    private ArrayList<String> mComments;
    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        // Get the image key from the activity that launched the CommentActivity
        imageKey = getIntent().getStringExtra(MapsActivity.IMAGE_KEY);
        // TODO: Pass the numComments to the Comment Activity so we don't have to go to Firebase
        // twice (once for putting the num comments on the ImageActivity and below)
//        numComments = getIntent().getStringExtra(MapsActivity.NUM_COMMENTS);

        // Firebase reference to comments
        Firebase.setAndroidContext(this);
        comments = new Firebase(String.format("%s%s%s", MainActivity.FIREBASE_IMAGES_URL, imageKey, MainActivity.COMMENTS_URL));
        numCommentsRef = new Firebase(String.format("%s%s%s", MainActivity.FIREBASE_IMAGES_URL, imageKey, MainActivity.NUM_COMMENTS_URL));

        noCommentsText = (TextView) findViewById(R.id.noCommentsText);
        mListView = (ListView) findViewById(R.id.listView);
        sendButton = (Button) findViewById(R.id.buttonSend);
        mEditText = (EditText) findViewById(R.id.editTextComments);

        // Initializes the list view
        mComments = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(CommentActivity.this,
                android.R.layout.simple_list_item_1, mComments);
        mListView.setAdapter(mAdapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
                mEditText.setText("");

                // If the edit text has focus (could have focus and keyboard not showing if the
                // user typed something then pressed the back button to hide the keyboard. So we
                // would be closing the keyboard when it's already closed)
                View view = CommentActivity.this.getCurrentFocus();
                if (view != null && view.getId() == R.id.editTextComments) {

                    // Closes the keyboard
                    // Note: There is no direct way to check if the keyboard is open.
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                    mEditText.clearFocus();
                }
            }
        });

        // Set the number of comments
        numComments = 0;
        numCommentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // If there are comments, set the number of comments
                Object data = dataSnapshot.getValue();
                if (data != null) {
                    initialNumComments = (long) data;
                    noCommentsText.setVisibility(View.GONE);

                } else {
                    initialNumComments = 0;
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e("FirebaseError", firebaseError.getDetails());
            }
        });

        // Loads in new comments as they come in
        comments.orderByPriority().addChildEventListener(new CommentsChildEventListener());
    }

    // TODO: onPause() onResume() listeners and variables
//    @Override
//    protected void onPause() {
//        super.onPause();
//
//        userComment = null;
//        initialNumComments = 0;
//        numComments = 0;
//    }

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
     * Saves the user's message to Firebase and updates the number of comments
     */
    public void sendMessage() {
        userComment = mEditText.getText().toString().trim();

        // Adds the comment if there is a comment and increments the number of comments
        if (!userComment.equals("")) {
            comments.push().setValue(userComment);
            numCommentsRef.runTransaction(new IncrementNumCommentsTransaction());
        }
    }

    /**
     * Increments the number of comments at the specified url (should end in /num_comments)
     */
    private class IncrementNumCommentsTransaction implements Transaction.Handler {

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
         */
        @Override
        public void onComplete(FirebaseError firebaseError, boolean commited, DataSnapshot dataSnapshot) {
            Log.d("ME", "Updating num_comments to " + dataSnapshot.getValue());
        }
    }

    /**
     * Loads the comments from Firebase. Keeps track of new comments that come in so that a Toast
     * can be shown when other comments come in while the user is on the Comments page.
     */
    private class CommentsChildEventListener implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            // Hides the "No Comments" text when a new comment is added and
            // there was no comments initially
            if (noCommentsText.getVisibility() != View.GONE) {
                noCommentsText.setVisibility(View.GONE);
            }

            String comment = dataSnapshot.getValue(String.class);
            mComments.add(comment);

            // Updates the UI
            mAdapter.notifyDataSetChanged();
            Log.d("ME", "comment == " + comment);
            Log.d("ME", "userComment == " + userComment);
            Log.d("ME", "Comments ==? " + comment.equals(userComment));
            // If the user made the comment, go to the bottom
            if (comment.equals(userComment)) {
                mListView.setSelection(mAdapter.getCount());
                initialNumComments++;
                numComments++;

                // If we loaded all of the comments, then there is a new comment
                // at the bottom from a different user
            } else if (initialNumComments != 0 && initialNumComments == numComments) {
                Log.d("ME", "Making toast when comment == " + comment);
//                Toast.makeText(CommentActivity.this, "New Comment", Toast.LENGTH_SHORT).show();
//                    Snackbar.make(findViewById(R.id.listView), "New Comment",
//                            Snackbar.LENGTH_LONG).setAction("Action", null).show();

                // TODO: Want to make our own SnackBar like element that appears above the messageBox
//                    TranslateAnimation animation = new TranslateAnimation(0,0,0,-1000);
//                    animation.setDuration(500);
//                    animation.setFillAfter(true);
//                    animation.setRepeatCount(-1);
//                    animation.setRepeatMode(Animation.REVERSE);
//                    findViewById(R.id.mySnackBar).setAnimation(animation);


                // Counts how many comments we have loaded to figure out when
                // new comments are coming in
            } else {
                numComments++;
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) { }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
            Log.e("FirebaseError", firebaseError.getDetails());
        }
    }
}