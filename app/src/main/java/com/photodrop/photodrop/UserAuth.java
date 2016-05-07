package com.photodrop.photodrop;

import android.util.Log;

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


    public void createUser(String email, String password, final Firebase.ValueResultHandler<Map<String, Object>> valueResultHandler){

        ref.createUser(email, password, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                System.out.println("Successfully created user account with uid: " + result.get("uid"));
                valueResultHandler.onSuccess(result);
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                // there was an error
                valueResultHandler.onError(firebaseError);
            }
        });
    }


    public boolean isLoggedIn(){
        AuthData authData = ref.getAuth();
        if(authData != null) {
            Log.d("Sandy", "uid = " + ref.getAuth().getUid());
            return true;
        }
        else
            return false;
    }

    public void signIn(String email, String password, Firebase.AuthResultHandler authResultHandler){

        ref.authWithPassword(email, password, authResultHandler);
    }


    public void logOut() {
        ref.unauth();
    }

    public String getUID()
    {
        AuthData authData = ref.getAuth();
        if(authData != null) {
            Log.d("Sandy", "uid = " + ref.getAuth().getUid());
            return authData.getUid();
        }
        else
            return null;

    }

}