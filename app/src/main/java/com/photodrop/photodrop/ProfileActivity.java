package com.photodrop.photodrop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity implements ValueEventListener {

    private GridView imageGrid;
    private ArrayList<Bitmap> bitmapList;
    public Firebase images, userPhotos;
    private String userKey;
    private ImageAdapter myImageAdapter;
    private ArrayList<String> imageKeys;
    private View profileProgressView;

    private long numPhotos;

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

        profileProgressView = findViewById(R.id.profile_progress);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO: Some of this should be in onCreate()

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

        Query queryRef = userPhotos.orderByKey();
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showProgress(true);
                numPhotos = dataSnapshot.getChildrenCount();

                // TODO: Make an ExternalFileUtil class

                File photosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File photodropDir = new File(photosDir, MainActivity.PHOTODROP_DIR);
                File[] localPhotos = photodropDir.listFiles();

                // TODO: if they have < numPhotos, download the ones they are missing, then show them all
                // Or (maybe) make a numPhotos on firebase rather than getting the length of
                // all of them, then get all of them if there is a difference maybe? But then
                // that's another DB access

                if (localPhotos != null && localPhotos.length == numPhotos) {
                    loadPhotosLocally(localPhotos);

                } else {
                    // For each photo that the user has taken, add it to the grid view
                    for (DataSnapshot photo : dataSnapshot.getChildren()) {
                        imageKeys.add(photo.getKey());
                        setImageData(photo.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e("FirebaseError", firebaseError.getMessage());
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
                Bitmap bitmap = ImageUtil.decodeStringAndSampledBitmap(
                        (String) dataSnapshot.getValue(), RESOLUTION_WIDTH, RESOLUTION_HEIGHT);
                // Sets the image
                bitmapList.add(bitmap);
                //imageGrid.setAdapter(new ImageAdapter(this, this.bitmapList));
                //imageGrid.setAdapter(myImageAdapter);
                myImageAdapter.notifyDataSetChanged();
            }
        }
        numPhotos--;
        if (numPhotos == 0) {
            showProgress(false);
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

            profileProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            profileProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    profileProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            profileProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Loads the photos locally rather than going to the server asynchronously
     */
    private void loadPhotosLocally(final File[] images) {
        Log.d("ME", "Loading photos Locally");
        new AsyncTask<File, Integer, Void>(){
            @Override
            protected Void doInBackground(File... params) {
                for (File image : images) {
                    // Decodes the image and makes it a smaller resolution size
                    Bitmap bitmap = ImageUtil.decodeFileAndSampleBitmap(
                            image.getAbsolutePath(), RESOLUTION_WIDTH, RESOLUTION_HEIGHT);
                    // Sets the image
                    bitmapList.add(bitmap);
                    // Removes the file extension from the file name
                    String imageName = image.getName();
                    String imageKey = imageName.substring(0, imageName.length() - 4);
                    // Adds the image key to the list of image keys
                    imageKeys.add(imageKey);
                }
                return null;
            }

            // Updates the UI on the main UI thread
            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                // Notifies that the grid view has changed so the UI changes
                myImageAdapter.notifyDataSetChanged();

                // Stop the loading bar
                showProgress(false);
            }
        }.execute(images);
    }
}