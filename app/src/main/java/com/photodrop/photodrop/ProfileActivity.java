package com.photodrop.photodrop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

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
    public Firebase images, userPhotos;
    private String userKey;
    private ImageAdapter myImageAdapter;
    private ArrayList<String> imageKeys;

    private static final int RESOLUTION_WIDTH = 200;
    private static final int RESOLUTION_HEIGHT = RESOLUTION_WIDTH;

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
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.imageGrid = (GridView) findViewById(R.id.profileActivityGridview);
        this.bitmapList = new ArrayList<Bitmap>();
        // Initializes the Firebase references
        Firebase.setAndroidContext(this);
        images = new Firebase(MainActivity.FIREBASE_IMAGES_URL);
        myImageAdapter = new ImageAdapter(this, this.bitmapList);

        imageGrid.setAdapter(myImageAdapter);
        imageGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent openImageIntent = new Intent(ProfileActivity.this, ImageActivity.class);
                openImageIntent.putExtra(MapsActivity.IMAGE_KEY, imageKeys.get(position));
                startActivity(openImageIntent);
            }
        });

        // Sets the image with the corresponding image key that was passed to this activity
        // imageKey = "-KF2ii44GF_lLh8lNpOG";//getIntent().getStringExtra(MapsActivity.IMAGE_KEY);

        imageKeys = new ArrayList<>();

        userKey = SharedPrefUtil.getUserID(this);
        if(userKey == null) {
            throw new RuntimeException("\n\n T_T Len userKey is null!!!\n\n");
        }

        userPhotos = new Firebase(String.format("%s%s%s",
                MainActivity.FIREBASE_USERS_URL, userKey, MainActivity.USER_PHOTOS_URL));

        System.out.println("  \n\n   >_<     "+userKey+"\n");
        System.out.print("!!!!!!!!!!!!^_^ "+userPhotos.getPath()+"\n");
        // TODO: Removvevent  steners in onPuase() aadd inResume()b
        Query queryRef = userPhotos.orderByKey();
        queryRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChild) {
                System.out.println(">>>>>>>>>>>>>>>>>>>>"+snapshot.getKey()+"\n");

                // Adds the key to the end
                imageKeys.add(imageKeys.size(), snapshot.getKey());

                setImageData(snapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                System.out.println("onChildChanged: I am confused...\n");
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        //bitmapList = null;
        //myImageAdapter = null;
        //imageGrid = null;
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
    public void setImageData(String imageKey) {
        //Log.d("ME", "Adding listener to: " + images.child(imageKey).getPath().toString());
        Log.d("Len", "Adding listener to: " + userPhotos.child(userKey).getPath().toString());
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

                // Decodes the image and makes it a smaller resolution size
                Bitmap bitmap = ImageUtil.decodeSampledBitmap(
                        (String) dataSnapshot.getValue(), RESOLUTION_WIDTH, RESOLUTION_HEIGHT);
                // Sets the image
                bitmapList.add(bitmap);
                //imageGrid.setAdapter(new ImageAdapter(this, this.bitmapList));
                //imageGrid.setAdapter(myImageAdapter);
                myImageAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onCancelled(FirebaseError firebaseError) {
        throw new RuntimeException("\n\n T_T Len onCancelled()!!!\n\n"+firebaseError.getMessage());
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
