package com.example.healthexam.common;

import lombok.Data;

import java.util.UUID;

/**
 * 统一返回体：业务码 + 提示 + 数据 + 请求编号
 * 所有接口（含异常）都返回该结构，便于前端统一拦截。
 */
@Data
public class R<T> {
    private int code;
    private String msg;
    private T data;
    private String requestId;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        r.requestId = UUID.randomUUID().toString().substring(0, 8);
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        r.requestId = UUID.randomUUID().toString().substring(0, 8);
        return r;
    }
}
