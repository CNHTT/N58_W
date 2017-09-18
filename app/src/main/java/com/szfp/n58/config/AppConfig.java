package com.szfp.n58.config;

import com.newpos.mpos.iInterface.ICommunication;
import com.szfp.n58.widget.DeviceDialog;

/**
 * author：ct on 2017/9/18 10:49
 * email：cnhttt@163.com
 */

public class AppConfig {

    /**
     * 设备是否打开
     */
    public static boolean deviceOpen = false;

    /**
     * 是否是蓝牙通道
     */
    public static boolean isBTChannel = true;

    /**
     * 通信通道
     */
    public static ICommunication communication;

    public static DeviceDialog deviceDialog;

    public static boolean deviceDialogShowing = false;
}
