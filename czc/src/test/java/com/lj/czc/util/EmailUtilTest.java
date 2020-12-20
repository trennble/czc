package com.lj.czc.util;

import com.lj.czc.pojo.SkuInfo;
import org.junit.jupiter.api.Test;

/**
 * @author: jiangbo
 * @create: 2020-11-15
 **/
public class EmailUtilTest {

    @Test
    public void testSendEmail(){
        EmailUtil.sendStatusChange(new SkuInfo(10001L, "测试商品名称", "测试状态"));
    }
}
