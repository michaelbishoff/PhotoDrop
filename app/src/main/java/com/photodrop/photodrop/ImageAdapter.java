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


    public ImageAdapter(Context context, ArrayList<Bitmap> bitmapList) {
        this.context = context;
        this.bitmapList = bitmapList;
    }

    public int getCount() {
        return this.bitmapList.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float dd = dm.density;  //取出密度
        float px = 25 * dd;  //像素 = dp * 密度
        float screenWidth = dm.widthPixels;  //取出螢幕寬度
        int newWidth = (int) (screenWidth - px) / 4; // 一行顯示四個縮圖
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(this.context);
            // Crops the photo
            imageView.setLayoutParams(new GridView.LayoutParams(newWidth, newWidth));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            //imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageBitmap(this.bitmapList.get(position));
        return imageView;
    }
}
