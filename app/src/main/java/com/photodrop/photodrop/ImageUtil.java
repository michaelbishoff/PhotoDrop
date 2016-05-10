package com.photodrop.photodrop;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
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

    // TODO: May want to (should probably) make these AsyncTasks

    /**
     * Gets the Bitmap of the image at the specified URI
     */
    public static Bitmap getBitmapFromUri(Context context, Uri imageUri) {

        try {
            ExifInterface exif = new ExifInterface(imageUri.getPath());
            Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath());

            return rotateImage(bitmap, exif.getAttribute(ExifInterface.TAG_ORIENTATION));

        } catch (IOException e) {
            e.printStackTrace();
        }

/*
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
*/
        return null;
    }

    /**
     * Given the exit orientation as a string, return the correctly rotated image
     */
    private static Bitmap rotateImage(Bitmap source, String exifOrientationString) {

        if (exifOrientationString != null) {
            float angle = 0;
            int exifOrientation = Integer.parseInt(exifOrientationString);

            // Determine the angle
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    angle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    angle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    angle = 270;
                    break;
                default:
                    return source;
            }

            return rotateImage(source, angle);

            // orientation is null, so return the source
        } else {
            return source;
        }
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
     * Encodes the file at the specified URI to a Base64 String
     * @param context - The context that the saved the file
     * @param uri - The absolute path to the image
     * @param imageQuality - The quality of the encoding (0-100)
     * @return a Base64 version of the specified image
     */
    public static String encodeFile(Context context, Uri uri, int imageQuality) {
        // Gets the bitmap of the image from the URI
        Bitmap bitmap = ImageUtil.getBitmapFromUri(context, uri);
        return encodeBitmap(bitmap, imageQuality);
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


    /**
     * calculate a sample size value that is a power of two based on a target width and height
     * http://developer.android.com/intl/zh-tw/training/displaying-bitmaps/load-bitmap.html#load-bitmap
     * @param options - BitmapFactory.Options
     * @param reqWidth - int
     * @param reqHeight - int
     */
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


    /**
     * calculate a sample size value that is a power of two based on a target width and height
     * http://developer.android.com/intl/zh-tw/training/displaying-bitmaps/load-bitmap.html#load-bitmap
     * @param encodedImage - encoded image
     * @param reqWidth - int
     * @param reqHeight - int
     */
    //public static Bitmap decodeSampledBitmap(Resources res, int resId,
    //                                                     int reqWidth, int reqHeight) {
    public static Bitmap decodeStringAndSampledBitmap(String encodedImage,
                                                     int reqWidth, int reqHeight) {

        Bitmap bitmap = null;

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        //BitmapFactory.decodeResource(res, resId, options);
        if (encodedImage != null) {
            // Decodes the image
            byte[] decodedImage = Base64.decode(encodedImage, Base64.DEFAULT);
            // Sets the width and height in options ??
            BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length, options);
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeByteArray(decodedImage, 0,
                                                    decodedImage.length, options);
        }
        return bitmap;
    }

    /**
     * Given the absolute path to an image, create a sampled version of the image as a Bitmap
     * @param path - The absolute path to the image
     * @param reqWidth - the width of the sampled image
     * @param reqHeight - the height of the sampled image
     * @return a sampled Bitmap of the image at the specified path
     */
    public static Bitmap decodeFileAndSampleBitmap(String path, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        Bitmap sampledBitmap = BitmapFactory.decodeFile(path, options);

        // Rotates the image
        try {
            ExifInterface exif = new ExifInterface(path);
            return rotateImage(sampledBitmap, exif.getAttribute(ExifInterface.TAG_ORIENTATION));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
