package com.lj.czc.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: jiangbo
 * @create: 2020-12-27
 **/
@Data
public class SimilarProduct {
    private Integer dim;
    private List<SaleAttr> saleAttrList;
}
