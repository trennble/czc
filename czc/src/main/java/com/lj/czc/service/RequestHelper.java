package com.lj.czc.service;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author: jiangbo
 * @create: 2021-01-11
 **/
@Slf4j
@Service
public class RequestHelper {

    @Value("${cookie}")
    private String cookie;

    private static final String CODE = "-XWJ7oIOzkBMCQobPP_G4QLsUK2-f6_nOFsEDSm6rRk=";

    @Autowired
    private RestTemplate restTemplate;


    public  <T, E> Optional<E> sendGetRequest(String url, Class<T> clazz, Function<T, E> function) {
        HttpEntity<String> entity = getEntity(null);
        return sendRequest(url, HttpMethod.GET, entity, clazz, function);
    }

    public  <T, E> Optional<E> sendPostRequest(String url, String requestBody, Class<T> clazz, Function<T, E> function) {
        HttpEntity<String> entity = getEntity(requestBody);
        return sendRequest(url, HttpMethod.POST, entity, clazz, function);
    }

    @NotNull
    private HttpEntity<String> getEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        String cookie = String.format("sam_cookie_activity=%s; activityCode=\"%s\"", this.cookie, CODE);
        headers.add("cookie", cookie);
        HttpEntity<String> entity = Strings.isBlank(requestBody) ? new HttpEntity<>(headers) : new HttpEntity<>(requestBody, headers);
        return entity;
    }

    private <T, E> Optional<E> sendRequest(String url, HttpMethod httpMethod, HttpEntity<?> httpEntity, Class<T> clazz, Function<T, E> function) {
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, httpMethod, httpEntity, String.class);
        HttpStatus statusCode = responseEntity.getStatusCode();
        String body = responseEntity.getBody();
        if (HttpStatus.OK == statusCode) {
            try {
                JSONObject responseData = JSONObject.parseObject(body);
                T data = responseData.getObject("data", clazz);
                Integer code = responseData.getObject("code", Integer.class);
                if (code == 1000) {
                    return Optional.ofNullable(function.apply(data));
                } else {
                    log.error("服务器状态码错误[{}]", body);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                log.error("解析json异常[{}]", body);
            }
        } else {
            log.error("请求异常http status[{}]，response body[{}]", statusCode.value(), body);
        }
        return Optional.empty();
    }
}
