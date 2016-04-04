package com.photodrop.photodrop;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by michaelbishoff on 3/29/16.
 */
public class ImageUtil {

    // TODO: May want to make these AsyncTasks

    /**
     * Gets the Bitmap of the image at the specified URI
     */
    public static Bitmap getBitmapFromUri(Context context, Uri imageUri) {

        try {
            // Gets the image bitmap. The ContentResolver is the instance of the app
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);

            // Gets the orientation of the photo
            String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
            Cursor cur = context.getContentResolver().query(imageUri, orientationColumn, null, null, null);
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
                return rotateImage(bitmap, orientation);
            }
        } catch (IOException e) {
            Log.e("MainActivity", "BITMAP ERROR: " + e.toString());
        }

        return null;
    }

    /**
     * Rotates the image at the specified angle
     */
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap result = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        source.recycle();
        source = null;

        return result;
    }

    /**
     * Sets the image view with the encoded image that is passed in
     */
    public static Bitmap getBitmapFromEncodedImage(String encodedImage) {
        Bitmap bitmap = null;

        if (encodedImage != null) {
            // Decodes the image
            byte[] decodedImage = Base64.decode(encodedImage, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);
        }

        return bitmap;
    }

    /**
     * Encodes the image bitmap to a String at the specified quality 0-100
     * @param bitmap - The image bitmap
     * @param imageQuality - A value from 0-100 that specifies the image quality
     *                       0 meaning compress for small size, 100 meaning compress for max
     *                       quality. Some formats, like PNG which is lossless, will ignore
     *                       the quality setting
     */
    public static String encodeBitmap(Bitmap bitmap, int imageQuality) {
        // Converts the image to an encoded String
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // PNG is lossless quality but 100 ensures max quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, byteArrayOutputStream);
        bitmap.recycle();
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // Frees up some memory (i think lol)
        byteArrayOutputStream = null;

        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

}
