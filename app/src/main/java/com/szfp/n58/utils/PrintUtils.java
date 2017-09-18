package com.szfp.n58.utils;

/**
 * author：ct on 2017/9/18 17:39
 * email：cnhttt@163.com
 */

public class PrintUtils {
    public static void printData(String title, String message) {
        BluetoothService.Begin();
        BluetoothService.LF();
        BluetoothService.SetAlignMode((byte) 1);
        BluetoothService.SetLineSpacing((byte)40);
        BluetoothService.SetFontEnlarge((byte) 0x01);
        BluetoothService.BT_Write(title);
        BluetoothService.LF();
        BluetoothService.SetAlignMode((byte)0);//左对齐
        BluetoothService.SetFontEnlarge((byte)0x00);//默认宽度、默认高度
        BluetoothService.BT_Write(message+"\r");
        BluetoothService.BT_Write("SERVED BY: " +"ADMIN"+"\r");
        BluetoothService.BT_Write(" "+"\r");
        BluetoothService.BT_Write(" "+"\r");
    }
}
