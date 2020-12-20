package com.lj.czc.common;

import lombok.Data;

/**
 * @author: jiangbo
 * @create: 2020-12-20
 **/
@Data
public class RestResult<T> {
    private Integer code;
    private String message;
    private T result;
    private Long timestamp;

    public static RestResult<Object> success(){
        return success(null);
    }

    public static <T> RestResult<T> success(T result){
        RestResult<T> restResult = new RestResult<>();
        restResult.code = 0;
        restResult.result = result;
        restResult.timestamp = System.currentTimeMillis();
        return restResult;
    }
}
