package com.lj.czc.pojo.bean;

import com.lj.czc.pojo.vo.SkuInfoDto;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;

/**
 * @author: jiangbo
 * @create: 2020-12-19
 **/
@Data
@NoArgsConstructor
public class Sku {
    // 商品id
    @Id
    private String skuId;
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

    public Sku(String skuId, String name, String desc) {
        this.skuId = skuId;
        this.name = name;
        this.desc = desc;
        this.lastUpdateTs = System.currentTimeMillis();
    }

    public Sku(String skuId, String name, String desc, String hPrice, String wPrice, Integer serialNumber) {
        this.skuId = skuId;
        this.name = name;
        this.desc = desc;
        this.hPrice = hPrice;
        this.wPrice = wPrice;
        this.serialNumber = serialNumber;
        this.lastUpdateTs = System.currentTimeMillis();
    }

    public static Sku generate(SkuInfoDto skuInfoDto, Integer serialNumber) {
        Sku sku = new Sku();
        sku.skuId = String.valueOf(skuInfoDto.getSkuId());
        sku.name = skuInfoDto.getWareName();
        sku.desc = skuInfoDto.getStockInfo().getDesc();
        sku.hPrice = skuInfoDto.getHPrice();
        sku.wPrice = skuInfoDto.getWPrice();
        sku.serialNumber = serialNumber;
        sku.lastUpdateTs = System.currentTimeMillis();
        return sku;
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
