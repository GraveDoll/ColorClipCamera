package com.gravedoll.colorclipcamera;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
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

/**
 * Created by software on 2014/06/23.
 */
public class FilterDialogFragment extends DialogFragment{
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

    int dialogWidth;
    int dialogHeight;

    public interface OnFilterChangeListener {
        public void onFilterChanged(int filterNumber);

    }

    private OnFilterChangeListener mListener;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnFilterChangeListener == false) {
            throw new ClassCastException("activity が OnSeekBarChangedListener を実装していません.");
        }

        mListener = ((OnFilterChangeListener) activity);
    }

    public static FilterDialogFragment getInstance() {
        FilterDialogFragment f = new FilterDialogFragment();
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
        listView = (ListView)view.findViewById(R.id.listViewAbout);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //relativeLayout= (RelativeLayout) view.findViewById(R.id.relativeFiler);
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

        dialogWidth = (int) (700);
        dialogHeight = (int) (400);

        lp.width = dialogWidth;
        lp.height = dialogHeight;

        lp.gravity = Gravity.BOTTOM;
        dialog.getWindow().setAttributes(lp);


        list = new ArrayList<ListItem>();
        list.add(new ListItem(NORMAL_ITEM, "オリジナル",""));
        list.add(new ListItem(NORMAL_ITEM, "モノクロ",""));
        list.add(new ListItem(NORMAL_ITEM, "セピア",""));
        list.add(new ListItem(NORMAL_ITEM, "ぼかし",""));

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
                mListener.onFilterChanged(position);

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

}
