package com.photodrop.photodrop;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;

// TODO: Getting Performing stop of activity that is not resumed error http://stackoverflow.com/questions/26375920/android-performing-stop-of-activity-that-is-not-resumed
// TODO: Resource not found Exception

// Note: Was thinking about making a FirebaseUtil class that handles all of the firebase accesses
// and makes it so the entire app references the same Firebase object rather than having
// duplicates of the light weight references

// TODO: Why does the app use so much memory after taking a photo
// TODO: Delete teh photo on the user's device

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MapsActivity.LocationDataTransfer {

    // UI Buttons
    private ImageButton profileButton, cameraButton, settingsButton;
    private static final int CAMERA_REQUEST = 1000;
    private static final int IMAGE_QUALITY = 1;

    // Firebase Objects
    public Firebase images, userPhotos;
    private GeoFire geoFire;
    public static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";
    public static final String FIREBASE_IMAGES_URL = FIREBASE_URL + "images/";
    public static final String FIREBASE_USERS_URL = FIREBASE_URL + "users/";
    public static final String GEOFIRE_URL = FIREBASE_URL + "drops";
    // Keys for a specific image, comments, or likes
    public static final String IMAGE_URL = "/image";
    public static final String COMMENTS_URL = "/comments";
    public static final String LIKES_URL = "/likes";
    public static final String FLAGS_URL = "/flags";
    public static final String NUM_COMMENTS_URL = "/num_comments";
    public static final String USER_PHOTOS_URL = "/photos";

    // Service Objects
    private LocationService.LocationServiceBinder binder;
    private LocationService locationService;
    private boolean connected = false;

    // The Map Fragment
    private MapsActivity mapsActivity;

    // The location to save photos
    public File photodropDir;
    // TODO: Probably don't need this Uri, but just the path
    private Uri photoSaveLocation;
    public static final String PHOTODROP_DIR = "Photodrop/";
    private static final String TEMP_IMAGE_FILENAME = "temp.png";
    public static final String FILE_EXTENSION = ".png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        // Gets the UI elements
        cameraButton = (ImageButton) findViewById(R.id.cameraButton);
        profileButton = (ImageButton) findViewById(R.id.profileButton);
        settingsButton = (ImageButton) findViewById(R.id.settingsButtons);

        // Gets the Map Fragment so we can call setLocation()
        mapsActivity = (MapsActivity) getSupportFragmentManager().findFragmentById(R.id.map);

        // Initializes the Firebase references
        Firebase.setAndroidContext(this);
        images = new Firebase(FIREBASE_IMAGES_URL);
        userPhotos = new Firebase(String.format("%s%s%s", FIREBASE_USERS_URL, SharedPrefUtil.getUserID(this), USER_PHOTOS_URL));
        geoFire = new GeoFire(new Firebase(GEOFIRE_URL));

        // TODO: Make an ExternalFileUtil class

        // Specify where we want to save the photo
        File photosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        photodropDir = new File(photosDir, PHOTODROP_DIR);
        File tempPhotoLocation = new File(photodropDir, TEMP_IMAGE_FILENAME);
        photoSaveLocation = Uri.fromFile(tempPhotoLocation);

        Log.d("DIR", "file: " + tempPhotoLocation.getAbsolutePath());
        Log.d("DIR", "uri:  " + photoSaveLocation.getPath());

        Log.d("ME-MainActivity-DO", "Done onCreate()");
    }

    /**
     * Adds the OnClickListeners
     */
    @Override
    protected void onResume() {
        super.onResume();

        cameraButton.setOnClickListener(this);
        profileButton.setOnClickListener(this);
        settingsButton.setOnClickListener(this);

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
        cameraButton.setOnClickListener(null);
        profileButton.setOnClickListener(null);
        settingsButton.setOnClickListener(null);

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
            case R.id.cameraButton:

                // Open the camera and take a picture
                Intent cameraIntent = new Intent();
                cameraIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoSaveLocation);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
                break;

            case R.id.profileButton:

                // Switch to profile page
                Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
//                profileIntent.putExtra("USERNMAE", "michaelbishoff");
                startActivity(profileIntent);
                break;

            // Sets the maps center when the user presses the compass button
            case R.id.settingsButtons:

                //Switch to settings page
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
//                mapsActivity.setLocation(locationService.getUserLocation());
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
                File image = new File(photoSaveLocation.getPath());
                Log.d("ME", "Imaged saved to: " + image.getAbsolutePath());
                if (image.exists()) {

                    // Saves the image to Firebase
                    new Thread(new SaveImage(photoSaveLocation)).start();

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
        private Uri uri;
        public SaveImage(Uri uri) {
            // TODO: Could remove the .copy() and remove the bitmap.recycle() in the ImageUtil,
            // but will the garbage collector keep the SaveImage around because it points to
            // the bitmap in the imageView?
            this.uri = uri;//.copy(bitmap.getConfig(), true);
        }

        @Override
        public void run() {
            // TODO: If !connected, then locationService is null and it crashes
            if (connected) {

                // Getting the user's location
                Location location = locationService.getUserLocation();

                Log.d("ME", String.format("Saving image at:\nlat: %.10f\nlng: %.10f",
                        location.getLatitude(), location.getLongitude()));

                // Generates a unique hash to store the image
                String imageKey = images.push().getKey();

                // Saves the location of the drop and saves the image with the same key
                geoFire.setLocation(imageKey, new GeoLocation(location.getLatitude(), location.getLongitude()));
                images.child(imageKey + IMAGE_URL).setValue(ImageUtil.encodeFile(MainActivity.this, uri, IMAGE_QUALITY));
                userPhotos.child(imageKey).setValue(1);

                // Renames the file to the key name
                File savedImage = new File(uri.getPath());
                savedImage.renameTo(new File(photodropDir.getAbsolutePath() + "/" + imageKey + FILE_EXTENSION));

                // Lets other apps know about the photo that we took
                galleryAddPic(Uri.fromFile(savedImage));

                // Frees up some memory (i think lol)
                imageKey = null;
                location = null;
                savedImage = null;

                Log.d("ME", "Image saved!");
            } else {
                Toast.makeText(MainActivity.this, "Enable Location Services", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Lets other apps know about the photo that we took
         * @param path - the path to the photo
         */
        private void galleryAddPic(Uri path) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(path);
            MainActivity.this.sendBroadcast(mediaScanIntent);
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
            // TODO: This is redundant with MapsActivity onMapReady(), but we're not sure if
            // the service is ready before the map or if the map is ready before the service
            mapsActivity.setLocation(locationService.getUserLocation(), true);
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
