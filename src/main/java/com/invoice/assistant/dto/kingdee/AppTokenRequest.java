package com.invoice.assistant.dto.kingdee;

import lombok.Data;

@Data
public class AppTokenRequest {
    private String appId;
    private String appSecret;
    private String accountId;
}
