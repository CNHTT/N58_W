package com.szfp.n58.entity;

/**
 * author：ct on 2017/9/18 10:29
 * email：cnhttt@163.com
 */

public class LabelInfo {
    int drawableId;
    String labelValue;

    static int index = 1;

    public static void resetIndex() {
        index = 1;
    }

    public LabelInfo(String labelValue) {
        this.labelValue = index + ". " + labelValue;
        index++;
    }

    public int getDrawableId() {
        return drawableId;
    }

    public void setDrawableId(int drawableId) {
        this.drawableId = drawableId;
    }

    public String getLabelValue() {
        return labelValue;
    }

    public void setLabelValue(String labelValue) {
        this.labelValue = labelValue;
    }
}
