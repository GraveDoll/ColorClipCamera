package com.gravedoll.colorclipcamera;

import android.app.DialogFragment;
import android.os.Bundle;

/**
 * Created by software on 2014/05/29.
 */
public class SimpleProgressDialogFragment extends DialogFragment {
    private static SimpleProgressDialog progressDialog = null;

    public static SimpleProgressDialogFragment newInstance(){
        SimpleProgressDialogFragment instance = new SimpleProgressDialogFragment();

        //ダイアログにパラメータを渡す
        //Bundle arguments = new Bundle();
        //arguments.putString("title", title);
        //arguments.putString("message", message);

        //instance.setArguments(arguments);

        return instance;
    }

    //ProgressDialog作成
    @Override
    public SimpleProgressDialog onCreateDialog(Bundle savedInstance){
        if(progressDialog != null)
            return progressDialog;

        //パラメータを取得
        //String title = getArguments().getString("title");
        //String message = getArguments().getString("message");

        progressDialog = new SimpleProgressDialog(getActivity());
       // progressDialog.setTitle(title);
       // progressDialog.setMessage(message);
      //  progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        setCancelable(false);
        return progressDialog;

    }

    //progressDialog取得
    @Override
    public SimpleProgressDialog getDialog(){
        return progressDialog;
    }

    //progressDialog破棄
    @Override
    public void onDestroy(){
        super.onDestroy();
        progressDialog = null;
    }
}
