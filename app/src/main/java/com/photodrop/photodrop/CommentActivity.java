package com.photodrop.photodrop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;

public class CommentActivity extends AppCompatActivity {

    // Firebase Objects
    public Firebase images;
    public  Firebase comments;
    public static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";
    public static final String IMAGES_URL = FIREBASE_URL + "images";
    private String imageKey;

    //UI
    ListView mListView;
    Button mButton;
    EditText mEditText;

    //Comments Data
    ArrayList<String> mComments = new ArrayList<>();
    ArrayAdapter<String> mAadapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        //Reference to comments
        Firebase.setAndroidContext(this);
        images = new Firebase(IMAGES_URL);
        imageKey = getIntent().getStringExtra(MapsActivity.IMAGE_KEY);
        comments = images.child(imageKey + MainActivity.COMMENTS_URL);

        mListView = (ListView) findViewById(R.id.listView);

        mButton = (Button) findViewById(R.id.buttonSend);
        mEditText = (EditText) findViewById(R.id.editTextComments);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                }
                return true;
            }
        });

        mEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.setText("");
            }
        });


        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
                mEditText.setText("");
            }
        });


        images.child(imageKey + MainActivity.COMMENTS_URL).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                mComments.clear();

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String comment = (String) child.getValue();
                    mComments.add(comment);
                }

                mAadapter = new ArrayAdapter<>(
                        CommentActivity.this,
                        android.R.layout.simple_list_item_1,
                        mComments);

                mListView.setAdapter(mAadapter);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


    }


    public void sendMessage(){
        String temp = mEditText.getText().toString();
        //mComments.add(temp);
        if (!images.equals(""))
        {
            images.child(imageKey + MainActivity.COMMENTS_URL).push().setValue(temp);
        }
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
}
