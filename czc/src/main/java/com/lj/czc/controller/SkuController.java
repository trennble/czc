package com.lj.czc.controller;

import com.lj.czc.common.PageResult;
import com.lj.czc.service.MonitorServiceImpl;
import com.lj.czc.pojo.bean.Sku;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

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
    public PageResult<Sku> list(String cookie){
        List<Sku> skus = monitorService.list(cookie);
        List<Sku> filterSkus = skus.stream().filter(i -> i.getDesc().equals("有货")).collect(toList());
        List<Sku> sortedSkus = filterSkus.stream()
                .sorted(Comparator.comparingInt(Sku::getSerialNumber).thenComparingLong(Sku::getLastUpdateTs).reversed())
                .collect(toList());
        int size = filterSkus.size();
        return new PageResult<Sku>(1, size, size, 1, sortedSkus);
    }

    @GetMapping("list/ts")
    public PageResult<Sku> list(String cookie, Long ts){
        List<Sku> skus = monitorService.list(cookie);
        List<Sku> sortedSkus = skus.stream()
                .filter(i -> i.getLastUpdateTs() > ts)
                .collect(toList());
        int size = skus.size();
        return new PageResult<>(1, size, size, 1, sortedSkus);
    }

    @GetMapping("monitor-list")
    public void monitor(){
        monitorService.monitorList();
    }


}
