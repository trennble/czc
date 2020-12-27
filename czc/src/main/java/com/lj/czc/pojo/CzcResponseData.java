package com.lj.czc.pojo;

import lombok.Data;

/**
 * @author: jiangbo
 * @create: 2020-12-27
 **/
@Data
public class CzcResponseData<T> {

    private T data;

    private Integer code;

    private String msg;
}
