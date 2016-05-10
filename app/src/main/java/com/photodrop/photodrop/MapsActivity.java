package com.photodrop.photodrop;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

// TODO: Could make an object for the marker and the ID so that we can use the title and the snippet,
// but then still need a way to check if the marker they clicked was in range or not (in constant time) in onMarkerClick()

// TODO: Should store the user's location and the locations of the nearby drops in the SQLite
// database. Then when the app is reopened or resumed, we can check if the most recent location
// isn't significantly od or if it matches the current location and load the SQLite data rather than calling Firebase again

public class MapsActivity extends SupportMapFragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // Map Objects
    private GoogleMap mMap;
    private Circle queryRadius;
    private Circle maxQueryRadius;
    private HashMap<String, Marker> viewableDrops;
    private HashMap<String, Marker> notViewableDrops;
    private LatLng userLatLng;
    private static final int CAMERA_ZOOM = 16; // Zoom from 2 (furthest out) - 21 (closest zoom)

    // Firebase Objects
    private GeoFire geoFire;
    private GeoQuery geoQuery;
    private GeoQuery closeRangeGeoQuery;
    private boolean nearImagesAdded = false;
    private static final double QUERY_RADIUS = 0.2;
    private static final double MAX_QUERY_RADIUS = 25;

    // The main activity so we can get the user's location
    private MainActivity mainActivity;

    // Key used to pass the image key as a value to the ImageActivity
    public static final String IMAGE_KEY = "IMAGE_KEY";

    // Parachute icons
    public static BitmapDescriptor parachuteGrey;
    public static BitmapDescriptor parachuteBlue;
    public static BitmapDescriptor parachuteRed;
    public static BitmapDescriptor parachuteOrange;
    public static BitmapDescriptor parachuteYellow;

//    public static final int PARACHUTE_WIDTH = 104;
//    public static final int PARACHUTE_HEIGHT = 120;

    public static final int PARACHUTE_WIDTH = 130;
    public static final int PARACHUTE_HEIGHT = 150;

    public static final int ONE_MINUTE = 60000;


    /**
     * The first callback method invoked when you start a fragment. Gets the main activity
     * so we can get location data from the MainActivity.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mainActivity = (MainActivity) context;
        Log.d("ME", "onAttach() mainActivity == " + mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

        // Initializes the parachute objects
//        parachuteGrey = createBitmapDescriptor(R.drawable.parachute_grey);
        parachuteBlue = createBitmapDescriptor(R.drawable.parachute_blue);
        parachuteRed = createBitmapDescriptor(R.drawable.parachute_red);
//        parachuteOrange = createBitmapDescriptor(R.drawable.parachute_orange);
//        parachuteYellow = createBitmapDescriptor(R.drawable.parachute_yellow);


        mapFragment.getMapAsync(this);

        // Initializes the Firebase references
        Firebase.setAndroidContext(getActivity());
        geoFire = new GeoFire(new Firebase(MainActivity.GEOFIRE_URL));

//        geoFire = mainActivity.getGeoFire();
        // ^^ Want to do this because it saves memory on one object! But there's a RuntimeException
        // because it says mainActivity is null even though onCreateView() is called after onAttach()
        // But then it introduces a race condition if the map is ready before MainActivity.onCreate() finishes

        // Initializes the HashTable of keys to markers
        viewableDrops = new HashMap<>();
        notViewableDrops = new HashMap<>();

        // TODO: Remove this
        // This will most likely never be run because userLocationMarker is initialized
        // in an async thread right before this if statement
//        if (userLocationMarker != null) {
//            geoQuery = geoFire.queryAtLocation(new GeoLocation(userLocationMarker.getPosition().latitude,
//                    userLocationMarker.getPosition().longitude), 5);
//        }

        Log.d("ME-MapsActivity-DO", "Done onCreateView(): " + mainActivity);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();

        // TODO: Maybe unregister the geoFire listeners and the map, but then re-registering them
        // will load the markers again and reload the map
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Removes the navigation button from the map view
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Sets the handler for when the markers are clicked on
        mMap.setOnMarkerClickListener(this);

        // Adding the user's location to the map requires this check for permissions
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
            return;
        }

        // Adds the user location to the map
        mMap.setMyLocationEnabled(true);

        // TODO: Should setup a LocationSource so that the LocationService is used for
        // both the user's location and the circles
//        mMap.setLocationSource(LocationSource);

        // TODO: Remove this and make the location service give the first location it finds
        // But that will cause there to be an extra object in the location services class
        // Sets the location to be the user's location, but it's not available right away because
        // location services takes a while to start, so this is always: setLocation(null)
//        setLocation(mainActivity.getLocation(), true);

        startLocationUpdates();
    }

    /**
     * Callback triggered when a map marker is clicked. Sets the image view to be the image
     * that the user clicked on
     * @return false - centers the map on the marker and opens the marker's title
     *         true - we handled the click event. Doesn't show the title or anything
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d("ME", "Clicked on: " + marker.getTitle());

        // If the user clicked the user icon or a drop that is in range, then open the photo
        if (!notViewableDrops.containsKey(marker.getTitle())) {
            Intent openImageIntent = new Intent(getActivity(), ImageActivity.class);
            openImageIntent.putExtra(IMAGE_KEY, marker.getTitle());
            startActivity(openImageIntent);
        }

        // TODO: How should we indicate the marker that the user just clicked on or has previously clicked on

        return true;
    }

    /**
     * Moves the map to the user's current location. MainActivity calls this function if the user
     * wants to set the map location to their current location.
     *
     * Note: LocationService uses Location
     *       Google uses LatLng
     *       GeoFire uses GeoLocation
    */
    public void setLocation(Location location, boolean moveCamera) {
        if (location == null) {
            userLatLng = new LatLng(0, 0);
        } else {
            userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        }

        Log.d("ME", "Setting User Location & Query Center");


        // Initializes the GeoQuery or moves the query location
        if (geoQuery == null) {
            // Initializes the query area
            geoQuery = geoFire.queryAtLocation(new GeoLocation(userLatLng.latitude, userLatLng.longitude), MAX_QUERY_RADIUS);

            // Adds the query event listener
            geoQuery.addGeoQueryEventListener(new MaxRangeListener());

            // If the user hasn't moved, don't change the geoQuery center
        } else if (geoQuery.getCenter().latitude != userLatLng.latitude
                || geoQuery.getCenter().longitude != userLatLng.longitude) {

            // Moves the center of the query
            GeoLocation geoLocation = new GeoLocation(userLatLng.latitude, userLatLng.longitude);
            geoQuery.setCenter(geoLocation);

            if (closeRangeGeoQuery != null) {
                closeRangeGeoQuery.setCenter(geoLocation);
            }
        }


        // Initializes the user marker and query radius
        if (queryRadius == null) {

            // Adds a circle that indicates the query radius in meters for viewable photos
            queryRadius = mMap.addCircle(new CircleOptions()
                    .center(userLatLng)
                    .radius(QUERY_RADIUS * 1000)
                    .strokeColor(Color.BLUE));

            // Adds a circle that indicates the max query radius in meters for all
            // photos, including photos you can not view
            maxQueryRadius = mMap.addCircle(new CircleOptions()
                    .center(userLatLng)
                    .radius(MAX_QUERY_RADIUS * 1000)
                    .strokeColor(Color.GRAY));
        } else {

            // Moves the query radius to the user's location
            queryRadius.setCenter(userLatLng);
            maxQueryRadius.setCenter(userLatLng);
        }

        if (moveCamera) {
            // Zoom from 2 (furthest out) - 21 (closest zoom)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, CAMERA_ZOOM));
        }
    }

    /**
     * Initializes the timer to continually get the user's location and update the UI elements
     */
    private void startLocationUpdates() {
        // Gets the user's activity every 2 minutes
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask asyncTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mainActivity != null) {
                            // Get the current location
                            Location location = mainActivity.getLocation();

                            // Set the location on the UI if the location has changed
                            if (location != null && (location.getLatitude() != userLatLng.latitude
                                || location.getLongitude() != userLatLng.longitude)) {

                                // Set the location of the user on the map
                                // and update the query radii
                                setLocation(mainActivity.getLocation(), false);
                            }
                        }
                    }
                });
            }
        };
        // Get the user's location every minute
        timer.schedule(asyncTask, 0, ONE_MINUTE);
    }

    /* Google Maps Marker Methods */

    /**
     * Creates a BitmapDescriptor for the specified drawable. The drawable is a parachute icon that
     * is on the map, so they all have the same size.
     * @param drawableId - The ID of the drawable in the R file
     * @return a BitmapDescriptor
     */
    private BitmapDescriptor createBitmapDescriptor(int drawableId) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) getResources().getDrawable(drawableId, null);
        Bitmap bitmap = bitmapDrawable.getBitmap();
        bitmap = Bitmap.createScaledBitmap(bitmap, PARACHUTE_WIDTH, PARACHUTE_HEIGHT, false);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Toggles a marker from the viewable range to the not-viewable range and vice versa
     * @param key
     * @param thisHashMap
     * @param thatHashMap
     * @param bitmap
     */
    public void toggleMarkerViewable(String key, HashMap<String, Marker> thisHashMap,
                                     HashMap<String, Marker> thatHashMap, BitmapDescriptor bitmap) {

        // Remove the marker from the one hash table
        Marker marker = thisHashMap.get(key);
        thisHashMap.remove(key);

        // Change the color and add it to the other hash table
        marker.setIcon(bitmap);
        thatHashMap.put(key, marker);
    }


    /**
     * Defines an interface that MainActivity implements so that we can
     * receive the location data from the MainActivity.
     */
    public interface LocationDataTransfer {
        Location getLocation(); // Gets the user's current location
        GeoFire getGeoFire(); // Gets the GeoFire reference to save memory
    }


    public class MaxRangeListener implements GeoQueryEventListener {

        /**
         * Called for each result that is in the search area and when
         * a key enters the search area
         */
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            Log.d("ME", "KEY OUT of Range: " + key);
            synchronized (viewableDrops) {
                synchronized (notViewableDrops) {
                    if (mMap != null && !viewableDrops.containsKey(key)) {
                        Log.d("ME", "Dropping image out of view range");

                        // Creates a LatLng object from the location
                        LatLng drop = new LatLng(location.latitude, location.longitude);

                        // TODO: Resize the photo for the screen size (maybe do it this way, or
                        // just do it the regular way and have multiple versions of the same file
                        // or maybe we don't have to do it?)

                        // Adds the marker to the map
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(drop)
                                .title(key)
                                .icon(parachuteBlue));

                        // If a new photo was added, show the animation
                        if (nearImagesAdded) {
                            dropAnimation(drop, marker);
                        }

                        // Adds the key and the marker on the map to the hash table of not viewable drops
                        notViewableDrops.put(key, marker);
                        Log.d("ME", "Drop Key: " + key);
                    }
                }
            }
        }

        /**
         *  Called when a key has moved out of the search area (should never be called)
         *  Note: GeoFire makes onChildRemoved() and onChildChanged() call onKeyExited()
         */
        @Override
        public void onKeyExited(String key) {
            // Removes the corresponding marker from the map

            Log.d("ME", "Removing key: " + key);

            // If it was viewable and exited, then the entry was deleted
            if (viewableDrops.containsKey(key)) {
                // Removes the marker from the map
                viewableDrops.get(key).remove();
                // Removes the marker key pair from the hash table
                viewableDrops.remove(key);

            } else {
                // If it was not viewable, then it's just out of the MAX_QUERY_RADIUS

                // Removes the marker from the map
                notViewableDrops.get(key).remove();
                // Removes the marker key pair from the hash table
                notViewableDrops.remove(key);
            }
        }

        /* Called when a key has moved within the search area (should never be called) */
        @Override
        public void onKeyMoved(String key, GeoLocation location) { }

        /**
         * Called when all previous data has been loaded
         * (when all markers in the DB have been placed)
         */
        @Override
        public void onGeoQueryReady() {
            Log.d("ME", "GeoQueryReady() for MAX Range");

            // Only want to run this once, but need to run it after
            // MaxRangeListener executes onGeoQueryReady()
            // nearImagesAdded ensures we run it once. notViewableDrops.size() ensures that we run
            // it after the markers have been added, rather than once the listener has been initialized
            if (!nearImagesAdded && notViewableDrops.size() > 0) {
                Log.d("ME", "Adding close range query");
                // Initializes the query area
                closeRangeGeoQuery = geoFire.queryAtLocation(new GeoLocation(userLatLng.latitude, userLatLng.longitude), QUERY_RADIUS);
                closeRangeGeoQuery.addGeoQueryEventListener(new CloseRangeListener());

                // Note: Having two geoQueries is a race condition since I don't think it's
                // guaranteed which listener will trigger first. Not sure how to fix this because
                // they are event listeners and synchronizing the HashTables with the markers won't
                // the race condition result. It doesn't matter for onKeyExited() because the key
                // will be deleted either way (if was actually deleted from the DB or if the user
                // walked out of the MAX_QUERY_RANGE from that point), but it does for
                // onKeyEntered() because the key could show up as un-clickable.
                // Firebase or GeoFire may do them in order because I haven't had a problem yet (onKeyEntered() twice then onGeoQueryRead() twice)

                nearImagesAdded = true;
            }
        }

        @Override
        public void onGeoQueryError(FirebaseError error) {
            System.err.println("There was an error with this query: " + error);
        }
    }

    /**
     * Precondition: This listener is added once the MaxRangeListener has finished loading all of its markers
     *
     * Could just use the MaxRangeListener and use math to determine the close ones, but would
     * have to do that for every marker
     */
    public class CloseRangeListener implements GeoQueryEventListener {

        /**
         * Called for each result that is in the search area and when
         * a key enters the search area
         */
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            Log.d("ME", "KEY in Range: " + key);

            synchronized (viewableDrops) {
                synchronized (notViewableDrops) {

                    // If the drop was not in range, make it viewable
                    // Redundancy check since the MaxRangeListener is called first
                    if (notViewableDrops.containsKey(key)) {
                        Log.d("ME", "Toggling drop ON");
                        toggleMarkerViewable(key, notViewableDrops, viewableDrops, parachuteRed);
//                                BitmapDescriptorFactory.defaultMarker());
                        return;

                        // CloseRangeListener onKeyEntered() called before MaxRangeListener onKeyEntered()
                    } else {

                        // Creates a LatLng object from the location
                        LatLng drop = new LatLng(location.latitude, location.longitude);

                        // Adds the marker to the map
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(drop)
                                .title(key)
                                .icon(parachuteRed));

                        // If a new photo was added, show the animation
                        if (nearImagesAdded) {
                            dropAnimation(drop, marker);
                        }

                        // Adds the key and the marker on the map to the hash table of viewable drops
                        viewableDrops.put(key, marker);

//                throw new RuntimeException("Marker for close range dropping before Max range");
                    }
                }
            }
        }

        /**
         *  Called when a key has moved out of the search area (should never be called)
         *  Note: GeoFire makes onChildRemoved() and onChildChanged() call onKeyExited()
         */
        @Override
        public void onKeyExited(String key) {
            // It was viewable, so make it not viewable

            Log.d("ME", "Key no longer Viewable: " + key);

            // Redundancy check since if it's exiting the search area, then it
            // must already be in viewableDrops
            if (viewableDrops.containsKey(key)) {
                Log.d("ME", "Toggling drop OFF");
                toggleMarkerViewable(key, viewableDrops, notViewableDrops, parachuteBlue);
//                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            }
        }

        /* Called when a key has moved within the search area (should never be called) */
        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        /**
         * Called when all previous data has been loaded
         * (when all markers in the DB have been placed)
         */
        @Override
        public void onGeoQueryReady() {
            Log.d("ME", "GeoQueryReady() for CLOSE Range");
        }

        @Override
        public void onGeoQueryError(FirebaseError error) {
            System.err.println("There was an error with this query: " + error);
        }
    }

    private void dropAnimation(final LatLng target, final Marker marker) {
        final long duration = 1000;
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();

        Point startPoint = proj.toScreenLocation(target);
        startPoint.y = 0;
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);

        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * target.longitude + (1 - t) * startLatLng.longitude;
                double lat = t * target.latitude + (1 - t) * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                if (t < 1.0) {
                    // Post again 10ms later.
                    handler.postDelayed(this, 10);
                } else {
                    // animation ended
                }
            }
        });
    }
}
