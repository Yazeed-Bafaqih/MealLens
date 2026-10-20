package com.example.meallens;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

/** Loads a meal photo (content/file URI or the bundled demo image) into an ImageView. */
public final class ImageUtil {
    /** Marker used instead of a URI for the bundled demo meal photo. */
    public static final String DEMO = "demo";

    private ImageUtil() {}

    public static void load(ImageView view, String image) {
        if (image == null || DEMO.equals(image)) {
            view.setImageResource(R.drawable.masoub);
            return;
        }
        Bitmap bmp = decode(view.getContext(), Uri.parse(image), 1200);
        if (bmp != null) view.setImageBitmap(bmp);
        else view.setImageResource(R.drawable.masoub);
    }

    /** Decodes a downscaled, EXIF-rotated bitmap. Returns null when the URI cannot be read. */
    public static Bitmap decode(Context ctx, Uri uri, int maxSide) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bmp;
            try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
                bmp = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bmp == null) return null;
            int rotation = 0;
            try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
                if (in != null) {
                    int o = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                    if (o == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                    else if (o == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                    else if (o == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
                }
            } catch (IOException ignored) {
            }
            if (rotation != 0) {
                Matrix m = new Matrix();
                m.postRotate(rotation);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            }
            return bmp;
        } catch (IOException | SecurityException e) {
            return null;
        }
    }
}
