package com.affectiva.affdexme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageHelper {

    private static final String LOG_TAG = "AffdexMe";

    // Prevent instantiation of this object
    private ImageHelper() {
    }

    public static boolean checkIfImageFileExists(@NonNull Context context, @NonNull String fileName) {

        // path to /data/data/yourapp/app_data/images
        File directory = context.getDir("images", Context.MODE_PRIVATE);

        // File location to save image
        File imagePath = new File(directory, fileName);

        return imagePath.exists();
    }

    public static boolean deleteImageFile(@NonNull Context context, @NonNull String fileName) {
        // path to /data/data/yourapp/app_data/images
        File directory = context.getDir("images", Context.MODE_PRIVATE);

        // File location to save image
        File imagePath = new File(directory, fileName);

        return imagePath.delete();
    }

    public static void resizeAndSaveResourceImageToInternalStorage(@NonNull Context context, @NonNull String fileName, @NonNull String resourceName) throws FileNotFoundException {
        final int resourceId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());

        if (resourceId == 0) {
            //unrecognised resource
            throw new FileNotFoundException("Resource not found for file named: " + resourceName);
        }
        resizeAndSaveResourceImageToInternalStorage(context, fileName, resourceId);
    }

    public static void resizeAndSaveResourceImageToInternalStorage(@NonNull Context context, @NonNull String fileName, int resourceId) {
        Resources resources = context.getResources();
        Bitmap sourceBitmap = BitmapFactory.decodeResource(resources, resourceId);
        Bitmap resizedBitmap = resizeBitmapForDeviceDensity(context, sourceBitmap);
        saveBitmapToInternalStorage(context, resizedBitmap, fileName);
        sourceBitmap.recycle();
        resizedBitmap.recycle();
    }

    public static Bitmap resizeBitmapForDeviceDensity(@NonNull Context context, @NonNull Bitmap sourceBitmap) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        int targetWidth = Math.round(sourceBitmap.getWidth() * metrics.density);
        int targetHeight = Math.round(sourceBitmap.getHeight() * metrics.density);

        return Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, false);
    }

    public static void saveBitmapToInternalStorage(@NonNull Context context, @NonNull Bitmap bitmapImage, @NonNull String fileName) {

        // path to /data/data/yourapp/app_data/images
        File directory = context.getDir("images", Context.MODE_PRIVATE);

        // File location to save image
        File imagePath = new File(directory, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imagePath);

            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "Exception while trying to save file to internal storage: " + imagePath, e);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Exception while trying to flush the output stream", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Exception wile trying to close file output stream.", e);
                }
            }
        }
    }

    public static Bitmap loadBitmapFromInternalStorage(@NonNull Context applicationContext, @NonNull String fileName) {

        // path to /data/data/yourapp/app_data/images
        File directory = applicationContext.getDir("images", Context.MODE_PRIVATE);

        // File location to save image
        File imagePath = new File(directory, fileName);

        try {
            return BitmapFactory.decodeStream(new FileInputStream(imagePath));
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "Exception wile trying to load image: " + imagePath, e);
            return null;
        }
    }

    public static void preproccessImageIfNecessary(Context context, String fileName, String resourceName) {
        // Set this to true to force the app to always load the images for debugging purposes
        final boolean DEBUG = false;

        if (ImageHelper.checkIfImageFileExists(context, fileName)) {
            // Image file already exists, no need to load the file again.

            if (DEBUG) {
                Log.d(LOG_TAG, "DEBUG: Deleting: " + fileName);
                ImageHelper.deleteImageFile(context, fileName);
            } else {
                return;
            }
        }

        try {
            ImageHelper.resizeAndSaveResourceImageToInternalStorage(context, fileName, resourceName);
            Log.d(LOG_TAG, "Resized and saved image: " + fileName);
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "Unable to process image: " + fileName, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the bitmap position inside an imageView.
     * Source: http://stackoverflow.com/a/26930938
     * Author: http://stackoverflow.com/users/1474079/chteuchteu
     *
     * @param imageView source ImageView
     * @return 0: left, 1: top, 2: width, 3: height
     */
    public static int[] getBitmapPositionInsideImageView(ImageView imageView) {
        int[] ret = new int[4];

        if (imageView == null || imageView.getDrawable() == null)
            return ret;

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int actW = Math.round(origW * scaleX);
        final int actH = Math.round(origH * scaleY);

        ret[2] = actW;
        ret[3] = actH;

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (imgViewH - actH) / 2;
        int left = (imgViewW - actW) / 2;

        ret[0] = left;
        ret[1] = top;

        return ret;
    }
}
