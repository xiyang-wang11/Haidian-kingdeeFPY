package com.invoice.assistant.service;

/**
 * 金蝶鉴权服务，负责获取和缓存 appToken / accessToken
 */
public interface KingdeeAuthService {

    /** 获取 appToken（带缓存） */
    String getAppToken();

    /** 获取 accessToken（带缓存） */
    String getAccessToken();

    /** 强制刷新所有 token */
    void refreshTokens();
}
