package com.szfp.n58;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;

import com.newpos.app.cmd.Instruction;
import com.newpos.app.cmd.SYS;
import com.newpos.app.entity.DataResponse;
import com.newpos.app.function.OperateMPOS;
import com.newpos.app.function.SysFunction;
import com.newpos.mpos.iInterface.ICommunication;
import com.newpos.mpos.iInterface.IDevice;
import com.newpos.mpos.protocol.audio.AudioController;
import com.newpos.mpos.protocol.audio.AudioTransfer;
import com.newpos.mpos.protocol.bluetooth.BtTransfer;
import com.newpos.mpos.tools.BaseUtility;
import com.newpos.mpos.tools.BaseUtils;
import com.szfp.n58.adapter.TitleAdapter;
import com.szfp.n58.config.AppConfig;
import com.szfp.n58.entity.BlueDevice;
import com.szfp.n58.entity.LabelInfo;
import com.szfp.n58.utils.WidgetUtils;
import com.szfp.n58.widget.DeviceDialog;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class SystemScreen extends AppCompatActivity {
    private static final String TAG = SystemScreen.class.getSimpleName();

    private static final String PREFIX = "";

    private List<BlueDevice> devices;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Set<BluetoothDevice> bondedDevices;

    private Dialog requestingDialog;
    private Handler deviceChangeHanler;

    private ListView mListView;
    private List<LabelInfo> labelInfos = new ArrayList<LabelInfo>();

    private Context cxt;

    private SysFunction sysFunction;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_screen);
        cxt = this;

        setupViews();
    }

    private void setupViews() {

        mListView = (ListView) findViewById(R.id.list1);

        LabelInfo.resetIndex();
        labelInfos.add(new LabelInfo(getString(R.string.read_SN)));
        labelInfos.add(new LabelInfo(getString(R.string.read_pos_time)));
        labelInfos.add(new LabelInfo(getString(R.string.set_pos_time)));
        labelInfos.add(new LabelInfo(getString(R.string.pos_reboot)));
        labelInfos.add(new LabelInfo(getString(R.string.pos_shutdown)));
        labelInfos.add(new LabelInfo(getString(R.string.pos_beep)));
        labelInfos.add(new LabelInfo(getString(R.string.pos_LED)));
        labelInfos.add(new LabelInfo(getString(R.string.read_battery)));
        labelInfos.add(new LabelInfo(getString(R.string.read_soft_verison)));

        TitleAdapter adapter = new TitleAdapter(this, labelInfos);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new OnItemListClickListener());

        requestingDialog = WidgetUtils.getMyDialog(getString(R.string.requesting), this);
        devices = new ArrayList<BlueDevice>();
        AppConfig.deviceDialog = new DeviceDialog(this, R.style.MyDialogStyle, devices);

    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 3);
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (AppConfig.isBTChannel && !AppConfig.deviceOpen) {
            popupDeviceDialog();
        } else {
            if (!AppConfig.deviceOpen) {
                AudioController adController = AudioController.getInstance();
                int ret = adController.open(IDevice.CommunicationMode.AUDIO);
                if (ret < 0) {
                    WidgetUtils.mToast(cxt, getString(R.string.plug_in));
                }
                AppConfig.deviceOpen = ret == 0;
            }
        }
    }

    /**
     * 弹出设备蓝牙设备对话框
     */
    private void popupDeviceDialog() {
        if (AppConfig.deviceDialog == null) {
            AppConfig.deviceDialog = new DeviceDialog(this, R.style.MyDialogStyle, devices);
        }
        if (!AppConfig.deviceDialogShowing) {
            AppConfig.deviceDialogShowing = true;
            AppConfig.deviceDialog.show();
        }

        // 已绑定的设备
        devices.clear();
        bondedDevices = mBluetoothAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice device : bondedDevices) {

                BlueDevice bd = new BlueDevice();
                bd.setDeviceName(device.getName());
                bd.setAddress(device.getAddress());
                devices.add(bd);

            }
        }
        deviceChangeHanler = AppConfig.deviceDialog.getDeviceHandler();
        Message msg = new Message();
        msg.obj = devices;
        deviceChangeHanler.sendMessage(msg);
    }

    @Override
    protected void onDestroy() {
        BaseUtility.LOGE(TAG, "APP　exit!!!!!!!!!");
        AppConfig.deviceDialogShowing = false;

        if (AppConfig.deviceOpen) {
            close();
        }

        super.onDestroy();
    }

    private void close() {
        AppConfig.deviceOpen = false;
        // 释放IO资源
        if (AppConfig.communication != null) {
            AppConfig.communication.release();
            AppConfig.communication = null;
        }
        IDevice btController = AppConfig.deviceDialog.getBTController();
        // 关闭通道
        if (AppConfig.isBTChannel) {
            if (btController != null)
                btController.close();
        } else {
            AudioController.getInstance().close();
        }
    }

    String current = null;

    long startT;

    /**
     * @function 功能选项
     * @author panjp
     * @time 2014-9-24
     */
    class OnItemListClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            if (AppConfig.isBTChannel && !AppConfig.deviceOpen) {
                popupDeviceDialog();
                return;
            }

            if (AppConfig.communication == null) {
                if (AppConfig.isBTChannel) {
                    if (AppConfig.deviceOpen) {
                        AppConfig.communication = BtTransfer.getInstance();
                    }
                } else {
                    if (AppConfig.deviceOpen) {
                        AppConfig.communication = AudioTransfer.getInstance();
                    }
                }
            }

            // 弹出请求框
            if (requestingDialog != null) {
                if (!requestingDialog.isShowing()) {
                    requestingDialog.show();
                }
            }

            sysFunction = new SysFunction(cxt, AppConfig.communication, sysDataHandler);

            switch (arg2) {
                case 0:// 获取序列号
                    sysFunction.toGetSN();
                    break;

                case 1:// 读POS时间
                    sysFunction.toGetPOSTime();
                    break;

                case 2:// 设置POS时间
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");// 设置日期格式
                    String currentDatetime = df.format(new Date());
                    current = BaseUtils.stringPattern(currentDatetime, "yyyyMMddHHmmss",
                            "yyyy-MM-dd HH:mm:ss");
                    byte[] datetime = BaseUtils.convertBCD2ByteArr(currentDatetime);
                    sysFunction.toSetPOSTime(datetime);
                    break;

                case 3:// POS重启
                    sysFunction.toRebootPOS();
                    break;

                case 4:// POS关机
                    sysFunction.toShutdownPOS();
                    break;

                case 5:// 蜂鸣
                    beepSettingDialog();
                    break;

                case 6:// 背光
                    ledSettingDialog();
                    break;

                case 7:// 读电池电量
                    sysFunction.toGetBattery();
                    break;

                case 8:// 读软件版本号
                    sysFunction.toGetSoftVersion();
                    break;

                default:
                    break;
            }
        }
    }

    private Handler sysDataHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            BaseUtils.vibrate(SystemScreen.this, 200);

            // 关闭请求框
            if (requestingDialog != null) {
                if (requestingDialog.isShowing()) {
                    requestingDialog.dismiss();
                }
            }
            // 发送失败
            if (msg.arg1 == ICommunication.SEND_FAILURE) {
                WidgetUtils.showResult(cxt, getString(R.string.tips),
                        getString(R.string.send_failure));
                close();
                return;
            }
            // 操作超时
            if (msg.arg1 == ICommunication.OPER_TIMEOUT) {
                WidgetUtils.showResult(cxt, getString(R.string.tips),
                        getString(R.string.req_timeout));
                return;
            }

            // MPOS端应答指令执行结果或响应数据
            DataResponse dataRsp = (DataResponse) msg.obj;
            if (dataRsp == null) {
                WidgetUtils.showResult(cxt, getString(R.string.tips),
                        getString(R.string.cmd_read_error));
                return;
            }

            // 指令执行结果
            int ret = dataRsp.getRspResult();

            if (ret == Instruction.Code.INS_SUCCESS) {
                SYS sys = SYS.getInstance(msg.what);
                switch (sys) {
                    case GET_SN:
                        String sn = BCDArr2String(dataRsp.getDataContent());
                        sysFunction.setMposSN(sn);
                        WidgetUtils.showResult(cxt, getString(R.string.read_SN), sn);
                        break;

                    case SYS_TIME_READ:

                        String mposTime = BaseUtils.stringPattern(
                                BaseUtils.byteArr2HexStr(dataRsp.getDataContent()),
                                "yyyyMMddHHmmss", "yyyy-MM-dd HH:mm:ss");
                        WidgetUtils.showResult(cxt, getString(R.string.read_pos_time), mposTime);
                        break;

                    case SYS_TIME_SET:
                        WidgetUtils.showResult(cxt, getString(R.string.set_pos_time), current
                                + "\n"
                                + getString(R.string.success));
                        break;

                    case SYS_REBOOT:
                        WidgetUtils.showResult(cxt, getString(R.string.pos_reboot),
                                getString(R.string.success));
                        if (AppConfig.isBTChannel) {
                            close();
                        }
                        break;

                    case SYS_SHUTDOWN:
                        WidgetUtils.showResult(cxt, getString(R.string.pos_shutdown),
                                getString(R.string.success));
                        if (AppConfig.isBTChannel) {
                            close();
                        }
                        break;

                    case SYS_BEEP:
                        WidgetUtils.showResult(cxt, getString(R.string.pos_beep),
                                getString(R.string.success));
                        SharedPreferences.Editor editor = beepSP.edit();
                        editor.putInt("beepHz", beepHz);
                        editor.putInt("beepMs", beepMs);
                        editor.commit();
                        break;

                    case SYS_LED:
                        WidgetUtils.showResult(cxt, getString(R.string.pos_LED),
                                getString(R.string.success));
                        break;

                    case SYS_BATTERY_READ:
                        // 电量百分比
                        int batteryPercent = 0;
                        // 电池状态
                        int batteryState = -1;

                        byte[] data = dataRsp.getDataContent();

                        if (data.length >= 2) {
                            batteryPercent = data[0];
                            batteryState = data[1];
                        }

                        String energyPercent = String.format(getString(R.string.energy_percent),
                                batteryPercent);
                        String battery = "";
                        if (batteryState == 0) {
                            battery = getString(R.string.only_battery_supply);
                        } else if (batteryState == 1) {
                            battery = getString(R.string.charging);
                        } else if (batteryState == 2) {
                            battery = getString(R.string.full_charged);
                        } else if (batteryState == 3) {
                            battery = getString(R.string.only_power_supply);
                        }

                        WidgetUtils.showResult(cxt, getString(R.string.read_battery), energyPercent
                                + "% ," + battery);

                        break;

                    case SYS_SOFTWAREINFO_READ:
                        String version = BCDArr2String(dataRsp.getDataContent());
                        WidgetUtils.showResult(cxt, getString(R.string.read_soft_verison), version);
                        break;

                    default:
                        break;
                }
            } else {
                WidgetUtils.showResult(cxt, getString(R.string.tips), MainActivity.codeMsg.get(ret));
            }
        }

        /**
         * 把BCD数组转化String
         *
         * @param dataResp
         * @return
         */
        private String BCDArr2String(byte[] dataResp) {
            String str = null;
            try {
                str = new String(dataResp, OperateMPOS.CHARSET);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return str;
        }

    };

    private int beepHz;
    private int beepMs;
    private SharedPreferences beepSP;

    /**
     * 弹出蜂鸣设置对话框
     */
    public void beepSettingDialog() {
        if (requestingDialog != null && requestingDialog.isShowing()) {
            requestingDialog.show();
        }
        beepSP = getSharedPreferences("beep", MODE_PRIVATE);
        int HZ = beepSP.getInt("beepHz", 0);
        int MS = beepSP.getInt("beepMs", 0);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_beep_setting, null);

        // 蜂鸣器频率
        final EditText hzEdit = (EditText) view.findViewById(R.id.edit_hz);
        hzEdit.setText(String.valueOf(HZ));

        // 蜂鸣持续时间
        final EditText msEdit = (EditText) view.findViewById(R.id.edit_ms);
        msEdit.setText(String.valueOf(MS));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.pos_beep))
                .setView(view)
                .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        beepHz = Integer.parseInt(hzEdit.getText().toString());
                        beepMs = Integer.parseInt(msEdit.getText().toString());

                        byte[] beepHzArr = BaseUtils.int2ByteArr(beepHz);
                        byte[] beepMsArr = BaseUtils.int2ByteArr(beepMs);

                        byte[] beep = new byte[8];
                        System.arraycopy(beepHzArr, 0, beep, 0, 4);
                        System.arraycopy(beepMsArr, 0, beep, 4, 4);

                        // 弹出请求框
                        if (requestingDialog != null) {
                            if (!requestingDialog.isShowing()) {
                                requestingDialog.show();
                            }
                        }
                        // 设置蜂鸣器频率和持续时间
                        sysFunction.toSetBeep(beep);

                    }

                })
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // 关闭请求框
                                if (requestingDialog != null) {
                                    if (requestingDialog.isShowing()) {
                                        requestingDialog.dismiss();
                                    }
                                }
                                arg0.dismiss();
                            }
                        }).show();
    }

    private int ledArg = 0;

    /**
     * 弹出蜂鸣设置对话框
     */
    public void ledSettingDialog() {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_led_setting, null);

        RadioGroup group = (RadioGroup) view.findViewById(R.id.radioGroup);

        final RadioButton keepRadio = (RadioButton) view.findViewById(R.id.keep_ms);
        final RadioButton alwaysDarkRadio = (RadioButton) view.findViewById(R.id.always_dark);
        final RadioButton alwaysBrightRadio = (RadioButton) view.findViewById(R.id.always_bright);

        final TableLayout keepMSLayout = (TableLayout) view.findViewById(R.id.keep_area);
        final EditText keepMSEdit = (EditText) view.findViewById(R.id.edit_ms);

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == alwaysDarkRadio.getId()) {
                    keepMSLayout.setVisibility(View.GONE);
                    ledArg = 0;
                }
                if (checkedId == alwaysBrightRadio.getId()) {
                    keepMSLayout.setVisibility(View.GONE);
                    ledArg = -1;
                }
                if (checkedId == keepRadio.getId()) {
                    keepMSLayout.setVisibility(View.VISIBLE);
                    keepMSEdit.requestFocus();
                    ledArg = 1;
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.pos_LED))
                .setView(view)
                .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (ledArg == 1) {
                            String ms = keepMSEdit.getText().toString();
                            if ("".equals(ms)) {
                                WidgetUtils.mToast(cxt,getString(R.string.keep_time) );
                                return;
                            } else {
                                ledArg = Integer.parseInt(ms);
                            }
                        }

                        byte[] led = BaseUtils.int2ByteArr(ledArg);
                        sysFunction.toSetLED(led);
                    }

                })
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // 关闭请求框
                                if (requestingDialog != null) {
                                    if (requestingDialog.isShowing()) {
                                        requestingDialog.dismiss();
                                    }
                                }
                                arg0.dismiss();
                            }
                        }).show();
    }
}
