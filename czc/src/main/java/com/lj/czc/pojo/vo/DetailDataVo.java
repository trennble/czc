package com.lj.czc.pojo.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author: jiangbo
 * @create: 2020-12-27
 **/
@Data
public class DetailDataVo {

    public static final String MODEL_PRICE = "MODEL_PRICE";
    public static final String JD_PRICE = "JD_PRICE";
    public static final String KA_PRICE = "KA_PRICE";
    public static final String MARKET_PRICE = "MARKET_PRICE";

    private String skuId;

    private BaseInfo baseInfo;

    private Map<String, String> bpMap;

    private ProductStockVo productStockVo;

    private List<SimilarProduct> similarProducts;
}
