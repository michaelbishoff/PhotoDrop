package com.photodrop.photodrop;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends SupportMapFragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // Map Objects
    private GoogleMap mMap;
    private Marker userLocationMarker;
    private Circle queryRadius;

    // Firebase Objects
    private GeoFire geoFire;
    private GeoQuery geoQuery;
    private static final double QUERY_RADIUS = 0.2;
    private static final double MAX_QUERY_RADIUS = 5;
    private static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";
    private static final String GEOFIRE_URL = FIREBASE_URL + "drops";

    // The main activity so we can get the user's location
    private MainActivity mainActivity;


    /**
     * The first callback method invoked when you start a fragment. Gets the main activity
     * so we can get location data from the MainActivity.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mainActivity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initializes the Firebase references
        Firebase.setAndroidContext(getActivity());
        geoFire = new GeoFire(new Firebase(GEOFIRE_URL));
//        geoFire = mainActivity.getGeoFire();
        // ^^ Want to do this because it saves memory on one object! But there's a RuntimeException
        // because it says mainActivity is null even though onCreateView() is called after onAttach()


        // TODO: Remove this
        // This will most likely never be run because userLocationMarker is initialized
        // in an async thread right before this if statement
//        if (userLocationMarker != null) {
//            geoQuery = geoFire.queryAtLocation(new GeoLocation(userLocationMarker.getPosition().latitude,
//                    userLocationMarker.getPosition().longitude), 5);
//        }

        // Initializes the query area
        geoQuery = geoFire.queryAtLocation(new GeoLocation(0, 0), QUERY_RADIUS);

        // Adds the query event listener
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            /**
             * Called for each result that is in the serach area and when
             * a new key enters the search area
             */
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
//                System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
                if (mMap != null) {
                    // Add a marker in UMBC and move the camera
                    LatLng drop = new LatLng(location.latitude, location.longitude);
                    mMap.addMarker(new MarkerOptions().position(drop).title(key));
                    Log.d("ME", "Drop Key: " + key);
                }
            }

            /* Called when a key is moved out of the search area (should never be called) */
            @Override
            public void onKeyExited(String key) {
//                System.out.println(String.format("Key %s is no longer in the search area", key));
            }

            /* Called when a key is moved within the search area (should never be called) */
            @Override
            public void onKeyMoved(String key, GeoLocation location) {
//                System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
            }

            /**
             * Called when all previous data has been loaded
             * (when all markers in the DB have been placed)
             */
            @Override
            public void onGeoQueryReady() {
//                System.out.println("All initial data has been loaded and events have been fired!");
                Log.d("ME", "Done loading drops!");
            }

            @Override
            public void onGeoQueryError(FirebaseError error) {
                System.err.println("There was an error with this query: " + error);
            }
        });

        return view;
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

        // TODO: Remove this and make the location service give the first location it finds
        // But that will cause there to be an extra object in the location services class
        // Sets the location to be the user's location, but it's not available right away because
        // location services takes a while to start, so this is always: setLocation(null)
        setLocation(mainActivity.getLocation());
    }

    /**
     * Callback triggered when a map marker is clicked. Sets the image view to be the image
     * that the user clicked on
     * @return false - centers the map on the marker and opens the marker's title
     *         true - does nothing
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d("ME", "Clicked on: " + marker.getTitle());

        // If the user clicked the user icon, then do nothing
        if (marker.equals(userLocationMarker)) {
            return true;
        }

        // Sets the image in the corner to be the image the user clicked on
        mainActivity.setImage(marker.getTitle());

        // return false moves centers the map on the marker
        // return true doesn't show the title or anything
        return false;
    }

    /**
     * Moves the map to the user's current location. MainActivity calls this function if the user
     * wants to set the map location to their current location.
    */
    public void setLocation(Location location) {
        LatLng userLatLng;
        if (location == null) {
            userLatLng = new LatLng(0, 0);
        } else {
            userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        }

        // Initializes the user marker and query radius
        if (userLocationMarker == null) {
            // Adds the user location to the map and offsets the image so it looks correct
            userLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(userLatLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))
                    .anchor(0.5f, 0.5f));

            // Adds a circle that indicates the query radius in meters
            queryRadius = mMap.addCircle(new CircleOptions()
                    .center(userLatLng)
                    .radius(QUERY_RADIUS * 1000)
                    .strokeColor(Color.BLUE));
        }

        // Moves the user's location marker
        userLocationMarker.setPosition(userLatLng);

        // Moves the query radius to the user's location
        queryRadius.setCenter(userLatLng);

        // Moves the center of the query
        geoQuery.setCenter(new GeoLocation(userLatLng.latitude, userLatLng.longitude));
        Log.d("ME", "Setting User Location & Query Center");

        // Zoom from 2 (furthest out) - 21 (closest zoom)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
    }

    /**
     * Defines an interface that MainActivity implements so that we can
     * receive the location data from the MainActivity.
     */
    public interface LocationDataTransfer {
        public Location getLocation(); // Gets the user's current location
        public void setImage(String key); // Sets the image view with the image with the corresponding key in Firebase
        public GeoFire getGeoFire(); // Gets the GeoFire reference to save memory
    }
}
