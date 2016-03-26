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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MapsActivity.LocationDataTransfer {

    // UI Buttons
    private FloatingActionButton fab;
    private ImageView imageView;
    private ImageButton profileButton;
    private static final int START_CAMERA = 1000;

    // Firebase Objects
    private Firebase firebaseRef;
    private Firebase images;
    private GeoFire geoFire;
    private static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";

    // Service Objects
    private LocationService.LocationServiceBinder binder;
    private LocationService locationService;
    private boolean connected = false;

    // The Map Fragment
    private MapsActivity mapsActivity;
    private FloatingActionButton compassButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        imageView = (ImageView) findViewById(R.id.imageView);
        profileButton = (ImageButton) findViewById(R.id.profileButton);
        compassButton = (FloatingActionButton) findViewById(R.id.compassButton);

        // Gets the Map Fragment so we can call setLocation()
        mapsActivity = (MapsActivity) getSupportFragmentManager().findFragmentById(R.id.map);

        Firebase.setAndroidContext(this);
        firebaseRef = new Firebase(FIREBASE_URL);
        images = new Firebase(FIREBASE_URL + "images");
        geoFire = new GeoFire(images);

        firebaseRef.child("image").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                // If there's no photo yet, return
                if (snapshot.getValue() == null) {
                    return;
                }
                Log.d("ME", "Decoding image");
                // Decodes the image
                byte[] decodedImage = Base64.decode((String) snapshot.getValue(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);

                // Sets the image
                imageView.setImageBitmap(bitmap);
            }

            @Override
            public void onCancelled(FirebaseError error) {
            }
        });

        // Binds the service. Calls the onServiceConnected() method below
        Intent serviceIntent = new Intent(this, LocationService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }


    // Adds the OnClickListener
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

    // Removes the OnClickListener
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
                mapsActivity.setLocation(locationService.getUserLocation());
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_CAMERA) {
            if (resultCode == RESULT_OK) {
                try {
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

    public class SaveImage implements Runnable {
        private Bitmap bitmap;
        public SaveImage(Bitmap bitmap) {
            this.bitmap = bitmap.copy(bitmap.getConfig(), true);
        }

        @Override
        public void run() {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // PNG is lossless quality but 100 ensures max quality
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            bitmap.recycle();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String imageFile = Base64.encodeToString(byteArray, Base64.DEFAULT);

            // Getting the user's location
            Location location = locationService.getUserLocation();

            Log.d("ME", String.format("Saving image at:\nlat: %.10f\nlng: %.10f",
                    location.getLatitude(), location.getLongitude()));
//            firebaseRef.child("images/firebase-hq" + location.getLatitude() + "," + location.getLongitude()).setValue(imageFile);

            geoFire.setLocation(images.push().getKey(), new GeoLocation(location.getLatitude(), location.getLongitude()));
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
            return new Location("");
        }
//        return locationService.getUserLocation();
    }
}
