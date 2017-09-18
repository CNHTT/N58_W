package com.szfp.n58.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.szfp.n58.R;
import com.szfp.n58.entity.LabelInfo;

import java.util.List;

/**
 * author：ct on 2017/9/18 10:28
 * email：cnhttt@163.com
 */


public class TitleAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private List<LabelInfo> labelInfos;;

    public TitleAdapter(Context context, List<LabelInfo> laInfos) {
        super();
        this.labelInfos = laInfos;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        if (labelInfos != null)
            return labelInfos.size();
        else
            return 0;
    }

    @Override
    public Object getItem(int position) {
        return labelInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView iconView;
        TextView labelView;

        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.simple_list_item_1, null);
        }
        iconView = (ImageView) view.findViewById(R.id.icon);
        labelView = (TextView) view.findViewById(R.id.title);
        if (labelInfos != null) {
            labelView.setText(labelInfos.get(position).getLabelValue());
        }
        return view;
    }

}
