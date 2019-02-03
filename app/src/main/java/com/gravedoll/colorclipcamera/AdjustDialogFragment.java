package com.gravedoll.colorclipcamera;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by GraveDoll on 2014/10/13.
 */
public class AdjustDialogFragment extends DialogFragment {
    private static final String TAG = AdjustDialogFragment.class.getSimpleName();
    private DialogInterface.OnClickListener okClickListener = null;
    //private DialogInterface.OnClickListener cancelClickListener = null;
    private static final String PREF_ADJUST_HSV = "preferenceTest";
    private static final String MIN_H_VALUE= "minHvalue";
    private static final String MIN_H_TEXT= "minHText";

    public interface OnHSVChangeListener {
        public void onMinHChanged(int minH);
        public void onMinSChanged(int minS);
        public void onMinVChanged(int minV);

        public void onMaxHChanged(int maxH);
        public void onMaxSChanged(int maxS);
        public void onMaxVChanged(int maxV);
    }

    private OnHSVChangeListener mListener;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnHSVChangeListener == false) {
            throw new ClassCastException("activity が OnSeekBarChangedListener を実装していません.");
        }

        mListener = ((OnHSVChangeListener) activity);
    }

    public static AdjustDialogFragment newInstance(int degree) {
        AdjustDialogFragment fragment = new AdjustDialogFragment();
        Bundle args = new Bundle();
        args.putInt("degree", degree);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Dialog dialog = getDialog();
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int dialogWidth = (int) (600);
        int dialogHeight = (int) (600);

        lp.width = dialogWidth;
        lp.height = dialogHeight;
        lp.gravity = Gravity.RIGHT;
        dialog.getWindow().setAttributes(lp);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // フルスクリーン設定
        getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        // 背景を透明にする
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        final View content = inflater.inflate(R.layout.fragment_hsv, null);
        content.setRotation(270);

        SharedPreferences pref;
        SharedPreferences.Editor editor;
        // SharedPrefernces の取得
        pref = getActivity().getSharedPreferences(getString(R.string.pref_hsv_adjust), Activity.MODE_PRIVATE);


        final SeekBar seekBarMinH = (SeekBar)content.findViewById(R.id.seekBarMinH);
        final SeekBar seekBarMinS = (SeekBar)content.findViewById(R.id.seekBarMinS);
        final SeekBar seekBarMinV = (SeekBar)content.findViewById(R.id.seekBarMinV);
        final SeekBar seekBarMaxH = (SeekBar)content.findViewById(R.id.seekBarMaxH);
        final SeekBar seekBarMaxS = (SeekBar)content.findViewById(R.id.seekBarMaxS);
        final SeekBar seekBarMaxV = (SeekBar)content.findViewById(R.id.seekBarMaxV);

        seekBarMinH.setProgress(pref.getInt(getString(R.string.pref_minH_value),0));
        seekBarMinS.setProgress(pref.getInt(getString(R.string.pref_minS_value),0));
        seekBarMinV.setProgress(pref.getInt(getString(R.string.pref_minV_value),0));
        seekBarMaxH.setProgress(pref.getInt(getString(R.string.pref_maxH_value),0));
        seekBarMaxS.setProgress(pref.getInt(getString(R.string.pref_maxS_value),0));
        seekBarMaxV.setProgress(pref.getInt(getString(R.string.pref_maxV_value),0));

        final TextView textViewMinHValue = (TextView)content.findViewById(R.id.textViewMinHValue);
        final TextView textViewMinSValue = (TextView)content.findViewById(R.id.textViewMinSValue);
        final TextView textViewMinVValue = (TextView)content.findViewById(R.id.textViewMinVValue);
        final TextView textViewMaxHValue = (TextView)content.findViewById(R.id.textViewMaxHValue);
        final TextView textViewMaxSValue = (TextView)content.findViewById(R.id.textViewMaxSValue);
        final TextView textViewMaxVValue = (TextView)content.findViewById(R.id.textViewMaxVValue);

        textViewMinHValue.setText(pref.getString(getString(R.string.pref_minH_text),"0"));
        textViewMinSValue.setText(pref.getString(getString(R.string.pref_minS_text),"0"));
        textViewMinVValue.setText(pref.getString(getString(R.string.pref_minV_text),"0"));
        textViewMaxHValue.setText(pref.getString(getString(R.string.pref_maxH_text),"0"));
        textViewMaxSValue.setText(pref.getString(getString(R.string.pref_maxS_text),"0"));
        textViewMaxVValue.setText(pref.getString(getString(R.string.pref_maxV_text),"0"));

        seekBarMinH.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        // ツマミに触れたときに呼ばれる
                        if(mListener != null) {
                            mListener.onMinHChanged(progress);

                            textViewMinHValue.setText(String.valueOf(progress));
                        }

                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる
                        Log.v("SeekBar", String.valueOf(seekBar.getProgress()));

                    }
                }
        );

        seekBarMinS.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        // ツマミに触れたときに呼ばれる
                        if(mListener != null) {
                            mListener.onMinSChanged(progress);

                            textViewMinSValue.setText(String.valueOf(progress));
                        }
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる

                    }
                }
        );

        seekBarMinV.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        // ツマミに触れたときに呼ばれる
                        if(mListener != null) {
                            mListener.onMinVChanged(progress);

                            textViewMinVValue.setText(String.valueOf(progress));
                        }
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる

                    }
                }
        );

        seekBarMaxH.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        // ツマミに触れたときに呼ばれる
                        if(mListener != null) {
                            mListener.onMaxHChanged(progress);

                            textViewMaxHValue.setText(String.valueOf(progress));
                        }
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる

                    }
                }
        );

        seekBarMaxS.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        // ツマミに触れたときに呼ばれる
                        if(mListener != null) {
                            mListener.onMaxSChanged(progress);

                            textViewMaxSValue.setText(String.valueOf(progress));
                        }
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる

                    }
                }
        );

        seekBarMaxV.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        // ツマミに触れたときに呼ばれる
                        if(mListener != null) {
                            mListener.onMaxVChanged(progress);

                            textViewMaxVValue.setText(String.valueOf(progress));
                        }
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる

                    }
                }
        );

        return content;
    }

    public void setDegree(int degree){
        if(getView()!=null)
        getView().setRotation(degree);
    }
}

