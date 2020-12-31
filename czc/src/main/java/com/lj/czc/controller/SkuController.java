package com.lj.czc.controller;

import com.lj.czc.common.PageResult;
import com.lj.czc.pojo.bean.Sku;
import com.lj.czc.pojo.vo.SkuVo;
import com.lj.czc.service.MonitorServiceImpl;
import com.lj.czc.service.SkuServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
     * @return
     */
    @GetMapping("list")
    public PageResult<Sku> list(boolean sort){
        List<Sku> all = skuService.findAll();
        Comparator<Sku> comparator;
        if (sort) {
            comparator = Comparator.comparing(Sku::getSpuId);
        } else {
            comparator = Comparator.comparingLong(Sku::getLastUpdateTs).reversed();
        }
        List<Sku> sortedSkus = all.stream().sorted(comparator).collect(toList());
        int size = all.size();
        return new PageResult<Sku>(1, size, size, 1, sortedSkus);
    }

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
        monitorService.monitorPrice();
    }

    /**
     * 设置通知价格
     * @param skuId 设置的商品id
     * @param soldPrice 设置的通知价格
     * @return
     */
    @PutMapping("price-sold")
    public Sku priceSold(String skuId, String soldPrice){
        return monitorService.setSoldPrice(skuId, soldPrice);
    }

    /**
     * 添加购物车
     */
    @PostMapping("add-cart")
    public void addCart(){
        monitorService.addAllToCart();
    }

    /**
     * 购物车列表
     * @return
     */
    @GetMapping("cart-list")
    public List<SkuVo> cartList(){
        return monitorService.cartList();
    }


    /**
     * 初始化购物车
     * @return
     */
    @GetMapping("init-spu")
    public void initSpuId(){
        monitorService.initSpuId();
    }


}
