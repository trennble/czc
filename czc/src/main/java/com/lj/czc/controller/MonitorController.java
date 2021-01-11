package com.lj.czc.controller;

import com.lj.czc.service.MonitorServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: jiangbo
 * @create: 2021-01-11
 **/
@RestController
@RequestMapping("monitor")
public class MonitorController {

    @Autowired
    private MonitorServiceImpl monitorService;

    /**
     * 手动触发新商品监控
     */
    @GetMapping("monitor-list")
    public void monitorList(){
        monitorService.monitorNewItem();
    }

    /**
     * 手动触发商品价格监控
     */
    @GetMapping("monitor-price")
    public void monitorPrice(){
        monitorService.monitorCart();
    }
}
