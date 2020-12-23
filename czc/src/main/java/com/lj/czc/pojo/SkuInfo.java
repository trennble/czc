package com.lj.czc.pojo;

import com.lj.czc.vo.SkuInfoDto;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: jiangbo
 * @create: 2020-12-19
 **/
@Data
@NoArgsConstructor
public class SkuInfo {
    // 商品id
    private Long skuId;
    // 商品名称
    private String name;
    // 商品状态
    private String desc;
    // 京东价格
    private String hPrice;
    // 批发价格
    private String wPrice;
    // 上次更新时间
    private Long lastUpdateTs;
    // 序列号，标志当前商品第几次爬去
    private Integer serialNumber;

    public SkuInfo(Long skuId, String name, String desc) {
        this.skuId = skuId;
        this.name = name;
        this.desc = desc;
        this.lastUpdateTs = System.currentTimeMillis();
    }

    public SkuInfo(Long skuId, String name, String desc, String hPrice, String wPrice, Integer serialNumber) {
        this.skuId = skuId;
        this.name = name;
        this.desc = desc;
        this.hPrice = hPrice;
        this.wPrice = wPrice;
        this.serialNumber = serialNumber;
        this.lastUpdateTs = System.currentTimeMillis();
    }

    public static SkuInfo generate(SkuInfoDto skuInfoDto, Integer serialNumber) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.skuId = skuInfoDto.getSkuId();
        skuInfo.name = skuInfoDto.getWareName();
        skuInfo.desc = skuInfoDto.getStockInfo().getDesc();
        skuInfo.hPrice = skuInfoDto.getHPrice();
        skuInfo.wPrice = skuInfoDto.getWPrice();
        skuInfo.serialNumber = serialNumber;
        skuInfo.lastUpdateTs = System.currentTimeMillis();
        return skuInfo;
    }

    public void setWPrice(String wPrice){
        this.wPrice = wPrice;
        this.lastUpdateTs = System.currentTimeMillis();
    }

    public void setDesc(String desc){
        this.desc = desc;
        this.lastUpdateTs = System.currentTimeMillis();
    }

    public void setSerialNumber(Integer serialNumber){
        this.serialNumber = serialNumber;
        this.lastUpdateTs = System.currentTimeMillis();
    }
}
