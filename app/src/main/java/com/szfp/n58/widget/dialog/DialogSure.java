package com.szfp.n58.widget.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.szfp.n58.R;
import com.szfp.n58.utils.RegUtils;
import com.szfp.n58.utils.TextUtils;


/**
 * author：ct on 2017/8/31 16:08
 * email：cnhttt@163.com
 */

public class DialogSure extends BaseDialog {

    private ImageView mIvLogo;
    private TextView mTvTitle;
    private TextView mTvContent;
    private TextView mTvSure;

    public ImageView getIvLogo() {
        return mIvLogo;
    }

    public TextView getTvTitle() {
        return mTvTitle;
    }

    public TextView getTvSure() {
        return mTvSure;
    }

    public TextView getTvContent() {
        return mTvContent;
    }

    public void setLogo(int resId) {
        mIvLogo.setImageResource(resId);
    }

    public void setTitle(String title) {
        mTvTitle.setText(title);
    }

    public void setSure(String content) {
        mTvSure.setText(content);
    }


    public void setContent(String str) {
        if (RegUtils.isURL(str)) {
            // 响应点击事件的话必须设置以下属性
            mTvContent.setMovementMethod(LinkMovementMethod.getInstance());
            mTvContent.setText(TextUtils.getBuilder("").setBold().append(str).setUrl(str).create());//当内容为网址的时候，内容变为可点击
        } else {
            mTvContent.setText(str);
        }

    }

    private void initView() {
        View dialog_view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sure, null);
        mTvSure = (TextView) dialog_view.findViewById(R.id.tv_sure);
        mTvTitle = (TextView) dialog_view.findViewById(R.id.tv_title);
        mTvTitle.setTextIsSelectable(true);
        mTvContent = (TextView) dialog_view.findViewById(R.id.tv_content);
        mTvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        mTvContent.setTextIsSelectable(true);
        mIvLogo = (ImageView) dialog_view.findViewById(R.id.iv_logo);
        setContentView(dialog_view);
    }

    public DialogSure(Context context, int themeResId) {
        super(context, themeResId);
        initView();
    }

    public DialogSure(Context context, boolean cancelable, DialogInterface.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        initView();
    }

    public DialogSure(Context context) {
        super(context);
        initView();
    }

    public DialogSure(Activity context) {
        super(context);
        initView();
    }

    public DialogSure(Context context, float alpha, int gravity) {
        super(context, alpha, gravity);
        initView();
    }
}
