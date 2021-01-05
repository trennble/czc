package com.lj.czc.pojo.vo;

import lombok.Data;

/**
 * @author: jiangbo
 * @create: 2020-12-27
 **/
@Data
public class SkuVo {

    private String skuId;

    /**
     * 1: 不可购买 0: 可购买
     */
    private Integer goodsState;

    private String modelPrice;

    private StockInfo stockInfo;
}
