package com.lj.czc.controller;

import com.lj.czc.common.PageResult;
import com.lj.czc.service.MonitorServiceImpl;
import com.lj.czc.pojo.bean.Sku;
import com.lj.czc.service.SkuServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @Autowired
    private SkuServiceImpl skuService;

    /**
     * 商品列表
     * @param cookie 用户指定的cookie，在系统默认的cookie失效时使用
     * @return
     */
    @GetMapping("list")
    public PageResult<Sku> list(String cookie){
        List<Sku> skus = monitorService.list(cookie);
        List<Sku> sortedSkus = skus.stream()
                .sorted(Comparator.comparingInt(Sku::getSerialNumber).thenComparingLong(Sku::getLastUpdateTs).reversed())
                .collect(toList());
        int size = skus.size();
        return new PageResult<Sku>(1, size, size, 1, sortedSkus);
    }

    /**
     * 手动触发商品监控
     */
    @GetMapping("monitor-list")
    public void monitor(){
        monitorService.monitorList();
    }

    /**
     * 设置通知价格
     * @param skuId 设置的商品id
     * @param notifyPrice 设置的通知价格
     */
    @PutMapping("price-notify")
    public void priceNotify(String skuId, String notifyPrice){
        Sku sku = skuService.findById(skuId).orElseThrow(() -> new RuntimeException("没有找到对应的商品ID"));
        sku.setNotifyPrice(notifyPrice);
        skuService.save(sku);
    }


}
