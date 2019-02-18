package com.gravedoll.colorclipcamera;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener, AdjustDialogFragment.OnHSVChangeListener
{
    static {
        System.loadLibrary("opencv_java4");
    }
    private static final String TAG = "Main::Activity";
    private CameraView mOpenCvCameraView;
    private Mat mIntermediateMat;
    private Display display;
    private ImageButton imageButtonCamera, imageButtonAdjust, imageButtonAbout;
    private boolean isMagSensor;
    private boolean isAccSensor;

    // カメラアプリで撮影した写真を格納するディレクトリとファイル名
    private String mDirectory, mFilename;
    private Bitmap dst;
    private Mat tempMat;
    private int degree;

    private int minH; //= 100;
    private int minS; //= 50;
    private int minV; //= 120;

    private int maxH; //= 130;
    private int maxS; //= 255;
    private int maxV; //= 255;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private boolean mIsColorSelected = false;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(25, 50, 50, 0);

    private AdjustDialogFragment adjustDialogFragment;
    private AboutDialogFragment aboutDialogFragment;
    private OrientationEventListener orientationListener;
    private static double mMinContourArea = 0.99;
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
    private SimpleProgressDialogFragment progressDialog = null;    // ロード中画面のプログレスダイアログ作成
    private static int REQUEST_CODE = 1000;


//    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS: {
//                    Log.i(TAG, "OpenCV loaded successfully");
//                    mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
//                }
//                break;
//                default: {
//                    super.onManagerConnected(status);
//                }
//                break;
//            }
//        }
//    };

    public MainActivity() {
        // Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        ArrayList<String> permissions = new ArrayList<String>();
        //Runtime Permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            ActivityCompat.requestPermissions(
                    this,
                    permissions.toArray(new String[permissions.size()]),
                    REQUEST_CODE);
        }
        
        // 外部ストレージがない場合、メッセージを出して終了
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "SDカードがありません", Toast.LENGTH_SHORT)
                    .show();
            finish();
        } else {
            // 撮影した写真を格納するディレクトリ、ファイル名を定義。ディレクトリは、外部ストレージ(SDカードなど)のアプリデータ格納ディレクトリとしている
            mDirectory = Environment.getExternalStorageDirectory() + "/" +getString(R.string.app_name)+"/";
            // 撮影した写真を格納するディレクトリを作成する(すでに存在する場合は、作成されない)
            new File(mDirectory).mkdirs();
        }

        //タイトルバーの非表示
        //setContentViewの前に呼ぶ必要がある
        requestWindowFeature(Window.FEATURE_NO_TITLE);


        //通知領域の非表示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        // SharedPrefernces の取得
        pref = getSharedPreferences(getString(R.string.pref_hsv_adjust), Activity.MODE_PRIVATE);


        mOpenCvCameraView = (CameraView) findViewById(R.id.surfaceView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableView();
        mOpenCvCameraView.setOnTouchListener(MainActivity.this);

        imageButtonCamera = (ImageButton) findViewById(R.id.imageButtonShutter);
        imageButtonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog = SimpleProgressDialogFragment.newInstance();
                progressDialog.show(getFragmentManager(), "progress");
                getLoaderManager().restartLoader(1, null, checkDataLoaderCallbacks);
            }
        });

        imageButtonAdjust = (ImageButton) findViewById(R.id.imageButtonHsv);
        imageButtonAdjust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adjustDialogFragment = AdjustDialogFragment.newInstance(degree);
                adjustDialogFragment.show(getFragmentManager(), getString(R.string.app_name));
            }
        });

        imageButtonAbout = (ImageButton) findViewById(R.id.imageButtonAbout);
        imageButtonAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                aboutDialogFragment = AboutDialogFragment.getInstance();
                aboutDialogFragment.show(getFragmentManager(), getString(R.string.action_about));

            }
        });

    }


    private final LoaderManager.LoaderCallbacks checkDataLoaderCallbacks = new LoaderManager.LoaderCallbacks<File>() {
        @Override
        public Loader<File> onCreateLoader(int id, Bundle args) {
            return new SavePhotoAsyncTaskHelper(getApplicationContext(), tempMat, degree);
        }

        @Override
        public void onLoadFinished(android.content.Loader<File> loader, File data) {
            Intent intent = new Intent(getApplicationContext(), PreviewActivity.class);
            intent.putExtra("file", data);
            intent.putExtra("degree", degree);

            startActivity(intent);
            if (progressDialog.getShowsDialog()) {
                progressDialog.onDismiss(progressDialog.getDialog());
            }
        }

        @Override
        public void onLoaderReset(android.content.Loader<File> loader) {

        }
    };


    @Override
    public void onPause() {
        super.onPause();
        //orientationListener.disable();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        if (isMagSensor || isAccSensor) {
            //sensorManager.unregisterListener(this);
            isMagSensor = false;
            isAccSensor = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

//        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
//        } else {
//            Log.d(TAG, "OpenCV library found inside package. Using it!");
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }
        //orientationListener.enable();

        if(mOpenCvCameraView != null) {
            mOpenCvCameraView.enableView();
        }

    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
        tempMat = new Mat();
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();
        if (tempMat != null)
            tempMat.release();
        if (mRgba != null)
            mRgba.release();

        tempMat = null;
        mIntermediateMat = null;
        mRgba = null;
    }


    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Size sizeRgba = mRgba.size();


        Mat rgbaInnerWindow;
        Mat rgbaInnerWindow2 = new Mat();
        //Mat monoInnerWindow = new Mat();
        Mat compositeWindow = new Mat();

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = 0/*cols / 8*/;
        int top = 0/*rows / 8*/;

        int width = cols /** 3 / 4*/;
        int height = rows /** 3 / 4*/;

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        int minH = pref.getInt(getString(R.string.pref_minH_value), 0);
        int minS = pref.getInt(getString(R.string.pref_minS_value), 0);
        int minV = pref.getInt(getString(R.string.pref_minV_value), 0);
        int maxH = pref.getInt(getString(R.string.pref_maxH_value), 0);
        int maxS = pref.getInt(getString(R.string.pref_maxS_value), 0);
        int maxV = pref.getInt(getString(R.string.pref_maxV_value), 0);

        //Mat hsv = new Mat();
        Scalar lowerb = new Scalar(minH, minS, minV);
        Scalar upperb = new Scalar(maxH, maxS, maxV);

        rgbaInnerWindow = mRgba.submat(top, top + height, left, left + width);
        mRgba.copyTo(tempMat);

        //Imgproc.pyrDown(rgbaInnerWindow, mIntermediateMat);
        //Imgproc.pyrDown(mIntermediateMat, mIntermediateMat);

        Imgproc.cvtColor(rgbaInnerWindow, mIntermediateMat, Imgproc.COLOR_RGB2HSV_FULL);

        Core.inRange(mIntermediateMat, lowerb, upperb, mIntermediateMat);
        //Imgproc.dilate(mIntermediateMat, mIntermediateMat, new Mat());
        Imgproc.findContours(mIntermediateMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea * maxArea) {
                //Core.multiply(contour, new Scalar(4,4), contour);
                mContours.add(contour);
            }
        }

        Imgproc.drawContours(rgbaInnerWindow, mContours, -1, new Scalar(255, 0, 0, 255), 1);


        rgbaInnerWindow.release();
        //rgbaInnerWindow2.release();
        //roi.release();
        //maskMat.release();
        //monoInnerWindow.release();
        //maskWindow.release();

        return mRgba;
    }

    public static boolean hasFeatureAutoFocus(Context context) {
        boolean hasAutoFocus = false;
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        hasAutoFocus = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        return hasAutoFocus;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch event");

        if (hasFeatureAutoFocus(this.getApplicationContext())) {
            mOpenCvCameraView.autoFocus();
        }

        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x > 4) ? x - 4 : 0;
        touchedRect.y = (y > 4) ? y - 4 : 0;

        touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        //mDetector.setHsvColor(mBlobColorHsv);
        setHsvColor(mBlobColorHsv);

        //Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return true;
    }

    public void setHsvColor(Scalar hsvColor) {
        int lowerH = (int) ((hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0] - mColorRadius.val[0] : 0);
        int upperH = (int) ((hsvColor.val[0] + mColorRadius.val[0] <= 255) ? hsvColor.val[0] + mColorRadius.val[0] : 255);
        int lowerS = (int) ((hsvColor.val[1] >= mColorRadius.val[1]) ? hsvColor.val[1] - mColorRadius.val[1] : 1);
        int upperS = (int) ((hsvColor.val[1] + mColorRadius.val[1] <= 255) ? hsvColor.val[1] + mColorRadius.val[1] : 255);
        int lowerV = (int) ((hsvColor.val[2] >= mColorRadius.val[2]) ? hsvColor.val[2] - mColorRadius.val[2] : 2);
        int upperV = (int) ((hsvColor.val[2] + mColorRadius.val[2] <= 255) ? hsvColor.val[2] + mColorRadius.val[2] : 255);
        onMinHChanged(lowerH);
        onMaxHChanged(upperH);
        onMinSChanged(lowerS);
        onMaxSChanged(upperS);
        onMinVChanged(lowerV);
        onMaxVChanged(upperV);

    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override
    public void onMinHChanged(int minH) {
        this.minH = minH;
        Log.v("MinH", String.valueOf(minH));
        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_minH_value), minH);
        editor.putString(getString(R.string.pref_minH_text), String.valueOf(minH));
        // データの保存
        editor.commit();

    }

    @Override
    public void onMinSChanged(int minS) {
        this.minS = minS;
        Log.v("MinH", String.valueOf(minS));
        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_minS_value), minS);
        editor.putString(getString(R.string.pref_minS_text), String.valueOf(minS));
        // データの保存
        editor.commit();
    }

    @Override
    public void onMinVChanged(int minV) {
        this.minV = minV;
        Log.v("MinH", String.valueOf(minV));
        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_minV_value), minV);
        editor.putString(getString(R.string.pref_minV_text), String.valueOf(minV));
        // データの保存
        editor.commit();
    }

    @Override
    public void onMaxHChanged(int maxH) {
        this.maxH = maxH;
        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_maxH_value), maxH);
        editor.putString(getString(R.string.pref_maxH_text), String.valueOf(maxH));
        // データの保存
        editor.commit();
    }

    @Override
    public void onMaxSChanged(int maxS) {
        this.maxS = maxS;
        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_maxS_value), maxS);
        editor.putString(getString(R.string.pref_maxS_text), String.valueOf(maxS));
        // データの保存
        editor.commit();
    }

    @Override
    public void onMaxVChanged(int maxV) {
        this.maxV = maxV;
        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_maxV_value), maxV);
        editor.putString(getString(R.string.pref_maxV_text), String.valueOf(maxV));
        // データの保存
        editor.commit();
    }
}
