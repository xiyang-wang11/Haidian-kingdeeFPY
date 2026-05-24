package com.invoice.assistant.dto;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok() {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        return r;
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = ok();
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(String message) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.message = message;
        return r;
    }
}
