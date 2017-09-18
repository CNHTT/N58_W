package com.szfp.n58.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * author：ct on 2017/9/14 11:25
 * email：cnhttt@163.com
 */

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private static final boolean D = true;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_CONNECTION_LOST = 5;
    public static final int MESSAGE_UNABLE_CONNECT = 6;
    private static final String NAME = "BTPrinter";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static int mState=0;
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final Handler mHandler;
    private BluetoothService.AcceptThread mAcceptThread;
    private BluetoothService.ConnectThread mConnectThread;
    private static BluetoothService.ConnectedThread mConnectedThread;
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public BluetoothService(Context context, Handler handler) {
        this.mHandler = handler;
    }

    public synchronized boolean isAvailable() {
        return this.mAdapter != null;
    }

    public synchronized boolean isBTopen() {
        return this.mAdapter.isEnabled();
    }

    public synchronized BluetoothDevice getDevByMac(String mac) {
        return this.mAdapter.getRemoteDevice(mac);
    }
    //    public static final String[] items = { "复位打印机", "标准ASCII字体", "压缩ASCII字体", "字体大小:00","字体大小:02","字体大小:06",
//            "字体大小:11", "取消加粗模式", "选择加粗模式", "取消倒置打印", "选择倒置打印", "取消黑白反显", "选择黑白反显",
//            "取消顺时针旋转90°", "选择顺时针旋转90°","左对齐","居中","右对齐" };
    public static final byte[][] byteCommands = { { 0x1b, 0x40 },// 复位打印机
            { 0x1b, 0x4d, 0x00 },// 标准ASCII字体
            { 0x1b, 0x4d, 0x01 },// 压缩ASCII字体
            { 0x1d, 0x21, 0x00 },// 字体不放大
            { 0x1d, 0x21, 0x02 },// 宽高加倍
            { 0x1d, 0x21, 0x11 },// 宽高加倍
//            { 0x1d, 0x21, 0x11 },// 宽高加倍
            { 0x1b, 0x45, 0x00 },// 取消加粗模式
            { 0x1b, 0x45, 0x01 },// 选择加粗模式
            { 0x1b, 0x7b, 0x00 },// 取消倒置打印
            { 0x1b, 0x7b, 0x01 },// 选择倒置打印
            { 0x1d, 0x42, 0x00 },// 取消黑白反显
            { 0x1d, 0x42, 0x01 },// 选择黑白反显
            { 0x1b, 0x56, 0x00 },// 取消顺时针旋转90°
            { 0x1b, 0x56, 0x01 },// 选择顺时针旋转90°

            { 0x1b, 0x61, 0x30 },// 左对齐
            { 0x1b, 0x61, 0x31 },// 居中对齐
            { 0x1b, 0x61, 0x32 },// 右对齐
//            { 0x1b, 0x69 },// 切纸
    };


    public void print(int i){
        write(byteCommands[i]);
    }

    public void printReset(){
        if (getState() != BluetoothService.STATE_CONNECTED) {
            return;
        }
        write(byteCommands[0]);
    }

    public void printSize(int size){
        if (getState() != BluetoothService.STATE_CONNECTED) {
            return;
        }
        switch (size) {
            case 1:
                write(byteCommands[4]);
                break;
            case 2:
                write(byteCommands[5]);
                break;
            default:
                write(byteCommands[3]);
                break;
        }
    }
    public void printLeft(){
        if (getState() != BluetoothService.STATE_CONNECTED) {
            return;
        }
        write(byteCommands[14]);
    }
    public void printRight(){
        if (getState() != BluetoothService.STATE_CONNECTED) {
            return;
        }
        write(byteCommands[16]);
    }
    public void printCenter(){
        if (getState() != BluetoothService.STATE_CONNECTED) {
            return;
        }
        write(byteCommands[15]);
    }


    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(Utils.getContext(), "蓝牙没有连接", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send;
            try {
                send = message.getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                send = message.getBytes();
            }

            write(send);
        }
    }


    public synchronized BluetoothDevice getDevByName(String name) {
        BluetoothDevice tem_dev = null;
        Set pairedDevices = this.getPairedDev();
        if(pairedDevices.size() > 0) {
            Iterator var5 = pairedDevices.iterator();

            while(var5.hasNext()) {
                BluetoothDevice device = (BluetoothDevice)var5.next();
                if(device.getName().indexOf(name) != -1) {
                    tem_dev = device;
                    break;
                }
            }
        }

        return tem_dev;
    }

    public synchronized void sendMessage(String message, String charset) {
        if(message.length() > 0) {
            byte[] send;
            try {
                send = message.getBytes(charset);
            } catch (UnsupportedEncodingException var5) {
                send = message.getBytes();
            }

            this.write(send);
            byte[] tail = new byte[]{10, 13, 0};
            this.write(tail);
        }

    }

    public synchronized Set<BluetoothDevice> getPairedDev() {
        Set dev = null;
        dev = this.mAdapter.getBondedDevices();
        return dev;
    }

    public synchronized boolean cancelDiscovery() {
        return this.mAdapter.cancelDiscovery();
    }

    public synchronized boolean isDiscovering() {
        return this.mAdapter.isDiscovering();
    }

    public synchronized boolean startDiscovery() {
        return this.mAdapter.startDiscovery();
    }

    private synchronized void setState(int state) {
        this.mState = state;
        this.mHandler.obtainMessage(1, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return this.mState;
    }

    public synchronized void start() {
        Log.d("BluetoothService", "start");
        if(this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if(this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        if(this.mAcceptThread == null) {
            this.mAcceptThread = new BluetoothService.AcceptThread();
            this.mAcceptThread.start();
        }

        this.setState(1);
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d("BluetoothService", "connect to: " + device);
        if(this.mState == 2 && this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if(this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        this.mConnectThread = new BluetoothService.ConnectThread(device);
        this.mConnectThread.start();
        this.setState(2);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d("BluetoothService", "connected");
        if(this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if(this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        if(this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread = null;
        }

        this.mConnectedThread = new BluetoothService.ConnectedThread(socket);
        this.mConnectedThread.start();
        Message msg = this.mHandler.obtainMessage(4);
        this.mHandler.sendMessage(msg);
        this.setState(3);
    }

    public synchronized void stop() {
        Log.d("BluetoothService", "stop");
        this.setState(0);
        if(this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if(this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        if(this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread = null;
        }

    }

    public void write(byte[] out) {
        BluetoothService.ConnectedThread r;
        synchronized(this) {
            if(this.mState != 3) {
                return;
            }

            r = this.mConnectedThread;
        }

        r.write(out);
    }

    private void connectionFailed() {
        this.setState(1);
        Message msg = this.mHandler.obtainMessage(6);
        this.mHandler.sendMessage(msg);
    }

    private void connectionLost() {
        Message msg = this.mHandler.obtainMessage(5);
        this.mHandler.sendMessage(msg);
    }



    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = BluetoothService.this.mAdapter.listenUsingRfcommWithServiceRecord("BTPrinter", BluetoothService.MY_UUID);
            } catch (IOException var4) {
                Log.e("BluetoothService", "listen() failed", var4);
            }

            this.mmServerSocket = tmp;
        }

        public void run() {
            Log.d("BluetoothService", "BEGIN mAcceptThread" + this);
            this.setName("AcceptThread");
            BluetoothSocket socket = null;

            while(BluetoothService.this.mState != 3) {
                Log.d("AcceptThread线程运行", "正在运行......");

                try {
                    socket = this.mmServerSocket.accept();
                } catch (IOException var6) {
                    Log.e("BluetoothService", "accept() failed", var6);
                    break;
                }

                if(socket != null) {
                    BluetoothService e = BluetoothService.this;
                    synchronized(BluetoothService.this) {
                        switch(BluetoothService.this.mState) {
                            case 0:
                            case 3:
                                try {
                                    socket.close();
                                } catch (IOException var4) {
                                    Log.e("BluetoothService", "Could not close unwanted socket", var4);
                                }
                                break;
                            case 1:
                            case 2:
                                BluetoothService.this.connected(socket, socket.getRemoteDevice());
                        }
                    }
                }
            }

            Log.i("BluetoothService", "END mAcceptThread");
        }

        public void cancel() {
            Log.d("BluetoothService", "cancel " + this);

            try {
                this.mmServerSocket.close();
            } catch (IOException var2) {
                Log.e("BluetoothService", "close() of server failed", var2);
            }

        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            this.mmDevice = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(BluetoothService.MY_UUID);
            } catch (IOException var5) {
                Log.e("BluetoothService", "create() failed", var5);
            }

            this.mmSocket = tmp;
        }

        public void run() {
            Log.i("BluetoothService", "BEGIN mConnectThread");
            this.setName("ConnectThread");
            BluetoothService.this.mAdapter.cancelDiscovery();

            try {
                this.mmSocket.connect();
            } catch (IOException var5) {
                BluetoothService.this.connectionFailed();

                try {
                    this.mmSocket.close();
                } catch (IOException var3) {
                    Log.e("BluetoothService", "unable to close() socket during connection failure", var3);
                }

                BluetoothService.this.start();
                return;
            }

            BluetoothService e = BluetoothService.this;
            synchronized(BluetoothService.this) {
                BluetoothService.this.mConnectThread = null;
            }

            BluetoothService.this.connected(this.mmSocket, this.mmDevice);
        }

        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException var2) {
                Log.e("BluetoothService", "close() of connect socket failed", var2);
            }

        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d("BluetoothService", "create ConnectedThread");
            this.mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException var6) {
                Log.e("BluetoothService", "temp sockets not created", var6);
            }

            this.mmInStream = tmpIn;
            this.mmOutStream = tmpOut;
        }

        public void run() {
            Log.d("ConnectedThread线程运行", "正在运行......");
            Log.i("BluetoothService", "BEGIN mConnectedThread");

            try {
                while(true) {
                    byte[] e = new byte[256];
                    int bytes = this.mmInStream.read(e);
                    if(bytes <= 0) {
                        Log.e("BluetoothService", "disconnected");
                        BluetoothService.this.connectionLost();
                        if(BluetoothService.this.mState != 0) {
                            Log.e("BluetoothService", "disconnected");
                            BluetoothService.this.start();
                        }
                        break;
                    }

                    BluetoothService.this.mHandler.obtainMessage(2, bytes, -1, e).sendToTarget();
                }
            } catch (IOException var3) {
                Log.e("BluetoothService", "disconnected", var3);
                BluetoothService.this.connectionLost();
                if(BluetoothService.this.mState != 0) {
                    BluetoothService.this.start();
                }
            }

        }

        public void write(byte[] buffer) {
            try {
                this.mmOutStream.write(buffer);
                BluetoothService.this.mHandler.obtainMessage(3, -1, -1, buffer).sendToTarget();
            } catch (IOException var3) {
                Log.e("BluetoothService", "Exception during write", var3);
            }

        }

        public void write(byte[] buffer, int dataLen) {
            try {
                for(int e = 0; e < dataLen; ++e) {
                    this.mmOutStream.write(buffer[e]);
                }

                BluetoothService.this.mHandler.obtainMessage(3, -1, -1, buffer).sendToTarget();
            } catch (IOException var4) {
                Log.e("BluetoothChatService", "Exception during write", var4);
            }

        }

        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException var2) {
                Log.e("BluetoothService", "close() of connect socket failed", var2);
            }

        }
    }
    public static boolean IsNoConnection() {
        return mState != 3;
    }

    public static boolean InitPrinter() {
        byte[] combyte = new byte[]{27, 64};
        if(mState != 3) {
            return false;
        } else {
            BT_Write(combyte);
            return true;
        }
    }

    public static void WakeUpPritner() {
        byte[] b = new byte[3];

        try {
            BT_Write(b);
            Thread.sleep(100L);
        } catch (Exception var2) {
            var2.printStackTrace();
        }

    }

    public static void BT_Write(byte[] out, int dataLen) {
        if(mState == 3) {
            BluetoothService.ConnectedThread r = mConnectedThread;
            r.write(out, dataLen);
        }
    }

    public static void Begin() {
        WakeUpPritner();
        InitPrinter();
    }

    public static void LF() {
        byte[] cmd = new byte[]{13};
        BT_Write(cmd);
    }

    public static void CR() {
        byte[] cmd = new byte[]{10};
        BT_Write(cmd);
    }

    public static void SelftestPrint() {
        byte[] cmd = new byte[]{18, 84};
        BT_Write(cmd, 2);
    }

    public static void StatusInquiry() {
        byte[] cmd = new byte[]{0, 0, 16, 4, -2, 0, 0, 16, 4, -1};
        BT_Write(cmd, 10);
    }

    public static void SetRightSpacing(byte Distance) {
        byte[] cmd = new byte[]{27, 32, Distance};
        BT_Write(cmd);
    }

    public static void SetAbsolutePrintPosition(byte nL, byte nH) {
        byte[] cmd = new byte[]{27, 36, nL, nH};
        BT_Write(cmd);
    }

    public static void SetRelativePrintPosition(byte nL, byte nH) {
        byte[] cmd = new byte[]{27, 92, nL, nH};
        BT_Write(cmd);
    }

    public static void SetDefaultLineSpacing() {
        byte[] cmd = new byte[]{27, 50};
        BT_Write(cmd);
    }

    public static void SetLineSpacing(byte LineSpacing) {
        byte[] cmd = new byte[]{27, 51, LineSpacing};
        BT_Write(cmd);
    }

    public static void SetLeftStartSpacing(byte nL, byte nH) {
        byte[] cmd = new byte[]{29, 76, nL, nH};
        BT_Write(cmd);
    }

    public static void SetAreaWidth(byte nL, byte nH) {
        byte[] cmd = new byte[]{29, 87, nL, nH};
        BT_Write(cmd);
    }

    public static void SetCharacterPrintMode(byte CharacterPrintMode) {
        byte[] cmd = new byte[]{27, 33, CharacterPrintMode};
        BT_Write(cmd);
    }

    public static void SetUnderline(byte UnderlineEn) {
        byte[] cmd = new byte[]{27, 45, UnderlineEn};
        BT_Write(cmd);
    }

    public static void SetBold(byte BoldEn) {
        byte[] cmd = new byte[]{27, 69, BoldEn};
        BT_Write(cmd);
    }

    public static void SetCharacterFont(byte Font) {
        byte[] cmd = new byte[]{27, 77, Font};
        BT_Write(cmd);
    }

    public static void SetRotate(byte RotateEn) {
        byte[] cmd = new byte[]{27, 86, RotateEn};
        BT_Write(cmd);
    }

    public static void SetAlignMode(byte AlignMode) {
        byte[] cmd = new byte[]{27, 97, AlignMode};
        BT_Write(cmd);
    }

    public static void SetInvertPrint(byte InvertModeEn) {
        byte[] cmd = new byte[]{27, 123, InvertModeEn};
        BT_Write(cmd);
    }

    public static void SetFontEnlarge(byte FontEnlarge) {
        byte[] cmd = new byte[]{29, 33, FontEnlarge};
        BT_Write(cmd);
    }

    public static void BT_Write(String dataString) {
        byte[] data = null;
        if(mState == 3) {
            BluetoothService.ConnectedThread r = mConnectedThread;

            try {
                data = dataString.getBytes("GBK");
            } catch (UnsupportedEncodingException var4) {
                var4.printStackTrace();
            }

            r.write(data);
        }
    }

    public static void BT_Write(String dataString, boolean bGBK) {
        byte[] data = null;
        if(mState == 3) {
            BluetoothService.ConnectedThread r = mConnectedThread;
            if(bGBK) {
                try {
                    data = dataString.getBytes("GBK");
                } catch (UnsupportedEncodingException var5) {
                    ;
                }
            } else {
                data = dataString.getBytes();
            }

            r.write(data);
        }
    }

    public static void BT_Write(byte[] out) {
        if(mState == 3) {
            BluetoothService.ConnectedThread r = mConnectedThread;
            r.write(out);
        }
    }



    public static void SetBlackReversePrint(byte BlackReverseEn) {
        byte[] cmd = new byte[]{29, 66, BlackReverseEn};
        BT_Write(cmd);
    }

    public static void SetChineseCharacterMode(byte ChineseCharacterMode) {
        byte[] cmd = new byte[]{28, 33, ChineseCharacterMode};
        BT_Write(cmd);
    }

    public static void SelChineseCodepage() {
        byte[] cmd = new byte[]{28, 38};
        BT_Write(cmd);
    }

    public static void CancelChineseCodepage() {
        byte[] cmd = new byte[]{28, 46};
        BT_Write(cmd);
    }

    public static void SetChineseUnderline(byte ChineseUnderlineEn) {
        byte[] cmd = new byte[]{28, 45, ChineseUnderlineEn};
        BT_Write(cmd);
    }

    public static void OpenDrawer(byte DrawerNumber, byte PulseStartTime, byte PulseEndTime) {
        byte[] cmd = new byte[]{27, 112, DrawerNumber, PulseStartTime, PulseEndTime};
        BT_Write(cmd);
    }

    public static void CutPaper() {
        byte[] cmd = new byte[]{27, 105};
        BT_Write(cmd);
    }

    public static void PartialCutPaper() {
        byte[] cmd = new byte[]{27, 109};
        BT_Write(cmd);
    }

    public static void FeedAndCutPaper(byte CutMode) {
        byte[] cmd = new byte[]{29, 86, CutMode};
        BT_Write(cmd);
    }

    public static void FeedAndCutPaper(byte CutMode, byte FeedDistance) {
        byte[] cmd = new byte[]{29, 86, CutMode, FeedDistance};
        BT_Write(cmd);
    }

    public static void AddQRCodePrint() {
        byte[] cmd = new byte[]{29, 40, 107, 3, 0, 49, 67, 3, 29, 40, 107, 3, 0, 49, 69, 51, 29, 40, 107, 83, 0, 49, 80, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 29, 40, 107, 3, 0, 49, 81, 48, 29, 40, 107, 4, 0, 49, 65, 49, 0};
        BT_Write(cmd);
    }

    public static void AddCodePrint(int CodeType, String data) {
        switch(CodeType) {
            case 0:
                UPCA(data);
                break;
            case 1:
                UPCE(data);
                break;
            case 2:
                EAN13(data);
                break;
            case 3:
                EAN8(data);
                break;
            case 4:
                CODE39(data);
                break;
            case 5:
                ITF(data);
                break;
            case 6:
                CODEBAR(data);
                break;
            case 7:
                CODE93(data);
                break;
            case 8:
                Code128_B(data);
            case 9:
            case 10:
        }

    }

    public static void UPCA(String data) {
        byte m = 0;
        int num = data.length();
        byte mIndex = 0;
        byte[] cmd = new byte[1024];
        int var6 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var6++] = 107;
        cmd[var6++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if(data.charAt(i) > 57 || data.charAt(i) < 48) {
                return;
            }
        }

        if(num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var6++] = (byte)data.charAt(i);
            }

            BT_Write(cmd);
        }
    }

    public static void UPCE(String data) {
        byte m = 1;
        int num = data.length();
        byte mIndex = 0;
        byte[] cmd = new byte[1024];
        int var6 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var6++] = 107;
        cmd[var6++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if(data.charAt(i) > 57 || data.charAt(i) < 48) {
                return;
            }
        }

        if(num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var6++] = (byte)data.charAt(i);
            }

            BT_Write(cmd);
        }
    }

    public static void EAN13(String data) {
        byte m = 2;
        int num = data.length();
        byte mIndex = 0;
        byte[] cmd = new byte[1024];
        int var6 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var6++] = 107;
        cmd[var6++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if(data.charAt(i) > 57 || data.charAt(i) < 48) {
                return;
            }
        }

        if(num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var6++] = (byte)data.charAt(i);
            }

            BT_Write(cmd);
        }
    }

    public static void EAN8(String data) {
        byte m = 3;
        int num = data.length();
        byte mIndex = 0;
        byte[] cmd = new byte[1024];
        int var6 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var6++] = 107;
        cmd[var6++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if(data.charAt(i) > 57 || data.charAt(i) < 48) {
                return;
            }
        }

        if(num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var6++] = (byte)data.charAt(i);
            }

            BT_Write(cmd);
        }
    }

    public static void CODE39(String data) {
        byte m = 4;
        int num = data.length();
        byte mIndex = 0;
        byte[] cmd = new byte[1024];
        int var6 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var6++] = 107;
        cmd[var6++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if(data.charAt(i) > 127 || data.charAt(i) < 32) {
                return;
            }
        }

        if(num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var6++] = (byte)data.charAt(i);
            }

            BT_Write(cmd);
        }
    }

    public static void ITF(String data) {
        byte m = 5;
        int num = data.length();
        byte mIndex = 0;
        byte[] cmd = new byte[1024];
        int var6 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var6++] = 107;
        cmd[var6++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if(data.charAt(i) > 57 || data.charAt(i) < 48) {
                return;
            }
        }

        if(num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var6++] = (byte)data.charAt(i);
            }

            BT_Write(cmd);
        }
    }

    public static void CODEBAR(String data) {
        byte m = 6;
        int num = data.length();
        byte mIndex = 0;
        byte[] cmd = new byte[1024];
        int var6 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var6++] = 107;
        cmd[var6++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if(data.charAt(i) > 127 || data.charAt(i) < 32) {
                return;
            }
        }

        if(num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var6++] = (byte)data.charAt(i);
            }

            BT_Write(cmd);
        }
    }

    public static void CODE93(String data) {
        byte m = 7;
        int num = data.length();
        byte mIndex = 0;
        byte[] cmd = new byte[1024];
        int var6 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var6++] = 107;
        cmd[var6++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if(data.charAt(i) > 127 || data.charAt(i) < 32) {
                return;
            }
        }

        if(num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var6++] = (byte)data.charAt(i);
            }

            BT_Write(cmd);
        }
    }

    public static void Code128_B(String data) {
        byte m = 73;
        int num = data.length();
        int transNum = 0;
        byte mIndex = 0;
        byte[] cmd = new byte[1024];
        int var10 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var10++] = 107;
        cmd[var10++] = (byte)m;
        int Code128C = var10++;
        cmd[var10++] = 123;
        cmd[var10++] = 66;

        int checkcodeID;
        for(checkcodeID = 0; checkcodeID < num; ++checkcodeID) {
            if(data.charAt(checkcodeID) > 127 || data.charAt(checkcodeID) < 32) {
                return;
            }
        }

        if(num <= 30) {
            for(checkcodeID = 0; checkcodeID < num; ++checkcodeID) {
                cmd[var10++] = (byte)data.charAt(checkcodeID);
                if(data.charAt(checkcodeID) == 123) {
                    cmd[var10++] = (byte)data.charAt(checkcodeID);
                    ++transNum;
                }
            }

            checkcodeID = 104;
            int n = 1;

            for(int i = 0; i < num; ++i) {
                checkcodeID += n++ * (data.charAt(i) - 32);
            }

            checkcodeID %= 103;
            if(checkcodeID >= 0 && checkcodeID <= 95) {
                cmd[var10++] = (byte)(checkcodeID + 32);
                cmd[Code128C] = (byte)(num + 3 + transNum);
            } else if(checkcodeID == 96) {
                cmd[var10++] = 123;
                cmd[var10++] = 51;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if(checkcodeID == 97) {
                cmd[var10++] = 123;
                cmd[var10++] = 50;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if(checkcodeID == 98) {
                cmd[var10++] = 123;
                cmd[var10++] = 83;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if(checkcodeID == 99) {
                cmd[var10++] = 123;
                cmd[var10++] = 67;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if(checkcodeID == 100) {
                cmd[var10++] = 123;
                cmd[var10++] = 52;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if(checkcodeID == 101) {
                cmd[var10++] = 123;
                cmd[var10++] = 65;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if(checkcodeID == 102) {
                cmd[var10++] = 123;
                cmd[var10++] = 49;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            }

            BT_Write(cmd);
        }
    }

    public static void printString(String str) {
        try {
            BT_Write(str.getBytes("GBK"));
            BT_Write(new byte[]{10});
        } catch (IOException var2) {
            var2.printStackTrace();
        }

    }

    public static void printParameterSet(byte[] buf) {
        BT_Write(buf);
    }

    public static void printByteData(byte[] buf) {
        BT_Write(buf);
        BT_Write(new byte[]{10});
    }

    public static void printImage() {
        byte[] bufTemp2 = new byte[]{27, 74, 24, 29, 118, 48, 0, 16, 0, -128, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -13, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 8, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 12, 0, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 14, 0, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 15, 0, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -13, -1, -16, 15, -128, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -31, -1, -16, 15, -64, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -64, -1, -16, 15, -32, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -128, 127, -16, 15, -16, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 63, -16, 15, -8, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 31, -16, 15, -8, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -128, 15, -16, 15, -16, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -64, 7, -16, 15, -32, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -32, 3, -16, 15, -64, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 1, -16, 15, -128, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -8, 0, -16, 15, 0, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -4, 0, 112, 14, 0, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -2, 0, 48, 12, 0, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 16, 8, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -128, 0, 0, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -64, 0, 0, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -32, 0, 0, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 0, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -8, 0, 0, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -4, 0, 0, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -2, 0, 0, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -128, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -64, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -64, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -128, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -2, 0, 0, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -4, 0, 0, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -8, 0, 0, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 0, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -32, 0, 0, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -64, 0, 0, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -128, 0, 0, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 16, 8, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -2, 0, 48, 12, 0, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -4, 0, 112, 14, 0, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -8, 0, -16, 15, 0, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 1, -16, 15, -128, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -32, 3, -16, 15, -64, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -64, 7, -16, 15, -32, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -128, 15, -16, 15, -16, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 31, -16, 15, -8, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 63, -16, 15, -4, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -128, 127, -16, 15, -8, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -64, -1, -16, 15, -16, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -31, -1, -16, 15, -32, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -13, -1, -16, 15, -64, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 15, -128, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 15, 0, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 14, 0, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 12, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 8, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, 127, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -13, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 10};
        printByteData(bufTemp2);
    }


}
