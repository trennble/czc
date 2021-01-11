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
        return new PageResult<>(1, size, size, 1, sortedSkus);
    }

    /**
     * 设置通知价格
     * @param skuId 设置的商品id
     * @param soldPrice 设置的通知价格
     * @return
     */
    @PutMapping("price-sold")
    public Sku priceSold(String skuId, Integer soldPrice){
        return skuService.setSoldPrice(skuId, soldPrice);
    }

    /**
     * 初始化购物车
     */
    @PostMapping("init-cart")
    public void addCart(){
        skuService.addAllToCart();
    }

    /**
     * 购物车商品列表
     * @return
     */
    @GetMapping("cart-list")
    public List<SkuVo> cartList(){
        return skuService.cartList();
    }


    /**
     * 初始化商品的关联商品并使用当前商品的skuid作为spuid
     * @return
     */
    @GetMapping("init-spu")
    public void initSpuId(){
        skuService.initSpuId();
    }


}
