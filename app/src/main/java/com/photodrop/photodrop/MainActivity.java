package com.photodrop.photodrop;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;

// TODO: Getting Performing stop of activity that is not resumed error http://stackoverflow.com/questions/26375920/android-performing-stop-of-activity-that-is-not-resumed
// TODO: Resource not found Exception

// Note: Was thinking about making a FirebaseUtil class that handles all of the firebase accesses
// and makes it so the entire app references the same Firebase object rather than having
// duplicates of the light weight references

// TODO: Why does the app use so much memory after taking a photo
// TODO: Delete teh photo on the user's device

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MapsActivity.LocationDataTransfer {

    // UI Buttons
    private FloatingActionButton fab;
    private ImageButton profileButton;
    private FloatingActionButton compassButton;
    private static final int CAMERA_REQUEST = 1000;
    private static final int IMAGE_QUALITY = 1;

    // Firebase Objects
    public Firebase images;
    private GeoFire geoFire;
    public static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";
    public static final String FIREBASE_IMAGES_URL = FIREBASE_URL + "images";
    public static final String GEOFIRE_URL = FIREBASE_URL + "drops";
    // Keys for a specific image, comments, or likes
    public static final String IMAGE_URL = "/image";
    public static final String COMMENTS_URL = "/comments";
    public static final String LIKES_URL = "/likes";

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
        profileButton = (ImageButton) findViewById(R.id.profileButton);
        compassButton = (FloatingActionButton) findViewById(R.id.compassButton);

        // Gets the Map Fragment so we can call setLocation()
        mapsActivity = (MapsActivity) getSupportFragmentManager().findFragmentById(R.id.map);

        // Initializes the Firebase references
        Firebase.setAndroidContext(this);
        images = new Firebase(FIREBASE_IMAGES_URL);
        geoFire = new GeoFire(new Firebase(GEOFIRE_URL));

        Log.d("ME-MainActivity-DO", "Done onCreate()");
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

        // Binds the service. Calls the onServiceConnected() method below
        Intent serviceIntent = new Intent(this, LocationService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
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

        // This may be redundant since we're unbinding the service immediately after
        if (connected) {
            locationService.pauseService();
        }

        unbindService(serviceConnection);

        Log.d("ME", "Everything Paused");
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
                startActivityForResult(cameraIntent, CAMERA_REQUEST);

                break;

            case R.id.profileButton:

                // Switch to profile page
                Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
//                profileIntent.putExtra("USERNMAE", "michaelbishoff");
                startActivity(profileIntent);
                break;

            // Sets the maps center when the user presses the compass button
            case R.id.compassButton:
                mapsActivity.setLocation(locationService.getUserLocation());
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

        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (data != null && data.getData() != null) {
                    // Gets the bitmap of the image from the URI
                    Bitmap imageBitmap = ImageUtil.getBitmapFromUri(this, data.getData());
//                    Bitmap imageBitmap = (Bitmap) data.getExtras().get("data"); // This is of lower resolution

                    // Saves the image to Firebase
                    new Thread(new SaveImage(imageBitmap)).start();

                } else {
                    Toast.makeText(MainActivity.this, "Photo not saved :(", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Saves the image to Firebase
     * TODO: Convert this to an AsyncTask if we want updates about the image's save progress
     */
    public class SaveImage implements Runnable {
        private Bitmap bitmap;
        public SaveImage(Bitmap bitmap) {
            // TODO: Could remove the .copy() and remove the bitmap.recycle() in the ImageUtil,
            // but will the garbage collector keep the SaveImage around because it points to
            // the bitmap in the imageView?
            this.bitmap = bitmap;//.copy(bitmap.getConfig(), true);
        }

        @Override
        public void run() {
            // TODO: If !connected, then locationService is null and it crashes

            // Getting the user's location
            Location location = locationService.getUserLocation();

            Log.d("ME", String.format("Saving image at:\nlat: %.10f\nlng: %.10f",
                    location.getLatitude(), location.getLongitude()));

            // Generates a unique hash to store the image
            String imageKey = images.push().getKey();

            // Saves the location of the drop and saves the image with the same key
            geoFire.setLocation(imageKey, new GeoLocation(location.getLatitude(), location.getLongitude()));
            images.child(imageKey + IMAGE_URL).setValue(ImageUtil.encodeBitmap(bitmap, IMAGE_QUALITY));

            // Frees up some memory (i think lol)
            imageKey = null;
            bitmap = null;
            location = null;

            Log.d("ME", "Image saved!");
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

            // Sets the user's location on the map
            mapsActivity.setLocation(locationService.getUserLocation());
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
     * Returns the geoFire object so that the MapsActivity can use
     * the same object and save on memory :)
     */
    @Override
    public GeoFire getGeoFire() {
        return geoFire;
    }
}
