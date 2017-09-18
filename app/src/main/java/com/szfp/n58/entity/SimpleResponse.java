package com.szfp.n58.entity;

/**
 * author：ct on 2017/9/18 15:57
 * email：cnhttt@163.com
 */

public class SimpleResponse {
    private static final long serialVersionUID = -1477609349345966116L;

    public int code;
    public String msg;

    public FResponse toLzyResponse() {
        FResponse lzyResponse = new FResponse();
        lzyResponse.code = code;
        lzyResponse.msg = msg;
        return lzyResponse;
    }
}
