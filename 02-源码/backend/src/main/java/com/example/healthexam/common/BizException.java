package com.example.healthexam.common;

/** 业务异常：状态机非法流转、容量已满、权限不足等一律抛这个，由全局处理器转成 400/403 */
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String msg) {
        this(400, msg);
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
