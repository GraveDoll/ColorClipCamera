package com.gravedoll.colorclipcamera;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.provider.MediaStore;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by GraveDoll on 2015/03/15.
 */
public class SaveFilteredPhotoAsyncTaskHelper extends AsyncTaskLoader<File> {
    private Mat mat = null;
    private Context context;
    private String[] data = {""};
    private boolean cancelFlag = false;
    private File file;
    private int degree;

    public SaveFilteredPhotoAsyncTaskHelper(Context context, Mat mat, int degree) {
        super(context);

        this.mat = mat;
        this.degree = degree;
        this.context = context;
    }

    @Override
    public void deliverResult(File file) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            return;
        }

        this.file = file;
        if (isStarted()) {
            super.deliverResult(file);
        }
    }

    @Override
    protected void onStartLoading() {
        if (file != null) {
            deliverResult(file);
        }

        if (takeContentChanged() || file == null) {
            forceLoad();
        }
    }

    @Override
    public File loadInBackground() {
        String mDirectory = Environment.getExternalStorageDirectory() + "/" + context.getString(R.string.app_name) + "/";
        String mFilename = "pht" + String.valueOf(System.currentTimeMillis()) + "_filter" + ".jpg";
        String date = String.valueOf(System.currentTimeMillis());
        String attachName = mDirectory + "/" + mFilename;
        try {

            Bitmap dst = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, dst);
            Matrix matrix = new Matrix();
            if (degree == 270) {
                matrix.postRotate(90);
            } else if (degree == 0) {
                matrix.postRotate(0);
            } else if (degree == 90) {
                matrix.postRotate(270);
            } else {
                matrix.postRotate(180);
            }

            dst = Bitmap.createBitmap(dst, 0, 0, dst.getWidth(), dst.getHeight(), matrix, true);

            FileOutputStream outputStream = new FileOutputStream(new File(mDirectory, mFilename));

            dst.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();

        }

        // コンテンツ登録（Androidギャラリーへの登録)
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = context.getContentResolver();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATA, attachName);
        values.put(MediaStore.Images.Media.SIZE, new File(attachName).length());
        values.put(MediaStore.Images.Media.DATE_ADDED, date);
        values.put(MediaStore.Images.Media.DATE_TAKEN, date);
        values.put(MediaStore.Images.Media.DATE_MODIFIED, date);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        return file;
    }


    /**
     * 中止リクエスト
     */
    @Override
    protected void onStopLoading() {
        cancelFlag = true;
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(File file) {
        super.onCanceled(file);
        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(file);
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        file = null;
    }

    protected void onReleaseResources(File file) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }


}
