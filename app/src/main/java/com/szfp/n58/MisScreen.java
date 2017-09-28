package com.szfp.n58;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.Response;
import com.newpos.app.cmd.Instruction;
import com.newpos.app.cmd.MIS;
import com.newpos.app.entity.DataResponse;
import com.newpos.app.function.MisFunction;
import com.newpos.app.function.OperateMPOS;
import com.newpos.app.function.SysFunction;
import com.newpos.mpos.iInterface.ICommunication;
import com.newpos.mpos.iInterface.IDevice;
import com.newpos.mpos.iInterface.IDevice.CommunicationMode;
import com.newpos.mpos.protocol.audio.AudioController;
import com.newpos.mpos.protocol.audio.AudioTransfer;
import com.newpos.mpos.protocol.bluetooth.BtTransfer;
import com.newpos.mpos.tools.BCDUtils;
import com.newpos.mpos.tools.BaseUtility;
import com.newpos.mpos.tools.BaseUtils;
import com.szfp.n58.adapter.TitleAdapter;
import com.szfp.n58.callback.DialogCallback;
import com.szfp.n58.callback.OnShowPrintCallBack;
import com.szfp.n58.config.AppConfig;
import com.szfp.n58.data.TransactionData;
import com.szfp.n58.entity.BlueDevice;
import com.szfp.n58.entity.LabelInfo;
import com.szfp.n58.entity.ReturnData;
import com.szfp.n58.utils.ContextUtils;
import com.szfp.n58.utils.PrintUtils;
import com.szfp.n58.utils.ToastUtils;
import com.szfp.n58.utils.WidgetUtils;
import com.szfp.n58.widget.DeviceDialog;
import com.szfp.n58.widget.dialog.BaseDialog;
import com.szfp.n58.widget.sign.LinePathView;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import okhttp3.Call;


public class MisScreen extends BaseActivity {
    private static final String TAG = MisScreen.class.getSimpleName();
    private static final String URL ="http://"+App.ip+":"+App.port+"/N58/postTran.mp" ;

    private ListView mListView;
    private List<LabelInfo> labelInfos = new ArrayList<LabelInfo>();

    private Dialog requestingDialog;

    private Handler deviceChangeHanler;

    private Set<BluetoothDevice> bondedDevices;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private List<BlueDevice> devices;

    private MisFunction misFunction;

    private SysFunction sysFunction;
    private ReturnData data;

    private Context cxt;
    private Button button;

    @Override
    protected void showDisconnecting() {
        button.setText("The printer is disconnected");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_screen);
        cxt = this;
        setupViews();
        ToastUtils.showToast("Please connect the printer");
    }

    private void setupViews() {
        mListView = (ListView) findViewById(R.id.list1);
        button = (Button) findViewById(R.id.bt_print);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeviceList();
            }
        });

        LabelInfo.resetIndex();

        labelInfos.add(new LabelInfo(getString(R.string.pos_signin)));

        labelInfos.add(new LabelInfo(getString(R.string.consume)));
        labelInfos.add(new LabelInfo(getString(R.string.repeal)));
        labelInfos.add(new LabelInfo(getString(R.string.iccard_puk_download)));
        labelInfos.add(new LabelInfo(getString(R.string.iccard_aid_download)));

        TitleAdapter adapter = new TitleAdapter(this, labelInfos);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new OnItemListClickListener());

        requestingDialog = WidgetUtils.getMyDialog(getString(R.string.requesting), this);
        devices = new ArrayList<BlueDevice>();
        AppConfig.deviceDialog = new DeviceDialog(this, R.style.MyDialogStyle, devices);

        TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tel.listen(new TelLocationListener(), PhoneStateListener.LISTEN_CELL_LOCATION);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (AppConfig.isBTChannel && !AppConfig.deviceOpen) {
            popupDeviceDialog();
        } else {
            if (!AppConfig.deviceOpen) {
                AudioController adController = AudioController.getInstance();
                int ret = adController.open(CommunicationMode.AUDIO);
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

    @Override
    protected void onDestroy() {
        BaseUtility.LOGE(TAG, "APP　exit!!!!!!!!!");
        AppConfig.deviceDialogShowing = false;

        if (AppConfig.deviceOpen) {
            close();
        }

        super.onDestroy();
    }

    @Override
    protected void showConnecting() {
        button.setText("Printer connection....");
    }

    @Override
    protected void showConnectedDeviceName(String mConnectedDeviceName) {
        button.setText("The printer is connected successfully");
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

            misFunction = new MisFunction(cxt, AppConfig.communication, misHandher);
            sysFunction = new SysFunction(cxt, AppConfig.communication, misHandher);

            String imei = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId();
            byte[] IMEI = imei.getBytes();

            byte[] mcc = BaseUtils.int2ByteArr(MCC);
            byte[] mnc = BaseUtils.int2ByteArr(MNC);
            byte[] cid = BaseUtils.int2ByteArr(CID);
            byte[] loc = BaseUtils.int2ByteArr(LOC);

            // 基站信息 (MCC+MNC+CID+LOC)
            byte[] locationInfo = new byte[2 + 1 + 4 + 4];
            System.arraycopy(mcc, 2, locationInfo, 0, 2);
            System.arraycopy(mnc, 3, locationInfo, 2, 1);
            System.arraycopy(cid, 0, locationInfo, 2 + 1, 4);
            System.arraycopy(loc, 0, locationInfo, 2 + 1 + 4, 4);

            // IMEI+基站信息
            byte[] info = new byte[IMEI.length + locationInfo.length];
            System.arraycopy(IMEI, 0, info, 0, IMEI.length);
            System.arraycopy(locationInfo, 0, info, IMEI.length, locationInfo.length);

            switch (arg2) {

                case 0:// 联机签到
                    misFunction.signIn(info);
                    break;

                case 1:// 消费
                    consumeDialog(info);
                    break;

                case 2:// 撤销
                    repealDialog(info);

                    break;

                case 3:// IC卡公钥下载
                    misFunction.downloadICCardPUK(info);
                    break;

                case 4:// IC卡AID下载
                    misFunction.downloadICCardAID(info);
                    break;

                default:
                    break;
            }

        }
    }

    /**
     * 弹出撤销框
     */

    public void repealDialog(final byte[] info) {
        if (requestingDialog != null && requestingDialog.isShowing()) {
            requestingDialog.show();
        }
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.layout_repeal, null);
        // 消费金额
        final EditText amountdit = (EditText) view.findViewById(R.id.edit_amount);
        amountdit.addTextChangedListener(new TextWatcher() {
            private boolean isChanged = false;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                if (isChanged) {// ----->如果字符未改变则返回
                    return;
                }
                String str = s.toString();

                isChanged = true;
                String cuttedStr = str;
                /* 删除字符串中的dot */
                for (int i = str.length() - 1; i >= 0; i--) {
                    char c = str.charAt(i);
                    if ('.' == c) {
                        cuttedStr = str.substring(0, i) + str.substring(i + 1);
                        break;
                    }
                }
                /* 删除前面多余的0 */
                int NUM = cuttedStr.length();
                int zeroIndex = -1;
                for (int i = 0; i < NUM - 2; i++) {
                    char c = cuttedStr.charAt(i);
                    if (c != '0') {
                        zeroIndex = i;
                        break;
                    } else if (i == NUM - 3) {
                        zeroIndex = i;
                        break;
                    }
                }
                if (zeroIndex != -1) {
                    cuttedStr = cuttedStr.substring(zeroIndex);
                }
                /* 不足3位补0 */
                if (cuttedStr.length() < 3) {
                    cuttedStr = "0" + cuttedStr;
                }
                /* 加上dot，以显示小数点后两位 */
                cuttedStr = cuttedStr.substring(0, cuttedStr.length() - 2) + "."
                        + cuttedStr.substring(cuttedStr.length() - 2);

                amountdit.setText(cuttedStr);

                amountdit.setSelection(amountdit.length());
                isChanged = false;
            }
        });
        // 消费流水号
        final EditText flowdit = (EditText) view.findViewById(R.id.edit_flow);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.repeal))
                .setView(view)
                .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        String string = amountdit.getText().toString().replace(".", "");
                        byte[] amountBCD = BCDUtils.str2Bcd(string);
                        byte[] amount = new byte[6];

                        if (amountBCD.length <= 6) {
                            System.arraycopy(amountBCD, 0, amount, 6 - amountBCD.length,
                                    amountBCD.length);
                        }

                        String string1 = flowdit.getText().toString();
                        byte[] flowBCD = BCDUtils.str2Bcd(string1);
                        byte[] flow = new byte[3];

                        if (flowBCD.length <= 3) {
                            System.arraycopy(flowBCD, 0, flow, 3 - flowBCD.length,
                                    flowBCD.length);
                        }

                        try {
                            Field field = arg0.getClass().getSuperclass()
                                    .getDeclaredField("mShowing");
                            field.setAccessible(true);
                            if ("".equals(amount)) {
                                field.set(arg0, false); // 使之不能关闭
                                amountdit.requestFocus();
                            } else {
                                field.set(arg0, true);
                                // 弹出请求框
                                if (requestingDialog != null) {
                                    if (!requestingDialog.isShowing()) {
                                        requestingDialog.show();
                                    }
                                }
                                byte[] data = new byte[info.length + 9];
                                System.arraycopy(info, 0, data, 0, info.length);
                                System.arraycopy(flow, 0, data, info.length, 3);
                                System.arraycopy(amount, 0, data, info.length + 3, 6);
                                misFunction.repeal(data);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                })
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                try {
                                    Field field = arg0.getClass().getSuperclass()
                                            .getDeclaredField("mShowing");
                                    field.setAccessible(true);
                                    field.set(arg0, true);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

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

    /**
     * 弹出消费框
     */
    public void consumeDialog(final byte[] info) {
        if (requestingDialog != null && requestingDialog.isShowing()) {
            requestingDialog.show();
        }

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_consume, null);

        // 消费金额
        final EditText amountdit = (EditText) view.findViewById(R.id.edit_amount);
        amountdit.addTextChangedListener(new TextWatcher() {
            private boolean isChanged = false;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                if (isChanged) {// ----->如果字符未改变则返回
                    return;
                }
                String str = s.toString();

                isChanged = true;
                String cuttedStr = str;
                /* 删除字符串中的dot */
                for (int i = str.length() - 1; i >= 0; i--) {
                    char c = str.charAt(i);
                    if ('.' == c) {
                        cuttedStr = str.substring(0, i) + str.substring(i + 1);
                        break;
                    }
                }
                /* 删除前面多余的0 */
                int NUM = cuttedStr.length();
                int zeroIndex = -1;
                for (int i = 0; i < NUM - 2; i++) {
                    char c = cuttedStr.charAt(i);
                    if (c != '0') {
                        zeroIndex = i;
                        break;
                    } else if (i == NUM - 3) {
                        zeroIndex = i;
                        break;
                    }
                }
                if (zeroIndex != -1) {
                    cuttedStr = cuttedStr.substring(zeroIndex);
                }
                /* 不足3位补0 */
                if (cuttedStr.length() < 3) {
                    cuttedStr = "0" + cuttedStr;
                }
                /* 加上dot，以显示小数点后两位 */
                cuttedStr = cuttedStr.substring(0, cuttedStr.length() - 2) + "."
                        + cuttedStr.substring(cuttedStr.length() - 2);

                amountdit.setText(cuttedStr);

                amountdit.setSelection(amountdit.length());
                isChanged = false;
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.consume))
                .setView(view)
                .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        byte[] data = new byte[7];
                        // 消费金额
                        String amount = amountdit.getText().toString().replace(".", "");
                        byte[] amountBCD = BCDUtils.str2Bcd(amount);

                        if (amountBCD.length <= 6) {
                            System.arraycopy(amountBCD, 0, data, 6 - amountBCD.length,
                                    amountBCD.length);
                        }

                        data[6] = (byte) 0xFC;

                        try {
                            Field field = arg0.getClass().getSuperclass()
                                    .getDeclaredField("mShowing");
                            field.setAccessible(true);
                            if ("".equals(amount)) {
                                field.set(arg0, false); // 使之不能关闭
                                amountdit.requestFocus();
                            } else {
                                field.set(arg0, true);
                                // 弹出请求框
                                if (requestingDialog != null) {
                                    if (!requestingDialog.isShowing()) {
                                        requestingDialog.show();
                                    }
                                }

                                byte[] reqData = new byte[info.length + data.length];
                                System.arraycopy(info, 0, reqData, 0, info.length);
                                System.arraycopy(data, 0, reqData, info.length, data.length);

                                misFunction.consume(reqData);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                })
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                try {
                                    Field field = arg0.getClass().getSuperclass()
                                            .getDeclaredField("mShowing");
                                    field.setAccessible(true);
                                    field.set(arg0, true);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

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

    private Handler misHandher = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            BaseUtils.vibrate(MisScreen.this, 200);
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
            // 发送超时
            if (msg.arg1 == ICommunication.OPER_TIMEOUT) {
                WidgetUtils.showResult(cxt, getString(R.string.tips),
                        getString(R.string.req_timeout));
                return;
            }
            // 读取超时
            if (msg.arg2 == ICommunication.OPER_TIMEOUT) {
                WidgetUtils.showResult(cxt, getString(R.string.tips),
                        getString(R.string.recv_timeout));
                return;
            }

            // MPOS端应答指令执行结果或响应数据
            DataResponse dataRsp = (DataResponse) msg.obj;

            Log.d("DataResponse",dataRsp.toString());
            if (dataRsp == null) {
                WidgetUtils.showResult(cxt, getString(R.string.tips),
                        getString(R.string.cmd_read_error));
                return;
            }

            int excpCode = dataRsp.getExceptionCode();
            if (excpCode == OperateMPOS.INTERNET_EXCP) {
                WidgetUtils.showResult(cxt, getString(R.string.tips),
                        getString(R.string.excp_internet));
                sysFunction.cancelAction();
                return;
            } else if (excpCode == OperateMPOS.UNKNOW_HOST_EXCP) {
                WidgetUtils.showResult(cxt, getString(R.string.tips),
                        getString(R.string.excp_unknow_host));
                sysFunction.cancelAction();
                return;
            } else if (excpCode == OperateMPOS.CONNECT_EXCP) {
                WidgetUtils.showResult(cxt, getString(R.string.tips),
                        getString(R.string.connect_server_failed));
                sysFunction.cancelAction();
                return;
            }

            // 指令执行结果
            int ret = dataRsp.getRspResult();
            if (ret == Instruction.Code.INS_SUCCESS) {
                MIS mis = MIS.getInstance(msg.what);
                switch (mis) {
                    case MIS_TERMINAL_INIT:
                        WidgetUtils.showResult(cxt, getString(R.string.terminal_init),
                                getString(R.string.success));
                        break;
                    case MIS_SIGN_IN:
                        WidgetUtils.showResult(cxt, getString(R.string.pos_signin),
                                getString(R.string.success));
                        break;
                    case MIS_SIGN_OUT:
                        WidgetUtils.showResult(cxt, getString(R.string.pos_signout),
                                getString(R.string.success));
                        break;
                    case MIS_SETTLEMENT:
                        WidgetUtils.showResult(cxt, getString(R.string.pos_settlement),
                                getString(R.string.success));
                        break;
                    case MIS_POS_UPLOAD_ARGS:
                        WidgetUtils.showResult(cxt, getString(R.string.send_terminal_args),
                                getString(R.string.success));
                        break;
                    case MIS_POS_DOWNLOAD_ARGS:
                        WidgetUtils
                                .showResult(cxt, getString(R.string.download_terminal_args),
                                        getString(R.string.success));
                        break;

                    case MIS_ICCARD_PUK_DOWNLOAD:
                        WidgetUtils.showResult(cxt, getString(R.string.iccard_puk_download),
                                getString(R.string.success));
                        break;

                    case MIS_ICCARD_AID_DOWNLOAD:
                        WidgetUtils.showResult(cxt, getString(R.string.iccard_aid_download),
                                getString(R.string.success));
                        break;

                    case MIS_RESPONDING_TESTING:
                        WidgetUtils.showResult(cxt, getString(R.string.responding_test),
                                getString(R.string.success));
                        break;
                    case MIS_BALANCE_QUERY:
                        WidgetUtils.showResult(cxt, getString(R.string.query_balance),
                                getString(R.string.success));
                        break;

                    case MIS_CONSUME:
                        byte[] content = dataRsp.getDataContent();
                        BaseUtility.printHex(TAG, content);

                        TransactionData trac = new TransactionData();
                        data = trac.parseTLVData(cxt, content);
                        data.getData().settType("1");
                        WidgetUtils.showResultPrint(
                                cxt,
                                getString(R.string.consume),
                                getString(R.string.success) + "\n"+ data.getStr(), new OnShowPrintCallBack() {
                                    @Override
                                    public void success(String title, String message) {
                                        showSign(title,message);

                                    }
                                });


                        postData(data);
                        break;

                    case MIS_REPEAL:
                        byte[] content1 = dataRsp.getDataContent();

                        BaseUtility.printHex(TAG, content1);

                        TransactionData trac1 = new TransactionData();
                        data = trac1.parseTLVData(cxt, content1);
                        data.getData().settType("2");
                        WidgetUtils.showResultPrint(
                                cxt,
                                getString(R.string.repeal),
                                getString(R.string.success) + "\n"
                                        + data.getStr(), new OnShowPrintCallBack() {
                                    @Override
                                    public void success(String title, String message) {
                                        showSign(title,message);

                                    }
                                });

                        postData(data);
                        break;

                    default:
                        break;
                }
            } else {
                String tips = MainActivity.codeMsg.get(ret);
                WidgetUtils.showResult(cxt, getString(R.string.tips), tips != null ? tips
                        : getString(R.string.failure));
            }
        }

    };

    /**
     * 签名
     */

    public static String path= Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "qm.png";
    BaseDialog signDialog;
    private LinePathView linePathView;
    private Button btSignClear;
    private Button btSignSave;
    private Button btSignBlack;
    private Bitmap signBitmap;
    private void showSign(final String title, final String message) {
        if (signDialog==null)
        {
            View view = ContextUtils.inflate(this,R.layout.dialog_sign);
            linePathView = (LinePathView) view.findViewById(R.id.signView);
            btSignClear = (Button) view.findViewById(R.id.bt_sign_clear);
            btSignSave = (Button) view.findViewById(R.id.bt_sign_save);
            btSignBlack = (Button) view.findViewById(R.id.bt_sign_black);
            signDialog = new BaseDialog(mContext,R.style.AlertDialogStyle);
            linePathView.setPaintWidth(10);
            signDialog.setCancelable(false);
            signDialog.setContentView(view);
            btSignBlack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    linePathView.clear();
                    signDialog.cancel();
                }
            });
            btSignClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    linePathView.clear();
                }
            });
            btSignSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (linePathView.getTouched()){
                        try {
                            linePathView.save("/sdcard/qm.png", true, 10);
                        } catch (IOException e) {
                            ToastUtils.error("You have no signature~~~~");
                            e.printStackTrace();
                        }

                        PrintUtils.printData(message, title,path);
                        signDialog.cancel();

                    }else {
                        ToastUtils.error("You have no signature~~~~");
                    }
                }
            });
        }
        linePathView.clear();
        signDialog.show();

    }

    /**
     * 提交数据到服务器
     * @param data
     */
    private void postData(ReturnData data) {
        OkGo.<String>post(URL)
                .tag(this)
                .params("data",new Gson().toJson(data))
                .execute(new DialogCallback<String>(mContext) {
                    @Override
                    public void onSuccess(Response<String> response) {
                        handleResponse(response);
                    }

                    @Override
                    public void onError(Response<String> response) {
                        handleError(response);
                    }
                });

    }

    private  void handleError(Response<String> response) {
        Toast.makeText(this,"error",Toast.LENGTH_SHORT).show();
    }

    private  void handleResponse(Response<String> response) {
        StringBuilder sb;
        Call call = response.getRawCall();
        String body = response.body();
        if (body == null) {
            Toast.makeText(this,"The return data is empty",Toast.LENGTH_SHORT).show();
        } else {
                Toast.makeText(this, body.toString(),Toast.LENGTH_SHORT).show();
        }
    }

    private static int MCC = -1;
    private static int MNC = -1;
    private static int CID = -1;
    private static int LOC = -1;

    // MCC （2字节）： 国际标识 Mobile Country Code 中国为460
    // MNC （1字节）： 运营商标志 Mobile Network Code 中国移动为00，中国联通为01,中国电信为03
    // CID （4字节）： 基站标识 cell id
    // LOC （4字节）： 区域标识 location id
    public class TelLocationListener extends PhoneStateListener {
        public void onCellLocationChanged(CellLocation location) {
            super.onCellLocationChanged(location);
            if (location == null)
                return;
            int mcc = 0;
            int mnc = 0;

            TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String Operator = telMgr.getNetworkOperator();
            if (Operator != null && Operator.length() >= 5) {
                mcc = Integer.valueOf(Operator.substring(0, 3));
                mnc = Integer.valueOf(Operator.substring(3, 5));
            }
            MCC = mcc;
            MNC = mnc;

            if (location.getClass().equals(GsmCellLocation.class)) {
                GsmCellLocation loc = (GsmCellLocation) location;
                CID = loc.getCid();
                LOC = loc.getLac();
            } else if (location.getClass().equals(CdmaCellLocation.class)) {
                CdmaCellLocation loc = (CdmaCellLocation) location;
                CID = loc.getBaseStationId();
                LOC = loc.getNetworkId();
            }

        }
    }

}

