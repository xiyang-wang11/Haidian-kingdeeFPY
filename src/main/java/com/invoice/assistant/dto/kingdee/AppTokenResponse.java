package com.invoice.assistant.dto.kingdee;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class AppTokenResponse {
    private String state;
    private Boolean status;

    private Data data;

    @lombok.Data
    public static class Data {
        @JSONField(name = "app_token")
        private String appToken;
        private Boolean success;
        @JSONField(name = "error_desc")
        private String errorDesc;
        @JSONField(name = "error_code")
        private String errorCode;
        @JSONField(name = "expire_time")
        private Long expireTime;
    }

    public String getAppToken() {
        return data != null ? data.getAppToken() : null;
    }

    public String getMessage() {
        return data != null ? data.getErrorDesc() : null;
    }
}
