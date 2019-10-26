package com.pg.facedetect.face;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by pghez on 2019/10/26.
 */

public class RecycleBitmap {

    /**
     * 回收ImageView占用的图像内存;
     * @param view
     */
    public static void recycleImageView(View view){
        if(view==null) return;
        if(view instanceof ImageView){
            Drawable drawable=((ImageView) view).getDrawable();
            if(drawable instanceof BitmapDrawable){
                Bitmap bmp = ((BitmapDrawable)drawable).getBitmap();
                if (bmp != null && !bmp.isRecycled()){
                    ((ImageView) view).setImageBitmap(null);
                    bmp.recycle();
                    bmp=null;
                }
            }
        }
    }
}
