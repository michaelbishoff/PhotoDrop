package com.photodrop.photodrop;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.AuthData;

import java.util.Map;

/**
 * Created by siqilin on 4/9/16.
 */
public class UserAuth {

    Firebase ref;

    UserAuth(){

        ref = new Firebase("https://photodrop-umbc.firebaseio.com/");
    }


    public void createUser(String email, String password){

        ref.createUser("bobtony@firebase.com", "correcthorsebatterystaple", new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                System.out.println("Successfully created user account with uid: " + result.get("uid"));
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                // there was an error
            }
        });
    }


    public boolean isLoggedIn(){
        AuthData authData = ref.getAuth();
        if(authData != null)
            return true;
        else
            return false;
    }

    public void signIn(String email, String password, Firebase.AuthResultHandler authResultHandler){

        ref.authWithPassword(email, password, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                System.out.println("User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                // there was an error
            }
        });
    }


    public void logOut() {
        ref.unauth();
    }

}
