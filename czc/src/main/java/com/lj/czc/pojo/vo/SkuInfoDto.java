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

    private Integer hPrice;
    private Integer kaPrice;
    private Integer wPrice;

    // 商品名称
    private String wareName;

    private StockInfo stockInfo;
}
