package com.invoice.assistant.dto;

import lombok.Data;

/** 金蝶回调接口响应体，必须按此格式返回 */
@Data
public class KingdeeCallbackResponse {

    private String message;
    private String code;
    private Boolean success;

    public static KingdeeCallbackResponse ok() {
        KingdeeCallbackResponse r = new KingdeeCallbackResponse();
        r.success = true;
        r.code = "0";
        r.message = "success";
        return r;
    }

    public static KingdeeCallbackResponse fail(String message) {
        KingdeeCallbackResponse r = new KingdeeCallbackResponse();
        r.success = false;
        r.code = "9999";
        r.message = message;
        return r;
    }
}
