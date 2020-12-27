package com.lj.czc.pojo.vo;

import lombok.Data;

/**
 * @author: jiangbo
 * @create: 2020-12-27
 **/
@Data
public class ProductStockVo {

    /**
     * 33：有货 34：该地区暂不支持配 无货 36：
     */
    private Integer state;

    private String desc;
}
