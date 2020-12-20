package com.lj.czc;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, @Autowired ClientHttpRequestFactory clientHttpRequestFactory) {
        RestTemplate restTemplate = builder.build();// 生成一个RestTemplate实例
        restTemplate.setRequestFactory(clientHttpRequestFactory);
        return restTemplate;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient().newBuilder().protocols(Collections.singletonList(Protocol.HTTP_1_1)).build();
    }

    /**
     * 客户端请求链接策略
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory(@Autowired OkHttpClient okHttpClient) {
        OkHttp3ClientHttpRequestFactory clientHttpRequestFactory = new OkHttp3ClientHttpRequestFactory(okHttpClient);
        clientHttpRequestFactory.setConnectTimeout(70000); // 连接超时时间/毫秒
        clientHttpRequestFactory.setReadTimeout(80000); // 读写超时时间/毫秒
        clientHttpRequestFactory.setWriteTimeout(80000);
        return clientHttpRequestFactory;
    }
}
