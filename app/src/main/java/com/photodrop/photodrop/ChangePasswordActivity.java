package com.photodrop.photodrop;

import android.content.Context;
import android.graphics.Typeface;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

public class ChangePasswordActivity extends AppCompatActivity{

    // Firebase Objects
    public static final String FIREBASE_URL = "https://photodrop-umbc.firebaseio.com/";


    //UI
    private Button change;
    private EditText oldpw, newpw, confpw;
    private Firebase ref;
    private Firebase.AuthResultHandler authResultHandler;
    private String email;
    private AuthData authData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        oldpw = (EditText) findViewById(R.id.editTextOLD);
        newpw = (EditText) findViewById(R.id.editTextNEW);
        confpw = (EditText) findViewById(R.id.editTextCONFIRM);

        Typeface font = Typeface.createFromAsset(this.getAssets(), "fonts/ValterStd-Thin.ttf");
        oldpw.setTypeface(font);
        newpw.setTypeface(font);
        confpw.setTypeface(font);
        ((TextInputLayout)oldpw.getParent()).setTypeface(font);
        ((TextInputLayout)newpw.getParent()).setTypeface(font);
        ((TextInputLayout)confpw.getParent()).setTypeface(font);

        change = (Button) findViewById(R.id.buttonCHANGE);
        change.setTypeface(font);

        confpw.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.editTextCONFIRM || id == EditorInfo.IME_ACTION_DONE) {
                    changePassword();
                    return true;
                }
                return false;
            }
        });

        ref = new Firebase(FIREBASE_URL);
        authData = ref.getAuth();
        email = (String) authData.getProviderData().get("email");
        Log.d("ChangePWD:", email);

        change.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                changePassword();
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

    private void changePassword() {
        String newPwd = newpw.getText().toString();
        if (!newPwd.equals(confpw.getText().toString()) ) {
            // Clears the new password fields
            newpw.setText("");
            confpw.setText("");

            // Shows the error
            newpw.setError("New passwords don't match");
            newpw.requestFocus();

            return;
        }

        String currentPwd = String.valueOf(oldpw.getText());
        Log.d("ChangePWD:", currentPwd);
        ref.changePassword(email, currentPwd, newPwd, new Firebase.ResultHandler() {

            @Override
            public void onSuccess() {
                Log.d("ChangePWD:", "success");
                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, "Password changed!", Toast.LENGTH_SHORT);
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

                        // Clears the password and shows the error
                        oldpw.setText("");
                        oldpw.setError(firebaseError.getMessage());
                        oldpw.requestFocus();

                        break;
                    default:
                        Log.d("ChangePWD:", firebaseError.getMessage());

                        // Clears the new password fields
                        newpw.setText("");
                        confpw.setText("");

                        // Shows the error
                        newpw.setError(firebaseError.getMessage());
                        newpw.requestFocus();
                }
            }
        });
    }
}