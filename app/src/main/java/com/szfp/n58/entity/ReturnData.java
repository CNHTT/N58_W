package com.szfp.n58.entity;

import com.szfp.n58.data.TransactionData;

/**
 * author：ct on 2017/9/18 15:10
 * email：cnhttt@163.com
 */

public class ReturnData {
    private String str;
    private TransactionData data;

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public TransactionData getData() {
        return data;
    }

    public void setData(TransactionData data) {
        this.data = data;
    }
}
