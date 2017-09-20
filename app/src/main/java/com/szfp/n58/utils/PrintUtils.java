package com.szfp.n58.utils;

import android.graphics.Bitmap;

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

    public static void printData(String message, String title, Bitmap signBitmap) {

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
        byte[] sendData = null;
        Bitmap bitmap = signBitmap;
        BluetoothService.BT_Write(" "+"\r");
        BluetoothService.BT_Write(" "+"\r");
    }
    public static void printData(String message, String title, String path) {

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
        byte[] sendData = null;
        PrintPic pg = new PrintPic();
        pg.initCanvas(384);
        pg.initPaint();
        pg.drawImage(0, 0, path);
        sendData = pg.printDraw();
        BluetoothService.BT_Write(sendData);   //打印byte流数据
        BluetoothService.BT_Write(" "+"\r");
        BluetoothService.BT_Write(" "+"\r");
    }

}
