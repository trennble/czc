package com.lj.czc.pojo.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lj.czc.pojo.vo.DetailDataVo;
import com.lj.czc.pojo.vo.SkuInfoDto;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;
import java.util.List;

import static com.lj.czc.pojo.vo.DetailDataVo.JD_PRICE;
import static com.lj.czc.pojo.vo.DetailDataVo.MODEL_PRICE;

/**
 * @author: jiangbo
 * @create: 2020-12-19
 **/
@Data
@NoArgsConstructor
public class Sku {
    //spu id，同一个商品用第一个获取到信息的sku id作为spu id
    private String spuId;
    // 商品id
    @Id
    private String skuId;
    // 商品名称
    private String name;
    // 商品状态 1: 不可购买 0: 可购买
    private Integer goodsState;
    // 商品状态
    private String desc;
    // 京东价格
    private Integer hPrice;
    // 批发价格
    private Integer wPrice;
    // 通知价格 默认为(soldPrice - 利润)*6000/(7499-茅台价格)
    private Integer notifyPrice;
    // 卖出价格
    private Integer soldPrice;
    // 上次更新时间
    private Long lastUpdateTs;
    // 序列号，标志当前商品第几次爬去
    // private Integer serialNumber;
    // 关联的sku商品id
    @JsonIgnore
    private List<String> similarSkus;

    public Sku(String skuId, String name, String desc) {
        this.skuId = skuId;
        this.name = name;
        this.desc = desc;
        this.lastUpdateTs = System.currentTimeMillis();
    }

    public static Sku generate(SkuInfoDto skuInfoDto) {
        Sku sku = new Sku();
        sku.skuId = String.valueOf(skuInfoDto.getSkuId());
        sku.name = skuInfoDto.getWareName();
        sku.desc = skuInfoDto.getStockInfo().getDesc();
        sku.hPrice = skuInfoDto.getHPrice();
        sku.wPrice = skuInfoDto.getWPrice();
        sku.notifyPrice = skuInfoDto.getWPrice();
        sku.lastUpdateTs = System.currentTimeMillis();
        /**
         * 默认使用描述初始化商品状态，可能会不正确
         */
        sku.goodsState = "有货".equals(sku.desc) ? 0 : 1;
        return sku;
    }

    public static Sku generate(DetailDataVo detailDataVo){
        Sku sku = new Sku();
        sku.skuId = String.valueOf(detailDataVo.getSkuId());
        sku.name = detailDataVo.getBaseInfo().getSkuName();
        sku.desc = detailDataVo.getProductStockVo().getDesc();
        sku.hPrice = (int)Double.parseDouble(detailDataVo.getBpMap().get(JD_PRICE));
        sku.wPrice = (int)Double.parseDouble(detailDataVo.getBpMap().get(MODEL_PRICE));
        sku.notifyPrice = (int)Double.parseDouble(detailDataVo.getBpMap().get(MODEL_PRICE));
        sku.lastUpdateTs = System.currentTimeMillis();
        /**
         * 默认使用描述初始化商品状态，可能会不正确
         */
        sku.goodsState = "有货".equals(sku.desc) ? 0 : 1;
        return sku;
    }

    public void setWPrice(Integer wPrice){
        this.wPrice = wPrice;
        this.lastUpdateTs = System.currentTimeMillis();
    }

    public void setGoodsState(Integer state){
        this.goodsState = state;
        this.lastUpdateTs = System.currentTimeMillis();
    }

    public void setDesc(String desc){
        this.desc = desc;
        this.lastUpdateTs = System.currentTimeMillis();
    }
}
