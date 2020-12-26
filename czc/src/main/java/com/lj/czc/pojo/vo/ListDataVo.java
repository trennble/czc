package com.lj.czc.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: jiangbo
 * @create: 2020-12-19
 **/
@Data
public class ListDataVo {

    // 待补充
    private Integer level;
    private Integer pageCount;
    private Integer pageIndex;
    private Integer pageSize;
    private Integer resultCount;

    private List<SkuInfoDto> skuInfoDtos;

}
