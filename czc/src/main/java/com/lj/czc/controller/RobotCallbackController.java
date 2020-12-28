package com.lj.czc.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lj.czc.pojo.DingRequestBody;
import com.lj.czc.pojo.bean.Config;
import com.lj.czc.pojo.bean.Sku;
import com.lj.czc.service.ConfigServiceImpl;
import com.lj.czc.service.RobotServiceImpl;
import com.lj.czc.service.SkuServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * @author: jiangbo
 * @create: 2020-12-26
 **/
@RestController
@RequestMapping("callback")
public class RobotCallbackController {

    @Autowired
    private RobotServiceImpl robotService;

    @Autowired
    private SkuServiceImpl skuService;

    @Autowired
    private ConfigServiceImpl configService;

    private static final String skuKey = "商品";

    private static final String priceKey = "价格";

    @PostMapping("ding-robot")
    public void dingRobot(@RequestBody DingRequestBody dingRequestBody) {
        String content = dingRequestBody.getText().getContent();
        List<String> keywords = Arrays.stream(ConfigServiceImpl.ConfigEnum.values()).map(ConfigServiceImpl.ConfigEnum::getValue).collect(toList());
        boolean contain = false;
        for (String keyword : keywords) {
            contain = content.contains(keyword);
            if (contain){
                break;
            }
        }
        if (contain) {
            config(dingRequestBody);
        } else {
            try {
                String skuId = content.substring(content.indexOf(skuKey) + skuKey.length(), content.indexOf(priceKey));
                String price = content.substring(content.indexOf(priceKey) + priceKey.length());
                Optional<Sku> skuOptional = skuService.findById(skuId);
                if (skuOptional.isPresent()) {
                    Sku sku = skuOptional.get();
                    sku.setNotifyPrice(price);
                    robotService.send("设置价格成功\n" +
                            "商品id：" + skuId + "\n" +
                            "商品名称：" + sku.getName() + "\n" +
                            "批发价格：" + sku.getWPrice() + "\n" +
                            "提醒价格：" + sku.getNotifyPrice());
                } else {
                    robotService.send("设置价格失败，没有找到对应的商品\n" + "商品id：" + skuId);

                }
            } catch (Exception e) {
                e.printStackTrace();
                robotService.send("设置价格失败，解析数据错误，请输入正确的格式");
            }
        }
    }

    public void config(DingRequestBody dingRequestBody){
        try {
            String content = dingRequestBody.getText().getContent();
            List<Config> configs = configService.parseAndSave(content);
            robotService.send("设置参数成功\n" + JSON.toJSONString(configs, SerializerFeature.PrettyFormat));
        } catch (Exception e) {
            e.printStackTrace();
            robotService.send("设置价格失败，解析数据错误，请输入正确的格式");
        }
    }

}
