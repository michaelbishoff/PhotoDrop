package com.photodrop.photodrop;

/**
 * Created by yuan316 on 4/12/16.
 */
import android.app.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.widget.*;
import java.util.*;
import android.graphics.*;
import android.view.*;
import android.content.*;

public class ImageAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Bitmap> bitmapList;
    public static final int CROP_WIDTH = 220;
    public static final int CROP_HEIGHT = CROP_WIDTH;

    private LayoutInflater inflater;

    public ImageAdapter(Context context, ArrayList<Bitmap> bitmapList) {
        this.context = context;
        this.bitmapList = bitmapList;
        this.inflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return this.bitmapList.size();
    }

    public Object getItem(int position) {
        return bitmapList.get(position);
    }

    public long getItemId(int position) {
        return bitmapList.get(position).getGenerationId();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView photo;

        // If the view isn't created, create it with the custom gridview_item and set the photo tag
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.gridview_item, parent, false);
            convertView.setTag(R.id.photo, convertView.findViewById(R.id.photo));
        }

        photo = (ImageView) convertView.getTag(R.id.photo);
//        photo = (ImageView) convertView.findViewById(R.id.photo);

        // Gets the bitmap from the list
        Bitmap bitmap = (Bitmap) getItem(position);
        // Sets the photo's bitmap
        photo.setImageBitmap(bitmap);

        return convertView;

/*
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float dd = dm.density;  //取出密度 get density of this device
        float px = 25 * dd;  //像素 = dp * 密度 pixel = dp * density
        float screenWidth = dm.widthPixels;  //取出螢幕寬度 get the breadth of this device
        int newWidth = (int) (screenWidth - px) / 3; // 一行顯示四個縮圖 show 3 items per row
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(this.context);
            // Crops the photo
//            imageView.setLayoutParams(new GridView.LayoutParams(newWidth, newWidth));
//            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
//            imageView.setLayoutParams(new GridView.LayoutParams(CROP_WIDTH, CROP_HEIGHT));
//            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageBitmap(this.bitmapList.get(position));
        return imageView;
        */
    }
}
