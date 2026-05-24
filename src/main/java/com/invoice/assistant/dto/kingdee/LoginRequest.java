package com.invoice.assistant.dto.kingdee;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class LoginRequest {
    private String accountId;
    private String user;
    private String password;
    @JSONField(name = "apptoken")
    private String appToken;
    private String loginType;
}
