package com.photodrop.photodrop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import android.widget.GridView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.net.URL;
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity implements ValueEventListener {
    private GridView imageGrid;
    private ArrayList<Bitmap> bitmapList;
    public Firebase images;
    public Firebase user;
    private String imageKey;
    private String userKey;
    public static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";
    public static final String FIREBASE_IMAGES_URL = FIREBASE_URL + "images";
    public static final String FIREBASE_USER_URL = FIREBASE_URL + "users";
    public static final String IMAGE_URL = "/image";
    public static final String PHOTO_URL= "/photos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.profileToolbar);
        setSupportActionBar(toolbar);
        System.out.print(">_<?\n\n");
        // Adds the back button to the toolbar since we're adding it dynamically
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        System.out.print(">_<\n\n");

        this.imageGrid = (GridView) findViewById(R.id.profileActivityGridview);
        this.bitmapList = new ArrayList<Bitmap>();
        // Initializes the Firebase references
        Firebase.setAndroidContext(this);
        images = new Firebase(FIREBASE_IMAGES_URL);

        // Sets the image with the corresponding image key that was passed to this activity
        imageKey = "-KF2ii44GF_lLh8lNpOG";//getIntent().getStringExtra(MapsActivity.IMAGE_KEY);
        userKey ="michaelbishoff";
        user = new Firebase(FIREBASE_USER_URL+"/"+userKey+PHOTO_URL);
        System.out.print("!!!!!!!!!!!!^_^"+"\n");
        //user.orderByChild().getPath();
        Query queryRef = user.orderByKey();
        queryRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChild) {
                System.out.println(">>>>>>>>>>>>>>>>>>>>"+snapshot.getKey()+"\n");
                imageKey = snapshot.getKey();
                setImageData();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }


        });


        //setImageData();



//        try {
//            for(int i = 0; i < 10; i++) {
//                System.out.print(">_<!\n\n");
//                this.bitmapList.add(urlImageToBitmap("http://placehold.it/150x150"));
//
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }
    private Bitmap urlImageToBitmap(String imageUrl) throws Exception {
        Bitmap result = null;
        URL url = new URL(imageUrl);
        System.out.print(">_<!!\n\n");
        if(url != null) {
          //  System.out.print(">_<!!!  if(url != null)\n\n");
            //if (android.os.Build.VERSION.SDK_INT > 9) {
              //  StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                //StrictMode.setThreadPolicy(policy);
            //}
            result = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            //result = BitmapFactory.decodeStream();
        }
        return result;
    }

    /**
     * Sets the image view with the image with the corresponding key in Firebase
     */
    public void setImageData() {
        //Log.d("ME", "Adding listener to: " + images.child(imageKey).getPath().toString());
        Log.d("Len", "Adding listener to: " + user.child(userKey).getPath().toString());
        // TODO: Should probably save the image locally and check if the image is saved before accessing the DB again

        // Adds a listener then removes it once it's triggered so that the image view and num likes are set
        images.child(imageKey + MainActivity.IMAGE_URL).addListenerForSingleValueEvent(this);
        //user.child(userKey+"/user").addListenerForSingleValueEvent(this);
    }
    /**
     * Sets the image view with the encoded image from Firebase
     */
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        Log.d("Len", "Key of this callback: " + dataSnapshot.getKey());

        // If there's a value in the
        if (dataSnapshot.getValue() != null) {

            String key = dataSnapshot.getKey();

            if (key.equals("image")) {
                Log.d("ME", "Getting image from: " + dataSnapshot.getRef().getPath());
                Bitmap bitmap = ImageUtil.getBitmapFromEncodedImage(
                        (String) dataSnapshot.getValue());
                // Sets the image
                bitmapList.add(bitmap);
                imageGrid.setAdapter(new ImageAdapter(this, this.bitmapList));
            }
        }
    }

    @Override
    public void onCancelled(FirebaseError firebaseError) { }
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
