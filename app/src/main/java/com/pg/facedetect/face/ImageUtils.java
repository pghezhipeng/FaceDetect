package com.pg.facedetect.face;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {

    //截取rect内的bitmap
    public static Bitmap cropBitmap(Bitmap bitmap, Rect rect, boolean recycle) {
        return cropBitmap(bitmap,rect,bitmap.getConfig(),recycle);
    }

    public static Bitmap cropBitmap(Bitmap bitmap, Rect rect, Bitmap.Config config, boolean recycle) {
        Bitmap ret = Bitmap.createBitmap(rect.width(), rect.height(),config);
        Canvas canvas = new Canvas(ret);
        canvas.drawBitmap(bitmap, -rect.left, -rect.top, null);
        if (recycle) {
            canvas = null;
        }
        return ret;
    }

    /**
     * bitmap转为base64
     *
     * @param bitmap
     * @return
     */
    public static String bitmapToBase64(Bitmap bitmap) {

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
