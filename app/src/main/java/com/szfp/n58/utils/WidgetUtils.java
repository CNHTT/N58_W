package com.szfp.n58.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.szfp.n58.R;
import com.szfp.n58.callback.OnShowPrintCallBack;

/**
 * author：ct on 2017/9/18 10:52
 * email：cnhttt@163.com
 */

public class WidgetUtils {
    /**
     * 跳转框
     *
     * @param msg
     * @return
     */
    public static Dialog getMyDialog(String msg, Context cxt) {
        ProgressDialog mypDialog = new ProgressDialog(cxt);
        mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mypDialog.setMessage(msg);
        mypDialog.setIndeterminate(false);
        mypDialog.setCancelable(false);
        return mypDialog;
    }

    /**
     * 提示信息
     *
     * @param msg
     */
    public static void mToast(Context cxt, String msg) {
        Toast.makeText(cxt, msg, 500).show();
    }

    /**
     * 弹出对话框
     *
     * @param cxt
     * @param title 弹出框标题
     * @param message 弹出信息
     */
    public static void showResult(Context cxt, String title, String message) {
        new AlertDialog.Builder(cxt)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(cxt.getString(R.string.sure),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
    } /**
     * 弹出对话框
     *  @param cxt
     * @param title 弹出框标题
     * @param message 弹出信息
     * @param onShowPrintCallBack
     */
    public static void showResultPrint(Context cxt, final String title, final String message, final OnShowPrintCallBack onShowPrintCallBack) {
        new AlertDialog.Builder(cxt)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(cxt.getString(R.string.sure),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("Sign and print", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onShowPrintCallBack.success(title,message);
                    }
                }).show();
    }
}
