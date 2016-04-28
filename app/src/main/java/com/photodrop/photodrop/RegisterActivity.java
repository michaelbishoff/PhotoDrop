package com.photodrop.photodrop;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    UserAuth userAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userAuth = new UserAuth();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    public void createUser(View v)
    {
        AutoCompleteTextView emailEditText = (AutoCompleteTextView)findViewById(R.id.email);
        EditText passwordEditText = (EditText) findViewById(R.id.password);
        EditText retypePassEditText = (EditText)findViewById(R.id.confirmpassword);

        //check if the passwords match
        if(passwordEditText == retypePassEditText) {
            userAuth.createUser(emailEditText.getText().toString(), passwordEditText.getText().toString(), new Firebase.ValueResultHandler<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> stringObjectMap) {

                    Intent goToLogin = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(goToLogin);
                }

                @Override
                public void onError(FirebaseError firebaseError) {

                }
            });
        }

    }


}
