package com.invoice.assistant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.invoice.assistant.dto.kingdee.AppTokenRequest;
import com.invoice.assistant.dto.kingdee.AppTokenResponse;
import com.invoice.assistant.dto.kingdee.LoginRequest;
import com.invoice.assistant.dto.kingdee.LoginResponse;
import com.invoice.assistant.service.KingdeeAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class KingdeeAuthServiceImpl implements KingdeeAuthService {

    @Value("${kingdee.api.get-app-token-url}")
    private String getAppTokenUrl;

    @Value("${kingdee.api.login-url}")
    private String loginUrl;

    @Value("${kingdee.api.app-id}")
    private String appId;

    @Value("${kingdee.api.app-secret}")
    private String appSecret;

    @Value("${kingdee.api.account-id}")
    private String accountId;

    @Value("${kingdee.api.user}")
    private String user;

    @Value("${kingdee.api.token-expire-minutes:55}")
    private int tokenExpireMinutes;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ReentrantLock lock = new ReentrantLock();

    private String cachedAppToken;
    private String cachedAccessToken;
    private LocalDateTime appTokenExpireAt;
    private LocalDateTime accessTokenExpireAt;

    @Override
    public String getAppToken() {
        if (cachedAppToken != null && LocalDateTime.now().isBefore(appTokenExpireAt)) {
            return cachedAppToken;
        }
        lock.lock();
        try {
            // 双重检查
            if (cachedAppToken != null && LocalDateTime.now().isBefore(appTokenExpireAt)) {
                return cachedAppToken;
            }
            return fetchAppToken();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getAccessToken() {
        if (cachedAccessToken != null && LocalDateTime.now().isBefore(accessTokenExpireAt)) {
            return cachedAccessToken;
        }
        lock.lock();
        try {
            if (cachedAccessToken != null && LocalDateTime.now().isBefore(accessTokenExpireAt)) {
                return cachedAccessToken;
            }
            return fetchAccessToken();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void refreshTokens() {
        lock.lock();
        try {
            cachedAppToken = null;
            cachedAccessToken = null;
            fetchAppToken();
            fetchAccessToken();
        } finally {
            lock.unlock();
        }
    }

    private String fetchAppToken() {
        log.info("正在获取金蝶 appToken...");
        AppTokenRequest req = new AppTokenRequest();
        req.setAppId(appId);
        req.setAppSecret(appSecret);
        req.setAccountId(accountId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        try {
            log.info("getAppToken 请求参数：appId={}, appSecret=[{}], accountId={}", appId, appSecret, accountId);
            ResponseEntity<String> resp = restTemplate.postForEntity(getAppTokenUrl, entity, String.class);
            log.info("getAppToken 原始响应：{}", resp.getBody());
            AppTokenResponse result = JSON.parseObject(resp.getBody(), AppTokenResponse.class);
            if (result == null || !Boolean.TRUE.equals(result.getStatus())) {
                String msg = result != null ? result.getMessage() : "响应为空";
                throw new RuntimeException("获取 appToken 失败：" + msg);
            }
            cachedAppToken = result.getAppToken();
            appTokenExpireAt = LocalDateTime.now().plusMinutes(tokenExpireMinutes);
            log.info("获取 appToken 成功，有效期至 {}", appTokenExpireAt);
            return cachedAppToken;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("调用 getAppToken 接口异常：" + e.getMessage(), e);
        }
    }

    private String fetchAccessToken() {
        log.info("正在获取金蝶 accessToken...");
        String appToken = getAppToken();

        LoginRequest req = new LoginRequest();
        req.setAccountId(accountId);
        req.setUser(user);
        req.setAppToken(appToken);
        req.setLoginType("mobile");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(req), headers);

        try {
            log.info("login 请求参数：user={}, accountId={}", user, accountId);
            ResponseEntity<String> resp = restTemplate.postForEntity(loginUrl, entity, String.class);
            log.info("login 原始响应：{}", resp.getBody());
            LoginResponse result = JSON.parseObject(resp.getBody(), LoginResponse.class);
            if (result == null || !Boolean.TRUE.equals(result.getStatus())) {
                String msg = result != null ? result.getMessage() : "响应为空";
                throw new RuntimeException("获取 accessToken 失败：" + msg);
            }
            cachedAccessToken = result.getAccessToken();
            accessTokenExpireAt = LocalDateTime.now().plusMinutes(tokenExpireMinutes);
            log.info("获取 accessToken 成功，有效期至 {}", accessTokenExpireAt);
            return cachedAccessToken;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("调用 login 接口异常：" + e.getMessage(), e);
        }
    }
}
