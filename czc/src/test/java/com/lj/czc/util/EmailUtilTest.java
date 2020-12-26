package com.lj.czc.util;

import com.lj.czc.pojo.bean.Sku;
import org.junit.jupiter.api.Test;

/**
 * @author: jiangbo
 * @create: 2020-11-15
 **/
public class EmailUtilTest {

    @Test
    public void testSendEmail(){
        EmailUtil.sendStatusChange(new Sku("10001", "测试商品名称", "测试状态"));
    }
}
