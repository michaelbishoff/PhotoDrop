package com.photodrop.photodrop;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.firebase.client.Firebase;

/**
 * Created by michaelbishoff on 5/8/16.
 */
public class SharedPrefUtil {

    public static void saveUserID(Context context, String uid) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("UID", uid);
        editor.commit();
    }

    public static String getUserID(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String userID = preferences.getString("UID", null);

        System.out.println("UID------------------" + userID);

        return userID;
    }

    public static void saveCurrentUsersLike(Context context, String imageKey, Boolean liked){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(imageKey, liked);
        editor.commit();
    }

    public static Boolean getCurrentUsersLike(Context context, String imageKey){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean liked = preferences.getBoolean(imageKey,false);
        return liked;
    }
}
