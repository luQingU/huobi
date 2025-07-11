package com.ruoyi.common.utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class UdunUtils {
    private static Logger logger = LoggerFactory.getLogger(UdunUtils.class);

    public static String post(String gateway, String merchantKey, String path, String body) {
        String rawBody = parseParams(merchantKey, body);
        logger.debug("HttpRequest POST {}",gateway+path);
        logger.debug("BODY {}",rawBody);
        HttpResponse response = HttpRequest.post(gateway + path)
                .body(rawBody)
                .execute();
        String resp =  response.body();
        logger.debug("HttpResponse {}",resp);
        return resp;
    }

    public static String parseParams(String merchantKey, String body) {
        Map<String, String> params = new HashMap<>();
        String timestamp = System.currentTimeMillis() + "";
        String nonce = RandomUtil.randomString(6);
        String sign = sign(merchantKey, timestamp, nonce, body);
        params.put("timestamp", timestamp);
        params.put("nonce", nonce);
        params.put("sign", sign);
        params.put("body", body);
        return JSONUtil.toJsonStr(params);
    }

    public static String sign(String key, String timestamp, String nonce, String body) {
        String raw = body + key + nonce + timestamp;
        return SecureUtil.md5(raw);
    }

    public static boolean checkSign(String key, String timestamp, String nonce, String body, String sign) {
        String checkSign = sign(key, timestamp, nonce, body);
        return checkSign.equals(sign);
    }
}
