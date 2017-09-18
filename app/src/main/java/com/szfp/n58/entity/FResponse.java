package com.szfp.n58.entity;

import java.io.Serializable;

/**
 * author：ct on 2017/9/18 15:40
 * email：cnhttt@163.com
 */

public class FResponse<T> implements Serializable {

    private static final long serialVersionUID = 5213230387175987834L;

    public int code;
    public String msg;
    public T data;

    @Override
    public String toString() {
        return "LzyResponse{\n" +//
                "\tcode=" + code + "\n" +//
                "\tmsg='" + msg + "\'\n" +//
                "\tdata=" + data + "\n" +//
                '}';
    }
}