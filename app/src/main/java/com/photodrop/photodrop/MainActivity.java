package com.photodrop.photodrop;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MapsActivity.LocationDataTransfer, ValueEventListener {

    // UI Buttons
    private FloatingActionButton fab;
    private ImageView imageView;
    private ImageButton profileButton;
    private FloatingActionButton compassButton;
    private static final int START_CAMERA = 1000;
    private static final int IMAGE_QUALITY = 1;

    // Firebase Objects
    private Firebase images;
    private GeoFire geoFire;
    private static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";
    private static final String IMAGES_URL = FIREBASE_URL + "images";
    private static final String GEOFIRE_URL = FIREBASE_URL + "drops";

    // Service Objects
    private LocationService.LocationServiceBinder binder;
    private LocationService locationService;
    private boolean connected = false;

    // The Map Fragment
    private MapsActivity mapsActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        // Gets the UI elements
        fab = (FloatingActionButton) findViewById(R.id.fab);
        imageView = (ImageView) findViewById(R.id.imageView);
        profileButton = (ImageButton) findViewById(R.id.profileButton);
        compassButton = (FloatingActionButton) findViewById(R.id.compassButton);

        // Gets the Map Fragment so we can call setLocation()
        mapsActivity = (MapsActivity) getSupportFragmentManager().findFragmentById(R.id.map);

        // Initializes the Firebase references
        Firebase.setAndroidContext(this);
        images = new Firebase(IMAGES_URL);
        geoFire = new GeoFire(new Firebase(GEOFIRE_URL));

        // Binds the service. Calls the onServiceConnected() method below
        Intent serviceIntent = new Intent(this, LocationService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    /**
     * Adds the OnClickListeners
     */
    @Override
    protected void onResume() {
        super.onResume();
        fab.setOnClickListener(this);
        profileButton.setOnClickListener(this);
        compassButton.setOnClickListener(this);

        if (connected) {
            locationService.resumeService();
        }
    }

    /**
     * Removes the OnClickListeners
     */
    @Override
    protected void onPause() {
        super.onPause();
        fab.setOnClickListener(null);
        profileButton.setOnClickListener(null);
        compassButton.setOnClickListener(null);

        if (connected) {
            locationService.pauseService();
        }
    }

    /**
     * Handles the UI button clicks
     */
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.fab:

                // Open the camera and take a picture
                Intent cameraIntent = new Intent();
                cameraIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, START_CAMERA);

                break;

            case R.id.profileButton:

                // Switch to profile page
                Toast.makeText(MainActivity.this, "Show Profile Page", Toast.LENGTH_SHORT).show();
                break;

            // Sets the maps center when the user presses the compass button
            case R.id.compassButton:
                Location userLocation = locationService.getUserLocation();
                if (userLocation != null) {
                    Log.d("MainActivity", "User Location != null");
                    mapsActivity.setLocation(userLocation);
                }
                break;
        }
    }

    /**
     * Handles the result of the cameraIntent. Fixes the images orientation, sets the image to
     * the one in the corner, and saves the image to Firebase.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_CAMERA) {
            if (resultCode == RESULT_OK) {
                try {

                    // Gets the image bitmap
                    Uri imageUri = data.getData();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                    // Gets the orientation of the photo
                    String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
                    Cursor cur = this.getContentResolver().query(imageUri, orientationColumn, null, null, null);
                    int orientation = -1;
                    if (cur != null && cur.moveToFirst()) {
                        orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
                    }
                    cur.close();


                    // Redundency check since the cursor should always have something in it
                    if (orientation == -1) {
                        Log.e("MainActivity", "Orientation still -1");
                    }
                    else {
                        // Correct the image's rotation
                        bitmap = rotateImage(bitmap, orientation);

                        // Set the image on screen
                        imageView.setImageBitmap(bitmap);

                        // Submits the image to the database in a new thread so the app runs super duper fast
                        new Thread(new SaveImage(bitmap)).start();
                    }
                } catch (IOException e) {
                    Log.e("MainActivity", "BITMAP ERROR: " + e.toString());
                }
            }
        }
    }

    /**
     * Rotates the image at the specified angle
     */
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Bitmap result;

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        result = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

        return result;
    }

    /**
     * Saves the image to Firebase
     */
    public class SaveImage implements Runnable {
        private Bitmap bitmap;
        public SaveImage(Bitmap bitmap) {
            this.bitmap = bitmap.copy(bitmap.getConfig(), true);
        }

        @Override
        public void run() {

            // Converts the image to an encoded String
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // PNG is lossless quality but 100 ensures max quality
            bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, byteArrayOutputStream);
            bitmap.recycle();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String imageFile = Base64.encodeToString(byteArray, Base64.DEFAULT);

            // Getting the user's location
            Location location = locationService.getUserLocation();

            Log.d("ME", String.format("Saving image at:\nlat: %.10f\nlng: %.10f",
                    location.getLatitude(), location.getLongitude()));

            // Generates a unique hash to store the image
            String imageKey = images.push().getKey();

            // Saves the location of the drop and saves the image with the same key
            geoFire.setLocation(imageKey, new GeoLocation(location.getLatitude(), location.getLongitude()));
            images.child(imageKey).setValue(imageFile);

            // Frees up some memory (i think lol)
            imageFile = null;
            imageKey = null;
            bitmap = null;
            byteArrayOutputStream = null;
            byteArray = null;
            location = null;
        }
    }


    /* Service Methods */

    /**
     * The service connection used to call functions in ActivityMonitorService
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // Gets the binder
            binder = (LocationService.LocationServiceBinder) service;

            // Calls a callback type of function that gets the parent of the binder
            locationService = binder.getService();

            // Indicates that we have access to the service
            connected = true;

            // Now the Activity is bound to the Service and we can call the
            // public methods defined in the activityMonitorService class

            // Resumes the service
            locationService.resumeService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected = false;
        }
    };


    /* MapsActivity Interface methods */

    /**
     * Method defined in MapsActivity so that the MapsActivity can get the location data.
     * (this may not need to be from the interface, but the interface ensures it)
     */
    @Override
    public Location getLocation() {
        if (connected) {
            return locationService.getUserLocation();
        } else {
            Log.d("MainActivity", "NOT CONNECTED AND REQUESTING LOCATION");
            return null;
        }
//        return locationService.getUserLocation();
    }

    /**
     * Sets the image view with the image with the corresponding key in Firebase
     */
    @Override
    public void setImage(String key) {
        Log.d("ME", "Adding listener to: " + images.child(key).getPath().toString());

        // TODO: Should probably save the image locally and check if the image is saved before accessing the DB again

        // Adds a listener then removes it once it's triggered so that the image view is set
        images.child(key).addListenerForSingleValueEvent(this);
    }

    /**
     * Returns the geoFire object so that the MapsActivity can use
     * the same object and save on memory :)
     */
    @Override
    public GeoFire getGeoFire() {
        return geoFire;
    }

    /**
     * Sets the image view with the encoded image that is passed in
     */
    private void setImageEncoded(String encodedImage) {

        // TODO: Remove this
        // Checks if the encoded image string is null. This occurs when there is no image
        // stored at that location because of testing
        if (encodedImage != null) {

            Log.d("ME", "Encoded Image != null! ");
            // Decodes the image
            byte[] decodedImage = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);

            // Sets the image
            imageView.setImageBitmap(bitmap);

            // Frees up some memory (i think lol)
            bitmap = null;
            decodedImage = null;
        }
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
        setImageEncoded((String) dataSnapshot.getValue());
    }

    @Override
    public void onCancelled(FirebaseError firebaseError) { }
}
