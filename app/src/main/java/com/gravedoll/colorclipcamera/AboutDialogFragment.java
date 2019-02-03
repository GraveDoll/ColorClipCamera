package com.gravedoll.colorclipcamera;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by GraveDoll on 2014/10/13.
 */
public class AboutDialogFragment extends DialogFragment{
    // Viewタイプ
    // 更新するときはVIEW_TYPE_NUMBERも更新すること
    public final int CATEGORY = 0;
    public final int NORMAL_ITEM = 1;
    // Viewタイプの数
    public final int VIEW_TYPE_NUMBER = 2;

    private List<ListItem> list;
    private ArrayAdapter<ListItem> adapter;
    private String[] data = {""};
    private ListView listView;

    WindowManager.LayoutParams lp;
    //RelativeLayout relativeLayout;
    int dialogWidth;
    int dialogHeight;


    public static AboutDialogFragment getInstance() {
        AboutDialogFragment f = new AboutDialogFragment();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // フルスクリーン設定
        //getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        // 背景を透明にする
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View view = inflater.inflate(R.layout.list_filter, container, false);
        view.setRotation(270);
        listView = (ListView)view.findViewById(R.id.listViewAbout);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());

        return dialog;

    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Dialog dialog = getDialog();
        lp = dialog.getWindow().getAttributes();


        DisplayMetrics metrics = getResources().getDisplayMetrics();

        dialogWidth = (int) 600;
        dialogHeight = (int) 600;

        lp.width = dialogWidth;
        lp.height = dialogHeight;

        dialog.getWindow().setAttributes(lp);

        String versionName = "";
        PackageManager packageManager = getActivity().getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getActivity().getPackageName(), PackageManager.GET_META_DATA);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        list = new ArrayList<ListItem>();
        list.add(new ListItem(CATEGORY, "About"));
        list.add(new ListItem(NORMAL_ITEM, getString(R.string.about_main_version),versionName));
        list.add(new ListItem(NORMAL_ITEM, getString(R.string.about_main_icons),getResources().getString(R.string.about_sub_icons)));
        list.add(new ListItem(NORMAL_ITEM, "Open Source Computer Vision Library",getResources().getString(R.string.lisence_opencv)));



        adapter = new ListAdapter(getActivity(),list);
        //リストをアダプタにセット
        listView.setAdapter(adapter);
        // リストビューのアイテムがクリックされた時に呼び出されるコールバックリスナーを登録します
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                ListView listView = (ListView) parent;
                // クリックされたアイテムを取得します
                //mListener.onFilterChanged(position);
                //Toast.makeText(ListViewSampleActivity.this, item, Toast.LENGTH_LONG).show();
            }
        });

    }



    private class ListAdapter extends ArrayAdapter<ListItem> {
        private LayoutInflater inflater;
        private List<ListItem> items;

        public ListAdapter(Context context, List<ListItem> objects) {
            super(context, 0, objects);
            this.items = objects;
            this.inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return this.items.size();
        }

        @Override
        public int getItemViewType(int position) {
            // 行に応じたViewタイプを返す
            return items.get(position).getViewType();
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_NUMBER; // Viewタイプの数
        }

        @Override
        public boolean areAllItemsEnabled() {
            // falseにしないと選択不可項目の上下にdividerが表示される
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            // 項目を選択不可にする
            return !(getItemViewType(position) == CATEGORY);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolderItem holderItem;
            final ViewHolderCategory holderCategory;
            // Viewの割り当て
            switch (getItemViewType(position)) {

                case CATEGORY:// カテゴリー
                    if (convertView == null) {
                        convertView = inflater.inflate(
                                R.layout.list_category, parent, false);

                        holderCategory = new ViewHolderCategory();
                        holderCategory.textViewCategory = (TextView) convertView
                                .findViewById(R.id.textViewListCategory);
                        convertView.setTag(holderCategory);
                    } else {
                        holderCategory = (ViewHolderCategory) convertView.getTag();
                    }
                    final ListItem itemCategory = getItem(position);
                    holderCategory.textViewCategory.setText(itemCategory
                            .getCategory());
                    break;


                case NORMAL_ITEM:// 通常の項目
                    if (convertView == null) {
                        convertView = inflater.inflate(R.layout.list_contents,
                                parent, false);
                        holderItem = new ViewHolderItem();
                        holderItem.textViewTitle = (TextView) convertView
                                .findViewById(R.id.textViewTitle);
                        holderItem.textViewSummary = (TextView) convertView
                                .findViewById(R.id.textViewSumamry);

                        convertView.setTag(holderItem);
                    } else {
                        holderItem = (ViewHolderItem) convertView.getTag();
                    }
                    ListItem item = getItem(position);

                    holderItem.textViewTitle.setText(item.getTitle());
                    holderItem.textViewSummary.setText(item.getSummary());


                    Pattern patternTitle = Pattern.compile("CC BY");
                    Pattern patternSubTitle = Pattern.compile("Material Design Icons");
                    final String strUrl = "http://creativecommons.org/licenses/by/4.0/";
                    final String strUrl2 = "https://github.com/google/material-design-icons";

                    Linkify.TransformFilter filter = new Linkify.TransformFilter() {
                        @Override
                        public String transformUrl(Matcher match, String url) {
                            return strUrl;
                        }
                    };
                    Linkify.TransformFilter filter2 = new Linkify.TransformFilter() {
                        @Override
                        public String transformUrl(Matcher match, String url) {
                            return strUrl2;
                        }
                    };
                    Linkify.addLinks(holderItem.textViewSummary, patternTitle, strUrl, null, filter);
                    Linkify.addLinks(holderItem.textViewSummary, patternSubTitle, strUrl2, null, filter2);

                    break;

            }
            return convertView;

        }
    }

    public class ViewHolderItem {
        TextView textViewTitle;
        TextView textViewSummary;
    }


    public class ViewHolderCategory {
        TextView textViewCategory;
    }

    private class ListItem {
        private int viewType = 0;
        private String category = "";
        private String title = "";
        private String summary = "";

        public ListItem() {
        }

        // カテゴリ用コンストラクタ
        public ListItem(int viewType, String category) {
            this.viewType = viewType;
            this.category = category;
        }

        // 通常の項目用コンストラクタ
        public ListItem(int viewType, String title, String summary) {
            this.viewType = viewType;
            this.title = title;
            this.summary = summary;
        }


        public int getViewType() {
            return this.viewType;
        }

        public String getCategory() {
            return this.category;
        }

        public String getTitle() {
            return this.title;
        }

        public String getSummary() {
            return this.summary;
        }

    }

    public void setDegree(int degree) {
        if (getView() != null)
            getView().setRotation(degree);






    }
}

