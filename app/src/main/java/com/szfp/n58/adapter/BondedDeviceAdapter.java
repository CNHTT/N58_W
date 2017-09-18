
package com.szfp.n58.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.szfp.n58.R;
import com.szfp.n58.entity.BlueDevice;

import java.util.List;


public class BondedDeviceAdapter extends BaseAdapter {
    private List<BlueDevice> list;
    private LayoutInflater mInflater;

    public BondedDeviceAdapter(Context context, List<BlueDevice> device) {
        list = device;
        mInflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return list.size();
    }

    public Object getItem(int position) {
        return list.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public int getItemViewType(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        BlueDevice item = list.get(position);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item, null);
            viewHolder = new ViewHolder((View) convertView.findViewById(R.id.list_child),
                    (TextView) convertView.findViewById(R.id.msg));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String deviceName = item.getDeviceName();
        viewHolder.msg.setText(deviceName);
        return convertView;
    }

    class ViewHolder {
        protected View child;
        protected TextView msg;

        public ViewHolder(View child, TextView msg) {
            this.child = child;
            this.msg = msg;

        }
    }
}
