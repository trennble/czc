package com.lj.czc.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: jiangbo
 * @create: 2020-12-20
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private Integer pageNo;
    private Integer pageSize;
    private Integer totalCount;
    private Integer totalPage;
    private List<? extends T> data;
}
