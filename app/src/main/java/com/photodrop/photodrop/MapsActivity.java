package com.photodrop.photodrop;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends SupportMapFragment implements OnMapReadyCallback {

    private GoogleMap mMap;

    // Firebase Objects
    private Firebase firebaseRef;
    private GeoFire geoFire;
    private static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";

    // The main activity so we can get the user's location
    private MainActivity mainActivity;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Firebase.setAndroidContext(getActivity());
        firebaseRef = new Firebase(FIREBASE_URL);
        geoFire = new GeoFire(new Firebase(FIREBASE_URL + "images"));

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(39.2551135,-76.7133843), 5);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
//                System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
                if (mMap != null) {
                    // Add a marker in UMBC and move the camera
                    LatLng drop = new LatLng(location.latitude, location.longitude);
                    mMap.addMarker(new MarkerOptions().position(drop).title(key));
                }
            }

            @Override
            public void onKeyExited(String key) {
//                System.out.println(String.format("Key %s is no longer in the search area", key));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
//                System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
            }

            /* Called when all previous data has been loaded */
            @Override
            public void onGeoQueryReady() {
//                System.out.println("All initial data has been loaded and events have been fired!");
            }

            @Override
            public void onGeoQueryError(FirebaseError error) {
                System.err.println("There was an error with this query: " + error);
            }
        });

/*
        firebaseRef.child("images").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                Log.d("Map", "Getting Images: " + snapshot);

                // If there's no photo yet, return
                if (snapshot.getValue() == null) {
                    return;
                }

                for (DataSnapshot drop : snapshot.getChildren()) {
                    Log.d("Map", "FOUND: " + drop.getKey());
                }
//                Log.d("ME", "Decoding image");
//                // Decodes the image
//                byte[] decodedImage = Base64.decode((String) snapshot.getValue(), Base64.DEFAULT);
//                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);
//
//                // Sets the image
//                imageView.setImageBitmap(bitmap);
            }

            @Override
            public void onCancelled(FirebaseError error) {
            }
        });
*/


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

        // Add a marker in UMBC and move the camera
//        LatLng umbc = new LatLng(39.2551135,-76.7133843);

        setLocation(mainActivity.getLocation());
//        mMap.addMarker(new MarkerOptions().position(umbc).title("Marker at UMBC"));
    }

    /**
     * The first callback method invoked when you start a fragment. Gets the main activity
     * so we can get location data from the MainActivity.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mainActivity = (MainActivity) context;
    }

    /**
     * Defines an interface that MainActivity implements so that we can
     * receive the location data from the MainActivity.
     */
    public interface LocationDataTransfer {
        public Location getLocation();
    }


    /**
     * Moves the map to the uer's current location. MainActivity calls this function if the user
     * wants to set the map location to their current location.
    */
    public void setLocation(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.addMarker(new MarkerOptions().position(userLatLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location)));

        // Zoom from 2 (furthest out) - 21 (closest zoom)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
    }
}
