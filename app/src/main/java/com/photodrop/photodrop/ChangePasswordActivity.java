package com.photodrop.photodrop;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

public class ChangePasswordActivity extends AppCompatActivity {

    // Firebase Objects
    public static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";


    //UI
    Button change;
    EditText oldpw, newpw, confpw;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);


        newpw = (EditText) findViewById(R.id.editTextNEW);
        oldpw = (EditText) findViewById(R.id.editTextOLD);
        confpw = (EditText) findViewById(R.id.editTextCONFIRM);

        change = (Button) findViewById(R.id.buttonCHANGE);

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
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}