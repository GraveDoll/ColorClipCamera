package com.gravedoll.colorclipcamera;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.io.FileOutputStream;
import java.util.List;

public class CameraView extends JavaCameraView implements PictureCallback {

    private static final String TAG = "CameraView";
    private String mPictureFileName;
    private Context context;

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }


    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void autoFocus() {
        Camera.Parameters params = mCamera.getParameters();
        if(!params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_FIXED)){
            mCamera.autoFocus(mAutoFocusListener);
        }
    }
    private Camera.AutoFocusCallback mAutoFocusListener =
            new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    Log.i(TAG, "AutoFocus:"+b);
                    /*Toast.makeText(context, "オートフォーカスON", Toast.LENGTH_SHORT).show();*/
                }
            };

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }

    }
}
