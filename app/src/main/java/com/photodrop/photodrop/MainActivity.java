package com.photodrop.photodrop;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private FloatingActionButton fab;
    private ImageView imageView;
    private static final int START_CAMERA = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        imageView = (ImageView) findViewById(R.id.imageView);
    }


    // Adds the OnClickListener
    @Override
    protected void onResume() {
        super.onResume();
        fab.setOnClickListener(this);
    }

    // Removes the OnClickListener
    @Override
    protected void onPause() {
        super.onPause();
        fab.setOnClickListener(null);
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
}
