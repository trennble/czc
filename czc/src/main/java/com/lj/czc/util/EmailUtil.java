package com.lj.czc.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.mail.MailUtil;
import com.lj.czc.pojo.bean.Sku;

import java.util.ArrayList;
import java.util.List;

import static com.lj.czc.service.MonitorServiceImpl.SKU_URL;

/**
 * @program: scrawlserver
 * @description: 发送邮件工具箱
 * @author: Liang Shan
 * @created: 2020/10/14 12:55
 * @blame ls Team
 */
public class EmailUtil {
    public static List<String> user = CollUtil.newArrayList("747765042@qq.com", "641811254@qq.com",
            "1243168364@qq.com", "1037844766@qq.com");
    public static List<String> admin = CollUtil.newArrayList("329863004@qq.com");


    public static void sendStatusChange(Sku sku) {
        String url = String.format(SKU_URL, sku.getSkuId());
        ArrayList<String> receivers = new ArrayList<>();
        receivers.addAll(user);
        receivers.addAll(admin);
        MailUtil.send(receivers, "诚至诚商品库存变更提示",
                "商品id：" + sku.getSkuId() + "\n" +
                        "商品名称：" + sku.getName() + "\n" +
                        "商品状态：" + sku.getDesc() + "\n" +
                        "京东价格：" + sku.getHPrice() + "\n" +
                        "批发价格：" + sku.getWPrice() + "\n" +
                        "商品链接：" + url, false);
    }

    public static void sendHtml(String content) {
        MailUtil.send(admin, "诚至诚程序异常提示", content, true);
    }
}
