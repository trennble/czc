package com.lj.czc.pojo.vo;

import lombok.Data;

/**
 * @author: jiangbo
 * @create: 2020-12-19
 **/
@Data
public class SkuInfoDto {

    private Long skuId;

    private String skuImage;

    private String hPrice;
    private String kaPrice;
    private String wPrice;

    // 商品名称
    private String wareName;

    private StockInfo stockInfo;
}
