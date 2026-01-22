package com.example.maintenance_dashboard.utils; // Changed Package

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {

    /**
     * Decodes a Bitmap from a content URI.
     *
     * @param context The context to use for accessing the content resolver.
     * @param uri     The URI of the image.
     * @return The decoded Bitmap.
     * @throws IOException If the stream cannot be opened or decoded.
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Unable to open InputStream for URI: " + uri);
        }
        return BitmapFactory.decodeStream(inputStream);
    }

    /**
     * Resizes a Bitmap to a new width and height.
     *
     * @param source    The original Bitmap.
     * @param newWidth  The desired new width.
     * @param newHeight The desired new height.
     * @return The resized Bitmap.
     */
    public static Bitmap resizeBitmap(Bitmap source, int newWidth, int newHeight) {
        if (source == null) {
            return null;
        }
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, false);
    }
}
