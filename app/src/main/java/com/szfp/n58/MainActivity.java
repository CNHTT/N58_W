package com.szfp.n58;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.newpos.app.cmd.Instruction.Code;
import com.newpos.mpos.iInterface.ICommunication;
import com.newpos.mpos.iInterface.IDevice;
import com.newpos.mpos.iInterface.IDevice.CommunicationMode;
import com.newpos.mpos.protocol.audio.AudioController;
import com.newpos.mpos.protocol.audio.AudioTransfer;
import com.newpos.mpos.protocol.bluetooth.BTController;
import com.newpos.mpos.protocol.bluetooth.BtTransfer;
import com.szfp.n58.config.AppConfig;
import com.szfp.n58.entity.BlueDevice;
import com.szfp.n58.widget.DeviceDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {
    private static final String HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
    AudioController adController;
    private String ip;
    private String port;
    private SharedPreferences settings;




    // public static boolean plugIn = false;

    class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("state")) {
                if (intent.getIntExtra("state", 0) == 0) {// 耳机拔出,选择蓝牙通道
                    AppConfig.deviceOpen = false;
                    AppConfig.isBTChannel = true;

                    if (adController != null) {// 关闭音频
                        ((ICommunication) AudioTransfer.getInstance()).release();
                        adController.close();
                    }

                    // 打开蓝牙
                    devices = new ArrayList<BlueDevice>();
                    if (AppConfig.deviceDialog == null) {
                        AppConfig.deviceDialog = new DeviceDialog(MainActivity.this,
                                R.style.MyDialogStyle, devices);
                    }
                    popupDeviceDialog();

                } else if (intent.getIntExtra("state", 0) == 1) {// 耳机插入,选择音频通道
                    AppConfig.isBTChannel = false;

                    if (AppConfig.deviceDialog != null) {
                        AppConfig.deviceDialog.dismiss();
                        AppConfig.deviceDialogShowing = false;
                    }

                    if (DeviceDialog.myDevice != null && AppConfig.deviceOpen) { // 关闭蓝牙
                        ((ICommunication) BtTransfer.getInstance()).release();
                        ((IDevice) BTController.getInstance(DeviceDialog.myDevice)).close();
                    }

                    // 打开音频
                    adController = AudioController.getInstance();
                    int ret = adController.open(CommunicationMode.AUDIO);
                    AppConfig.deviceOpen = ret == 0;

                    setMaxVolume(MainActivity.this);
                }

                AppConfig.communication = null;
            }
        }

        /**
         * 设置最大系统音量
         */
        private void setMaxVolume(Context context) {
            AudioManager mAudioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = 100;// 进入和刷卡器通信后，设置音量最大值
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        }
    }

    /**
     * 主界面的tabhost
     */
    private TabHost mTabHost;

    /**
     * tab页的TabWidget
     */
    private TabWidget mTabWidget;

    private int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    /**
     * 用来保存菜单中子view的容器
     */
    List<View> list = new ArrayList<View>();

    public static Map<Byte, String> codeMsg = new HashMap<Byte, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        codeMsg.put(Code.PKT_INVALID, getString(R.string.pkt_invalid));
        codeMsg.put(Code.INS_UNDEFINED, getString(R.string.ins_undefined));
        codeMsg.put(Code.INS_TYPE_UNDEFINED, getString(R.string.ins_type_undefined));
        codeMsg.put(Code.INS_CODE_UNDEFINED, getString(R.string.ins_code_undefined));
        codeMsg.put(Code.PERMISSION_DENIED, getString(R.string.permission_denied));
        codeMsg.put(Code.SESSION_KEY_INVALID, getString(R.string.session_key_invalid));
        codeMsg.put(Code.INS_VERSION_UNSUPPORTED, getString(R.string.ins_ver_unsupported));
        codeMsg.put(Code.INS_FAILED, getString(R.string.ins_failed));

        codeMsg.put(Code.COMMUNICATION_FAILED, getString(R.string.communication_failed));
        codeMsg.put(Code.TERMINAL_NOT_REGISTRATION, getString(R.string.terminal_not_registration));
        codeMsg.put(Code.TERMINAL_NOT_INIT, getString(R.string.terminal_not_init));
        codeMsg.put(Code.TERMINEL_REPEAT_INIT, getString(R.string.terminal_repeat_init));
        codeMsg.put(Code.PKT_DATA_LOSS, getString(R.string.pkt_data_loss));
        codeMsg.put(Code.PKT_DATA_INVALID, getString(R.string.pkt_invalid));
        codeMsg.put(Code.SALE_CANCEL, getString(R.string.pkt_invalid));
        codeMsg.put(Code.REPEAT_SIGNIN, getString(R.string.repeat_signin));
        codeMsg.put(Code.PED_ACTION_ERROR, getString(R.string.PED_action_error));

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabWidget = (TabWidget) findViewById(android.R.id.tabs);
        mTabHost.setOnTabChangedListener(this);

        registerHeadsetPlugReceiver(new HeadsetPlugReceiver());

        settings = getSharedPreferences("setting", MODE_PRIVATE);
        settingDialog = new SettingDialog(this, R.style.MyDialogStyle);
        ip = settings.getString("ip", "39.108.61.105");
        port = settings.getString("port", "80");
        App.setIp(ip);
        App.setPort(port);

        // 初始化页面
        setupViews();
        isOK();
    }
    public void isOK(){
        int osVersion = Integer.valueOf(android.os.Build.VERSION.SDK);
        if (osVersion>22){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                //申请WRITE_EXTERNAL_STORAGE权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            }else{
            }
        }else{
            //如果SDK小于6.0则不去动态申请权限
        }
//        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)
//                != PackageManager.PERMISSION_GRANTED) {
//            //申请WRITE_EXTERNAL_STORAGE权限
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE},
//                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
//        }else{
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE},
//                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
//        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {

            Toast.makeText(getApplicationContext(),"success",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(),"error",Toast.LENGTH_SHORT).show();
        }
    }




    private void registerHeadsetPlugReceiver(HeadsetPlugReceiver headsetPlugReceiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HEADSET_PLUG);
        registerReceiver(headsetPlugReceiver, intentFilter);
    }

    /**
     * 初始化页面
     */
    private void setupViews() {
        // 系统类
        setIndicator(0, new Intent(this, SystemScreen.class), R.string.system);

        // MIS交易类
        setIndicator(1, new Intent(this, MisScreen.class), R.string.mis);
    }

    /**
     * 初始化标签
     *
     * @param tabID 标签的ID
     * @param intent 跳转的对象
     * @param StringId 标签的文
     */
    private void setIndicator(int tabID, Intent intent, int StringId) {
        View localView = LayoutInflater.from(this.mTabHost.getContext()).inflate(
                R.layout.homepage_bottom_menu, null);
        TextView tv = (TextView) localView.findViewById(R.id.main_activity_tab_image);
        tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        tv.setText(StringId);
        tv.setTextSize(20);

        DisplayMetrics dm = new DisplayMetrics();

        getWindowManager().getDefaultDisplay().getMetrics(dm);
        Float density = dm.density;

        if (density == 1.0f) {
            tv.setPadding(2, 0, 0, 2);
            tv.setCompoundDrawablePadding(-19);
        } else if (density == 1.5f) {
            tv.setPadding(0, 0, 0, 4);
            tv.setCompoundDrawablePadding(-28);

        } else if (density == 0.75f) {
            tv.setPadding(0, 0, 0, 1);
            tv.setCompoundDrawablePadding(-14);
        } else if (density == 2.0f) {
            tv.setPadding(0, 0, 0, 6);
            tv.setCompoundDrawablePadding(-38);
        }

        if (tabID == 0) {
            tv.setTextColor(getResources().getColor(R.color.main_tab_text));
        }
        String str = String.valueOf(tabID);
        // 创建tabSpec
        TabHost.TabSpec localTabSpec = mTabHost.newTabSpec(str).setIndicator(localView)
                .setContent(intent);
        // 加载tabSpec
        mTabHost.addTab(localTabSpec);
        // 保存tab菜单中子菜单
        list.add(tv);
    }

    @Override
    public void onTabChanged(String tabId) {
        int tabID = Integer.valueOf(tabId);
        for (int i = 0; i < mTabWidget.getChildCount(); i++) {
            if (i == tabID) {
                if (list.size() != 0) {
                    if (i == 0) {
                        ((TextView) list.get(i))
                                .setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        ((TextView) list.get(i)).setTextColor(getResources().getColor(
                                R.color.main_tab_text));
                    } else if (i == 1) {
                        ((TextView) list.get(i))
                                .setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        ((TextView) list.get(i)).setTextColor(getResources().getColor(
                                R.color.main_tab_text));
                    } else if (i == 2) {
                        ((TextView) list.get(i))
                                .setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        ((TextView) list.get(i)).setTextColor(getResources().getColor(
                                R.color.main_tab_text));

                    }
                }

            } else {
                mTabWidget.getChildAt(Integer.valueOf(i)).setBackgroundDrawable(null);
                if (list.size() != 0) {
                    if (i == 0 && i != tabID) {
                        ((TextView) list.get(i))
                                .setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        ((TextView) list.get(i)).setTextColor(Color.GRAY);
                    } else if (i == 1 && i != tabID) {
                        ((TextView) list.get(i))
                                .setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        ((TextView) list.get(i)).setTextColor(Color.GRAY);
                    }

                    else if (i == 2 && i != tabID) {
                        ((TextView) list.get(i))
                                .setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        ((TextView) list.get(i)).setTextColor(Color.GRAY);
                    }

                }
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 退出程序
        ActivityManager activityMgr = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        activityMgr.restartPackage(getPackageName());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
        }
        return super.onKeyDown(keyCode, event);
    }

    List<BlueDevice> devices;
    private Set<BluetoothDevice> bondedDevices;

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Handler deviceChangeHanler;

    /**
     * 弹出设备蓝牙设备对话框
     */
    private void popupDeviceDialog() {
        if (AppConfig.deviceDialog != null && !AppConfig.deviceDialogShowing) {
            AppConfig.deviceDialog.show();
            AppConfig.deviceDialogShowing = true;
        }
        // 已绑定的设备
        devices.clear();
        bondedDevices = mBluetoothAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice device : bondedDevices) {
                // if (device.getName().contains("pos") ||
                // device.getName().contains(PREFIX)) {
                BlueDevice bd = new BlueDevice();
                bd.setDeviceName(device.getName());
                bd.setAddress(device.getAddress());
                devices.add(bd);
                // }
            }
        }
        deviceChangeHanler = AppConfig.deviceDialog.getDeviceHandler();
        Message msg = new Message();
        msg.obj = devices;
        deviceChangeHanler.sendMessage(msg);
    }

    private SettingDialog settingDialog;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Menu.FIRST, 0, getString(R.string.setting_title)).setIcon(null);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Menu.FIRST:
                if (settingDialog != null) {
                    settingDialog.show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);

    }

    class SettingDialog extends Dialog implements android.view.View.OnClickListener {
        EditText ipEditText;
        EditText portEditText;
        Button ok;
        Button cancel;

        public SettingDialog(Context context) {
            super(context);
        }

        public SettingDialog(Context context, int theme) {
            super(context, theme);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.ip_port_setting);
            ipEditText = (EditText) findViewById(R.id.ip);
            ipEditText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
            ipEditText.requestFocus();
            portEditText = (EditText) findViewById(R.id.port);
            portEditText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
            ok = (Button) findViewById(R.id.sure);
            ok.setOnClickListener(this);
            cancel = (Button) findViewById(R.id.cancel);
            cancel.setOnClickListener(this);

            ipEditText.setText(ip);
            portEditText.setText(port);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.sure:
                    ip = ipEditText.getText().toString();
                    port = portEditText.getText().toString();
                    if (!ipCheck(ip)) {
                        Toast.makeText(MainActivity.this, getString(R.string.invalid_ip),
                                Toast.LENGTH_LONG).show();
                        ipEditText.requestFocus();
                        return;
                    }
                    if ("".equals(port)) {
                        Toast.makeText(MainActivity.this, getString(R.string.invalid_port),
                                Toast.LENGTH_LONG).show();
                        portEditText.requestFocus();
                        return;
                    }

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("ip", ip);
                    editor.putString("port", port);
                    editor.commit();
                    App.setIp(ip);
                    App.setPort(port);
                    Toast.makeText(MainActivity.this, getString(R.string.setting_success),
                            Toast.LENGTH_LONG).show();
                    this.dismiss();
                    break;

                case R.id.cancel:
                    this.dismiss();
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * 验证IP地址
     *
     * @param text
     * @return
     */
    public boolean ipCheck(String text) {
        if (text != null && !"".equals(text)) {
            // 定义正则表达式
            String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
            // 判断ip地址是否与正则表达式匹配
            if (text.matches(regex)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

}
