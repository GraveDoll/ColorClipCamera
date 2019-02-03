package com.gravedoll.colorclipcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Created by GraveDoll on 2015/02/21.
 */
public class PhotoEditView extends AppCompatImageView {
    // 撮影した写真(コンストラクタに与えられた写真)を保持するメンバ変数
    private Bitmap mPhoto;

    // 描画対象となる画像(Bitmap)とそのキャンバスを保持するメンバ変数
    private Bitmap mBitmap;

    private Canvas mCanvas;

    Matrix matrix;
    int photoW;
    int photoH;

    int width;
    int height;

    int scaledWidth;
    int scaledHeight;

    float degrees;

    // コンストラクタ(撮影した写真がない場合)
    public PhotoEditView(Context context, AttributeSet attrs) {
        // 撮影した写真がある場合のコンストラクタを呼ぶ
        this(context, attrs, null);
    }

    // コンストラクタ（撮影した写真が存在する場合）
    public PhotoEditView(Context context, AttributeSet attrs, Bitmap bitmap) {
        super(context, attrs);
        mPhoto = bitmap;//撮影した写真で初期化
        // 描画対象となる画像、キャンバスを用意する
        mBitmap = null;
        mCanvas = null;
        //setBackgroundColor(getResources().getColor(R.color.colorPrimary));// 背景色設定
    }

    public void setCapturedImage(Bitmap capturedImage) {
        this.mPhoto = capturedImage;
        // キャンバスに描画する
        // 写真を回転・縮小した画像を生成し、
        Bitmap bitmap = Bitmap.createBitmap(mPhoto, 0, 0, photoW, photoH, matrix, true);
        mCanvas.drawBitmap(bitmap, (width-bitmap.getWidth())/2, (height-bitmap.getHeight())/2, null);
        bitmap.recycle();
        // Viewに描画対象となる画像をセットする
        setImageBitmap(mBitmap);
    }
    public void recycleImage() {
        this.mBitmap.recycle();
        this.mBitmap = null;
    }

    public void setBitmap(Bitmap bitmap){
        mPhoto = bitmap;
    }

    public int getScaledImageWidth(){
        return scaledWidth;
    }

    public int getScaledheightWidth(){
        return scaledHeight;
    }

    public float getDegrees(){
        return degrees;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {//Viewのサイズが変わったとき
        super.onSizeChanged(width, height, oldWidth, oldHeight);
            this.width = width;
            this.height = height;
            // 描画対象となる画像が未生成であれば、生成する
            if (mBitmap == null) {
                // 描画対象となる画像として、Viewの大きさのBitmapを生成する
                mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                // 描画対象となる画像のキャンバスを生成する
                mCanvas = new Canvas(mBitmap);
                // 写真があれば、回転・縮小した画像を生成する
                if (mPhoto != null) {
                    // 写真を回転・縮小するためのMatrixを生成
                    matrix = new Matrix();
                    degrees = 0; // 回転のデフォルト値(角度0)
                    float scale = 1; // 縮尺のデフォルト値(等倍)
                    float scaleW = 1; // 幅方向の縮尺のデフォルト値(等倍)
                    float scaleH = 1; // 高さ方向の縮尺のデフォルト値(等倍)
                    photoW = mPhoto.getWidth(); // 写真の幅
                    photoH = mPhoto.getHeight(); // 写真の高さ
                    // 画面が横長(Landscape)かつ写真が縦長の場合、写真を時計回りに270度(反時計回りに90度)回転する
                    if (width > height && photoW < photoH) {
                        degrees = 270;
                        // 画面が縦長(Portrait)かつ写真が横長の場合、写真を時計回りに90度回転する
                    } else if (width < height && photoW > photoH) {
                        degrees = 90;
                    }
                    // 写真が回転される場合は、
                    if (degrees > 0) {
                        // 回転を指定し、
                        matrix.postRotate(degrees);
                        // 回転を想定した縮尺計算をする(画面と写真の幅と高さを入れ替えて計算)
                        scaleW = (float) width / photoH;
                        scaleH = (float) height / photoW;
                    } else { // 写真が回転しない場合は、通常の縮尺計算をする
                        scaleW = (float) width / photoW;
                        scaleH = (float) height / photoH;
                    }
                    // 縮尺として、幅縮尺と高さ縮尺のの小さい方を採用(写真が画面に収まるように縮小する)
                    if (scaleW > scaleH) {
                        scale = scaleH;
                    } else {
                        scale = scaleW;
                    }
                    // 縮尺が1より小さい場合は縮小する(1以上の場合は、拡大しない)
                    if (scale < 1) {
                        // 縮小を指定
                        matrix.postScale(scale, scale);
                    }
                    // 写真を回転・縮小した画像を生成し、
                    Bitmap bitmap = Bitmap.createBitmap(mPhoto, 0, 0, photoW, photoH, matrix, true);
                    // キャンバスに描画する
                    mCanvas.drawBitmap(bitmap, (width - bitmap.getWidth()) / 2, (height - bitmap.getHeight()) / 2, null);

                    // 回転・縮小した写真は不要になるため、メモリリソースを開放する
                    bitmap.recycle();
                    bitmap = null;
                }
                // Viewに描画対象となる画像をセットする
                setImageBitmap(mBitmap);
            }


    }


}
