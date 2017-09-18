package com.szfp.n58.widget;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.newpos.mpos.iInterface.IDevice;
import com.newpos.mpos.protocol.bluetooth.BTController;
import com.newpos.mpos.tools.BaseUtility;
import com.szfp.n58.R;
import com.szfp.n58.adapter.BondedDeviceAdapter;
import com.szfp.n58.config.AppConfig;
import com.szfp.n58.entity.BlueDevice;
import com.szfp.n58.utils.WidgetUtils;

import java.util.List;

/**
 * author：ct on 2017/9/18 10:50
 * email：cnhttt@163.com
 */


public class DeviceDialog extends Dialog implements android.view.View.OnClickListener,
        OnItemClickListener {
        private static final String TAG = "DeviceDialog";

        /**
         * 打开设备标记
         */
        private static final int OPEN_FLAG = 0;

        private ListView bondedListView;
        private List<BlueDevice> devices;

        private BondedDeviceAdapter bondedDeviceAdapter;
        private Context context;
        private Button bluetoothSetBtn;
        private Button cancelBtn;
        private TextView noconnectView;
        private Dialog connectDialog;

        public static BluetoothDevice myDevice;

        private IDevice btController;

        public Handler getDeviceHandler() {
            return deviceChangeHanler;
        }

        private Handler deviceHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (connectDialog != null) {
                    connectDialog.dismiss();
                }
                switch (msg.what) {
                    case OPEN_FLAG:
                        if (msg.arg1 < 0) {
                            WidgetUtils.mToast(context, "Open Failure");
                        } else {
                            WidgetUtils.mToast(context, "Open Sucuess");
                            AppConfig.deviceDialog.dismiss();
                            AppConfig.deviceDialogShowing = false;
                        }
                        break;

                    default:
                        break;
                }
            }

        };

        public Handler deviceChangeHanler = new Handler() {

            @SuppressWarnings("unchecked")
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                devices = (List<BlueDevice>) msg.obj;
                if (devices.size() == 0) {
                    noconnectView.setVisibility(View.VISIBLE);
                } else {
                    noconnectView.setVisibility(View.GONE);
                }
                bondedDeviceAdapter.notifyDataSetChanged();
            }

        };

        public DeviceDialog(Context context, List<BlueDevice> devices) {
            super(context);
            this.devices = devices;
            connectDialog = WidgetUtils.getMyDialog(context.getString(R.string.connecting), context);
        }

        public DeviceDialog(Context context, int theme, List<BlueDevice> devices) {
            super(context, theme);
            this.context = context;
            this.devices = devices;
            connectDialog = WidgetUtils.getMyDialog(
                    context.getString(R.string.connecting), context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.bonded_devices);
            bondedListView = (ListView) findViewById(R.id.bonded_list);
            bluetoothSetBtn = (Button) findViewById(R.id.set_btn);
            cancelBtn = (Button) findViewById(R.id.cancel);
            noconnectView = (TextView) findViewById(R.id.no_connect);

            bondedDeviceAdapter = new BondedDeviceAdapter(context, devices);
            bondedListView.setAdapter(bondedDeviceAdapter);

            if (devices.size() == 0) {
                noconnectView.setVisibility(View.VISIBLE);
            } else {
                noconnectView.setVisibility(View.GONE);
            }
            bluetoothSetBtn.setOnClickListener(this);
            cancelBtn.setOnClickListener(this);
            bondedListView.setOnItemClickListener(this);
            // 设置透明度
            WindowManager.LayoutParams lp = this.getWindow().getAttributes();
            lp.alpha = 0.9f;
            this.getWindow().setAttributes(lp);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.set_btn:
                    Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    context.startActivity(intent);
                    break;
                case R.id.cancel:
                    this.dismiss();
                    AppConfig.deviceDialogShowing = false;
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String address = ((BlueDevice) devices.get(position)).getAddress();
            BluetoothDevice btDev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
            if (btDev.getBondState() == BluetoothDevice.BOND_BONDED) {// 已配对
                myDevice = btDev;
                if (!address.equals("null")) {
                    connectDialog.show();
                    new Thread() {
                        @Override
                        public void run() {
                            openDeivce(myDevice);
                        }

                    }.start();
                } else {
                    Toast.makeText(context, "address is null !", Toast.LENGTH_SHORT).show();
                }
            }
        }

        /**
         * 打开设备
         *
         * @param device
         */
        public void openDeivce(BluetoothDevice device) {
            Message msg = new Message();
            int ret = 0;

            btController = BTController.getInstance(device);
            // 打开设备
            ret = btController.open(IDevice.CommunicationMode.BT_SPP);
            if (ret < 0) {
                BaseUtility.LOGE(TAG, "Open error!!!");
                AppConfig.deviceOpen = false;
            } else {
                BaseUtility.LOGE(TAG, "Open success!!!");
                AppConfig.deviceOpen = true;
            }
            msg.what = OPEN_FLAG;
            msg.arg1 = ret;
            deviceHandler.sendMessage(msg);
        }

        public IDevice getBTController() {
            return btController;
        }

    }
