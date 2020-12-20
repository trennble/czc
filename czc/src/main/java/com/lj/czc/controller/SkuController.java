package com.lj.czc.controller;

import com.lj.czc.common.PageResult;
import com.lj.czc.service.MonitorServiceImpl;
import com.lj.czc.pojo.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author: jiangbo
 * @create: 2020-12-20
 **/
@RestController
@RequestMapping("sku")
public class SkuController {

    @Autowired
    private MonitorServiceImpl monitorService;

    @GetMapping("list")
    public PageResult<SkuInfo> list(String cookie){
        List<SkuInfo> skuInfos = monitorService.list(cookie);
        List<SkuInfo> filterSkuInfos = skuInfos.stream().filter(i -> i.getDesc().equals("有货")).collect(toList());
        List<SkuInfo> sortedSkuInfos = filterSkuInfos.stream()
                .sorted(Comparator.comparingInt(SkuInfo::getSerialNumber).thenComparingLong(SkuInfo::getLastUpdateTs).reversed())
                .collect(toList());
        int size = filterSkuInfos.size();
        return new PageResult<SkuInfo>(1, size, size, 1, sortedSkuInfos);
    }

    @GetMapping("list/ts")
    public PageResult<SkuInfo> list(String cookie, Long ts){
        List<SkuInfo> skuInfos = monitorService.list(cookie);
        List<SkuInfo> sortedSkuInfos = skuInfos.stream()
                .filter(i -> i.getLastUpdateTs() > ts)
                .collect(toList());
        int size = skuInfos.size();
        return new PageResult<>(1, size, size, 1, sortedSkuInfos);
    }
}
