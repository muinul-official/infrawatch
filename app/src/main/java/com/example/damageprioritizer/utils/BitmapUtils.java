package com.example.damageprioritizer.utils;

import android.graphics.Bitmap;

public class BitmapUtils {

    public static Bitmap resizeBitmap(Bitmap source, int maxWidth, int maxHeight) {
        if (source == null) {
            return null;
        }

        int width = source.getWidth();
        int height = source.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return source;
        }

        float aspectRatio = (float) width / (float) height;
        if (aspectRatio > 1) {
            width = maxWidth;
            height = (int) (width / aspectRatio);
        } else {
            height = maxHeight;
            width = (int) (height * aspectRatio);
        }

        return Bitmap.createScaledBitmap(source, width, height, true);
    }
}
