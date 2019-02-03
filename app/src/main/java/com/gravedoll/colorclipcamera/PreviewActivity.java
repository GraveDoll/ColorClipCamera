package com.gravedoll.colorclipcamera;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by GraveDoll on 2015/02/21.
 */
public class PreviewActivity extends AppCompatActivity implements FilterDialogFragment.OnFilterChangeListener, AdjustDialogFragment.OnHSVChangeListener{
    public static final int FILTER_NONE = 0;
    public static final int FILTER_MODE_GRAY = 1;
    public static final int FILTER_MODE_SEPIA = 2;
    public static final int FILTER_MODE_BLUR = 3;
    public static final int CROP = 4;

    public static int filterMode = FILTER_NONE;
    // 描画対象のViewを保持するメンバ変数
    private PhotoEditView photoEditView;
    private Uri saveUri;// 写真を格納場所を示すURI
    private Bitmap bitmap, dst;
    private int degree;
    private ImageButton imageButtonFilter,imageButtonSave;
    private FilterDialogFragment filterDialogFragment;
    private OrientationEventListener orientationListener;
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
    private SharedPreferences pref;
    SharedPreferences.Editor editor;
    private static double mMinContourArea = 0.999;
    private Scalar mBlobColorHsv;
    private Mat mRgba,roi;
    private Scalar mBlobColorRgba;
    private boolean mIsColorSelected = false;
    private boolean mCloppedFlag = false;
    private boolean mDrawContoursFlag = true;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(25,50,50,0);
    int minH;
    int minS;
    int minV;
    int maxH;
    int maxS;
    int maxV;

    int oldDegree;
    SimpleProgressDialogFragment progressDialog = null;    // ロード中画面のプログレスダイアログ作成

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_filter:
                    filterDialogFragment = FilterDialogFragment.getInstance();
                    filterDialogFragment.show(getFragmentManager(), getString(R.string.app_name));
                    return true;
                case R.id.navigation_crop:
                    if(filterMode!= CROP) {
                        filterMode = CROP;
                        photoEditView.setCapturedImage(processing());
                        item.setTitle(R.string.title_crop_on);
                    }
                    else{
                        filterMode = FILTER_NONE;
                        photoEditView.setCapturedImage(processing());
                        item.setTitle(R.string.title_crop_off);
                    }
                    return true;
                case R.id.navigation_border:
                    if(mDrawContoursFlag==false) {
                        mDrawContoursFlag = true;
                        item.setTitle(R.string.title_border_on);
                    }else{
                        mDrawContoursFlag = false;
                        item.setTitle(R.string.title_border_off);
                    }
                    photoEditView.setImageBitmap(null);
                    photoEditView.setCapturedImage(processing());
                    return true;
                case R.id.navigation_save:
                    if(filterMode == CROP){
                        mRgba = roi;
                    }
                    progressDialog = SimpleProgressDialogFragment.newInstance();
                    progressDialog.show(getFragmentManager(), "progress");
                    getLoaderManager().restartLoader(1, null, checkDataLoaderCallbacks);
                    return true;

            }
            return false;
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //タイトルバーの非表示
        //setContentViewの前に呼ぶ必要がある
        Toast.makeText(getApplicationContext(), "保存完了！", Toast.LENGTH_SHORT)
                .show();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //通知領域の非表示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_preview);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // SharedPrefernces の取得
        pref = getSharedPreferences(getString(R.string.pref_hsv_adjust), Activity.MODE_PRIVATE);
        Intent intent = getIntent();
        File data = (File) intent.getSerializableExtra("file");
        oldDegree = intent.getIntExtra("degree",0);
        saveUri = Uri.fromFile(data);
        // (画面回転問題への対策)この時点で、画面が横向きであれば
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 画面を横向きに固定する
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            // そうでなければ(画面が縦向きであれば)、
        } else {
            // 画面を縦向きに固定する
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        // 写真を格納するBitmapを用意する
        bitmap = null;

        try {
            // 存在するなら(撮影した写真を指定したファイルに出力するカメラアプリの場合)、ファイルから写真を読み込む
            if (data.exists()) {
                bitmap = loadBitmap(new FileInputStream(data));
                // 存在せず、インテントの戻り値に写真ファイルへのURIを返す場合(撮影した写真を取り出すためのコンテントプロバイダを提供するカメラアプリの場合)、コンテントプロバイダから写真を読み込む
            } else if (data != null) {
                bitmap = BitmapFactory.decodeStream(new BufferedInputStream(getContentResolver().openInputStream(saveUri)));
            }
            // いずれでもない場合は、写真の取り込みに失敗したとする(bitmapがnullのまま)
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // 写真の取り込みに成功した場合、
        if (bitmap != null) {
            // 表示する写真を指定して、Viewを生成する
            //photoMemoView = new PhotoMemoView(this, bitmap);
            this.photoEditView = (PhotoEditView)findViewById(R.id.photoEditView);
            // Viewを画面に表示する
            if (photoEditView != null) {
                //createLayout();
                photoEditView.setBitmap(processing());
            }

            // 写真の取り込みに失敗した場合(カメラアプリから正常に戻らなかった場合を含む)
        } else {
            // 写真を取り込めなかった旨を表示し、
            Toast.makeText(this, "写真を取り込めませんでした", Toast.LENGTH_SHORT)
                    .show();
            // 写真のないViewを生成する
            //photoMemoView = new PhotoMemoView(this);
        }

    }

    private final LoaderManager.LoaderCallbacks checkDataLoaderCallbacks = new LoaderManager.LoaderCallbacks<File>() {
        @Override
        public Loader<File> onCreateLoader(int id, Bundle args) {
            return new SaveFilteredPhotoAsyncTaskHelper(getApplicationContext(), mRgba, oldDegree);
        }

        @Override
        public void onLoadFinished(Loader<File> loader, File data) {

            if (progressDialog.getShowsDialog()) {
                progressDialog.onDismiss(progressDialog.getDialog());
            }
            Toast.makeText(getApplicationContext(), "保存完了！", Toast.LENGTH_SHORT)
                    .show();
        }

        @Override
        public void onLoaderReset(Loader<File> loader) {

        }
    };

    public void setHsvColor(Scalar hsvColor) {
        int lowerH = (int)((hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0);
        int upperH = (int)((hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255);

        int lowerS = (int)(hsvColor.val[1] - mColorRadius.val[1]);
        int upperS = (int)(hsvColor.val[1] + mColorRadius.val[1]);

        int lowerV = (int)(hsvColor.val[2] - mColorRadius.val[2]);
        int upperV = (int)(hsvColor.val[2] + mColorRadius.val[2]);

        minH = lowerH;
        maxH = upperH;
        minS = lowerS;
        maxS = upperS;
        minV = lowerV;
        maxV = upperV;

        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_minH_value),minH);
        editor.putString(getString(R.string.pref_minH_text),String.valueOf(minH));
        editor.putInt(getString(R.string.pref_minS_value),minS);
        editor.putString(getString(R.string.pref_minS_text),String.valueOf(minS));
        editor.putInt(getString(R.string.pref_minV_value),minV);
        editor.putString(getString(R.string.pref_minV_text),String.valueOf(minV));

        editor.putInt(getString(R.string.pref_maxH_value),maxH);
        editor.putString(getString(R.string.pref_maxH_text),String.valueOf(maxH));
        editor.putInt(getString(R.string.pref_maxS_value),maxS);
        editor.putString(getString(R.string.pref_maxS_text),String.valueOf(maxS));
        editor.putInt(getString(R.string.pref_maxV_value),maxV);
        editor.putString(getString(R.string.pref_maxV_text),String.valueOf(maxV));
        // データの保存
        editor.commit();

        photoEditView.setImageBitmap(null);
        photoEditView.setCapturedImage(processing());

    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }


    // ファイル等のInputStreamから画像(撮影した写真)を読み込む
    private Bitmap loadBitmap(InputStream is) {
        // 読み込む画像のデータサイズの上限(1MB)
        final int BITMAP_SIZE_MAX = 2 * 1024 * 1024;

        // 読み込んだ画像を格納するBitmapを用意する
        Bitmap bitmap = null;
        try {
            // ファイルサイズ(InputStreamのサイズ)が、読み込む上限を超えている場合、
            if (is.available() > BITMAP_SIZE_MAX) {
                // 画像を小さくして読み込む(大きな画像を読み込んだ場合に発生するメモリ不足への対策)
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = is.available() / BITMAP_SIZE_MAX + 1;
                bitmap = BitmapFactory.decodeStream(is, null, options);
                // 上限を超えていない場合、
            } else {
                // そのまま読み込む
                bitmap = BitmapFactory.decodeStream(is);
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 読み込んだ画像を返す
        return bitmap;
    }

    public void findContours(){
        minH = pref.getInt(getString(R.string.pref_minH_value),0);
        minS = pref.getInt(getString(R.string.pref_minS_value),0);
        minV = pref.getInt(getString(R.string.pref_minV_value),0);
        maxH = pref.getInt(getString(R.string.pref_maxH_value),0);
        maxS = pref.getInt(getString(R.string.pref_maxS_value),0);
        maxV = pref.getInt(getString(R.string.pref_maxV_value),0);

        mRgba = new Mat();
        Mat mIntermediateMat = new Mat();
        Utils.bitmapToMat(bitmap, mRgba);
        Size sizeRgba = mRgba.size();

        Mat rgbaInnerWindow = new Mat();

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;
        int left = 0;
        int top = 0;
        int width = cols;
        int height = rows;

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Scalar lowerb = new Scalar(minH, minS, minV);
        Scalar upperb = new Scalar(maxH, maxS, maxV);

        rgbaInnerWindow = mRgba.submat(top, top + height, left, left + width);

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
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                //Core.multiply(contour, new Scalar(4,4), contour);
                mContours.add(contour);
            }
        }

        //Imgproc.drawContours(rgbaInnerWindow, mContours, -1, new Scalar(255, 0, 0,255),1);
        rgbaInnerWindow.release();
        mIntermediateMat.release();

    }


    public Bitmap processing() {
        /*int minH = 100;
        int minS = 50;
        int minV = 120;

        int maxH = 130;
        int maxS = 255;
        int maxV = 255;*/
        minH = pref.getInt(getString(R.string.pref_minH_value),0);
        minS = pref.getInt(getString(R.string.pref_minS_value),0);
        minV = pref.getInt(getString(R.string.pref_minV_value),0);
        maxH = pref.getInt(getString(R.string.pref_maxH_value),0);
        maxS = pref.getInt(getString(R.string.pref_maxS_value),0);
        maxV = pref.getInt(getString(R.string.pref_maxV_value),0);
        Log.v("PrecireActivity-MinH", String.valueOf(minH));
        Log.v("PrecireActivity-MinS", String.valueOf(minS));
        Log.v("PrecireActivity-MinV", String.valueOf(minV));
        Log.v("PrecireActivity-MaxH", String.valueOf(maxH));
        Log.v("PrecireActivity-MaxS", String.valueOf(maxS));
        Log.v("PrecireActivity-MaxV", String.valueOf(maxV));

        findContours();

        Mat maskMat = new Mat();
        Size sizeRgba = mRgba.size();

        Mat rgbaInnerWindow = new Mat();

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = 0;
        int top = 0;

        int width = cols;
        int height = rows;

        rgbaInnerWindow = mRgba.submat(top, top + height, left, left + width);
        Mat maskedRgbaInnerWindow = rgbaInnerWindow.clone();

        switch (PreviewActivity.filterMode) {
            case FILTER_NONE:
                rgbaInnerWindow.release();
                maskMat.release();
                break;


            case FILTER_MODE_GRAY:
                Mat monoInnerWindow = new Mat();
                Imgproc.cvtColor(rgbaInnerWindow, monoInnerWindow, Imgproc.COLOR_RGB2GRAY);//モノクロ化
                Imgproc.cvtColor(monoInnerWindow, monoInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);

                maskMat = Mat.zeros(new Size(width, height), CvType.CV_8UC3);
                Imgproc.cvtColor(maskMat, maskMat, Imgproc.COLOR_BGRA2RGBA);
                Imgproc.drawContours(maskMat, mContours, -1, new Scalar(255, 255, 255), -1);

                Core.bitwise_and(rgbaInnerWindow, maskMat, maskedRgbaInnerWindow);//mask部分のみ残します
                Core.bitwise_not(maskMat, maskMat);       // maskを反転します
                Core.bitwise_and(monoInnerWindow, maskMat, monoInnerWindow); // 元画像からmask部分を除去して背景画像を準備します
                Core.bitwise_or(monoInnerWindow, maskedRgbaInnerWindow, rgbaInnerWindow);     // 処理した画像と元の背景部分を合成します

                rgbaInnerWindow.release();
                //mIntermediateMat.release();
                monoInnerWindow.release();
                maskedRgbaInnerWindow.release();
                maskMat.release();
                break;

            case FILTER_MODE_SEPIA:
                // Fill sepia kernel
                Mat sepiaKernel = new Mat(4, 4, CvType.CV_32F);
                sepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
                sepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
                sepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
                sepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);

                Mat sepiaWindow = new Mat();
                Core.transform(rgbaInnerWindow, sepiaWindow, sepiaKernel);

                maskMat = Mat.zeros(new Size(width, height), CvType.CV_8UC3);
                Imgproc.cvtColor(maskMat, maskMat, Imgproc.COLOR_BGRA2RGBA);
                Imgproc.drawContours(maskMat, mContours, -1, new Scalar(255, 255, 255), -1);

                Core.bitwise_and(rgbaInnerWindow, maskMat, maskedRgbaInnerWindow);//mask部分のみ残します
                Core.bitwise_not(maskMat, maskMat);       // maskを反転します
                Core.bitwise_and(sepiaWindow, maskMat, sepiaWindow); // 元画像からmask部分を除去して背景画像を準備します
                Core.bitwise_or(sepiaWindow, maskedRgbaInnerWindow, rgbaInnerWindow);     // 処理した画像と元の背景部分を合成します

                rgbaInnerWindow.release();
                //mIntermediateMat.release();
                sepiaWindow.release();
                sepiaKernel.release();
                maskedRgbaInnerWindow.release();
                maskMat.release();
                break;
            case FILTER_MODE_BLUR:
                Mat blurWindow = new Mat();
                Imgproc.GaussianBlur(rgbaInnerWindow,blurWindow,new Size(51,3),0,0);
                maskMat = Mat.zeros(new Size(width, height), CvType.CV_8UC3);
                Imgproc.cvtColor(maskMat, maskMat, Imgproc.COLOR_BGRA2RGBA);
                Imgproc.drawContours(maskMat, mContours, -1, new Scalar(255, 255, 255), -1);

                Core.bitwise_and(rgbaInnerWindow, maskMat, maskedRgbaInnerWindow);//mask部分のみ残します
                Core.bitwise_not(maskMat, maskMat);       // maskを反転します
                Core.bitwise_and(blurWindow, maskMat,blurWindow); // 元画像からmask部分を除去して背景画像を準備します
                Core.bitwise_or(blurWindow, maskedRgbaInnerWindow, rgbaInnerWindow);     // 処理した画像と元の背景部分を合成します

                rgbaInnerWindow.release();
                //mIntermediateMat.release();
                blurWindow.release();
                maskedRgbaInnerWindow.release();
                maskMat.release();
                break;

            case CROP:
                //Imgproc.drawContours(rgbaInnerWindow, contours, -1, new Scalar(255, 0, 0, 255), 1);
                double boxXStart = 0;
                double boxXEnd = rgbaInnerWindow.cols();
                double boxYStart = 0;
                double boxYEnd = rgbaInnerWindow.rows();

                for (int j = 0; j < mContours.size(); j++) {
                    MatOfPoint pointmat = mContours.get(j);
                    MatOfPoint2f pointmat2 = new MatOfPoint2f(pointmat.toArray());
                    RotatedRect bbox = Imgproc.minAreaRect(pointmat2);
                    Rect box = bbox.boundingRect();
                    Scalar color = new Scalar(0, 255, 0, 255);
                    //Core.rectangle(rgbaInnerWindow, box.tl(), box.br(), color, 2);
                    if (box.tl().y < 0) {
                        boxYStart = 0;
                    } else {
                        boxYStart = box.tl().y;
                    }

                    if (box.br().y > rgbaInnerWindow.rows()) {
                        boxYEnd = rgbaInnerWindow.height();
                    } else {
                        boxYEnd = box.br().y;
                    }

                    if (box.tl().x < 0) {
                        boxXStart = 0;
                    } else {
                        boxXStart = box.tl().x;
                    }

                    if (box.br().x > rgbaInnerWindow.cols()) {
                        boxXEnd = rgbaInnerWindow.width();
                    } else {
                        boxXEnd = box.br().x;
                    }

                }
                roi = rgbaInnerWindow.submat((int) boxYStart, (int) boxYEnd, (int) boxXStart, (int) boxXEnd);

                Mat blackMat = new Mat(new Size(width, height), CvType.CV_8UC3, new Scalar(0,0,0));
                Imgproc.cvtColor(blackMat, blackMat, Imgproc.COLOR_BGRA2RGBA);

                //Mat whiteWindow = new Mat(new Size(roi.width(), roi.height()),CvType.CV_8UC4,new Scalar(255, 255, 255,100));
                //Imgproc.GaussianBlur(rgbaInnerWindow,blackWindow,new Size(9,7),8,6);

                maskMat = Mat.zeros(new Size(width, height), CvType.CV_8UC3);
                Imgproc.cvtColor(maskMat, maskMat, Imgproc.COLOR_BGRA2RGBA);
                Imgproc.rectangle(maskMat, new Point(boxXStart,boxYStart),new Point(boxXEnd,boxYEnd),new Scalar(255,255,255),-1);


                Core.bitwise_and(rgbaInnerWindow, maskMat, maskedRgbaInnerWindow);//mask部分のみ残します
                Core.bitwise_not(maskMat, maskMat);       // maskを反転します
                Core.bitwise_and(blackMat, maskMat,blackMat); // 元画像からmask部分を除去して背景画像を準備します
                Core.bitwise_or(blackMat, maskedRgbaInnerWindow, rgbaInnerWindow);     // 処理した画像と元の背景部分を合成します



                rgbaInnerWindow.release();
                //mIntermediateMat.release();
                //roi.release();
                blackMat.release();
                maskMat.release();
                maskedRgbaInnerWindow.release();

                break;

        }

        Mat drawnMat = mRgba.clone();
        if(mDrawContoursFlag) {
            Imgproc.drawContours(drawnMat, mContours, -1, new Scalar(255, 0, 0, 255), 1);
        }

//        if(filterMode==FILTER_NONE){
//            imageButtonSave.setEnabled(false);
//            imageButtonSave.setImageBitmap(null);
//        }else{
//            imageButtonSave.setEnabled(true);
//
//            imageButtonSave.setImageResource(R.drawable.ic_save_white_24dp);
//        }


        Bitmap dst = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(drawnMat, dst);

        drawnMat.release();

        return dst;

    }

    @Override
    public void onPause() {
        super.onPause();
        //orientationListener.disable();

    }

    @Override
    public void onResume() {
        super.onResume();

        //orientationListener.enable();

    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onFilterChanged(int filterNumber) {
        filterMode = filterNumber;
        photoEditView.setImageBitmap(null);
        photoEditView.setCapturedImage(processing());

    }


    @Override
    public void onMinHChanged(int minH) {
        this.minH = minH;
        Log.v("MinH", String.valueOf(minH));
        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_minH_value),minH);
        editor.putString(getString(R.string.pref_minH_text),String.valueOf(minH));
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
        editor.putInt(getString(R.string.pref_minS_value),minS);
        editor.putString(getString(R.string.pref_minS_text),String.valueOf(minS));
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
        editor.putInt(getString(R.string.pref_minV_value),minV);
        editor.putString(getString(R.string.pref_minV_text),String.valueOf(minV));
        // データの保存
        editor.commit();
    }

    @Override
    public void onMaxHChanged(int maxH) {
        this.maxH = maxH;
        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_maxH_value),maxH);
        editor.putString(getString(R.string.pref_maxH_text),String.valueOf(maxH));
        // データの保存
        editor.commit();
    }

    @Override
    public void onMaxSChanged(int maxS) {
        this.maxS = maxS;
        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_maxS_value),maxS);
        editor.putString(getString(R.string.pref_maxS_text),String.valueOf(maxS));
        // データの保存
        editor.commit();
    }

    @Override
    public void onMaxVChanged(int maxV) {
        this.maxV = maxV;
        // Editor の設定
        editor = pref.edit();
        // Editor に値を代入
        editor.putInt(getString(R.string.pref_maxV_value),maxV);
        editor.putString(getString(R.string.pref_maxV_text),String.valueOf(maxV));
        // データの保存
        editor.commit();
    }




}
