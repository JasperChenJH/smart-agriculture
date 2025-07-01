package com.soultalk.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class R<T> {
    private int code;
    private String msg;
    private T data;

    public R(int code) {
        this.code = code;
        this.msg = "";
        this.data = null;
    }

    public R(int code, String msg) {
        this.code = code;
        this.msg = msg;
        this.data = null;
    }

    public static R Success(Object data) {
        return new R(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    public static R Success(String message, Object data) {
        return new R(ResultCodeEnum.SUCCESS.getCode(), message, data);
    }

    public static R Success() {
        return Success("");
    }

    public static R Failed(String msg) {
        return new R(ResultCodeEnum.SYSTEM_EXCEPTION.getCode(), msg);
    }

    public static R Failed() {
        return Failed("Failed");
    }

    public static R Failed(int code, String msg) {
        return new R(code, msg);
    }

    public boolean succeeded() {
        return getCode() == ResultCodeEnum.SUCCESS.getCode();
    }


    public boolean failed() {
        return getCode() != ResultCodeEnum.SUCCESS.getCode();
    }

}
