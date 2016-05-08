package com.photodrop.photodrop;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

public class ChangePasswordActivity extends AppCompatActivity{

    // Firebase Objects
    public static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";


    //UI
    Button change;
    EditText oldpw, newpw, confpw;
    Firebase ref;
    Firebase.AuthResultHandler authResultHandler;
    String email;
    AuthData authData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);


        newpw = (EditText) findViewById(R.id.editTextNEW);
        oldpw = (EditText) findViewById(R.id.editTextOLD);
        confpw = (EditText) findViewById(R.id.editTextCONFIRM);

        change = (Button) findViewById(R.id.buttonCHANGE);

        ref = new Firebase(FIREBASE_URL);
        authData = ref.getAuth();
        email = (String) authData.getProviderData().get("email");
        Log.d("ChangePWD:", email);

        change.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String newPwd = newpw.getText().toString();
                if ( !newPwd.equals(confpw.getText().toString()) ) {
                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, "New passwords don't match.", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }

                String currentPwd = String.valueOf(oldpw.getText());
                Log.d("ChangePWD:", currentPwd);
                ref.changePassword(email, currentPwd, newPwd, new Firebase.ResultHandler() {

                    @Override
                    public void onSuccess() {
                        Log.d("ChangePWD:", "success");
                        Context context = getApplicationContext();
                        Toast toast = Toast.makeText(context, "Password changed succeeded!", Toast.LENGTH_SHORT);
                        toast.show();
                        finish();
                    }

                    @Override
                    public void onError(FirebaseError firebaseError) {
                        Context context;
                        Toast toast;
                        switch (firebaseError.getCode()) {
                            case FirebaseError.INVALID_PASSWORD:
                                Log.d("ChangePWD:", "INVALID_PASSWORD");
                                context = getApplicationContext();
                                toast = Toast.makeText(context, "The specified user account password is incorrect.", Toast.LENGTH_SHORT);
                                toast.show();
                                break;
                            default:
                                Log.d("ChangePWD:", firebaseError.getMessage());
                                context = getApplicationContext();
                                toast = Toast.makeText(context, firebaseError.getMessage(), Toast.LENGTH_SHORT);
                                toast.show();
                        }
                    }
                });
            }
        });
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