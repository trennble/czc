package com.lj.czc.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lj.czc.pojo.bean.Config;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: jiangbo
 * @create: 2021-01-05
 **/
@Service
public class CacheServiceImpl implements InitializingBean {

    @Autowired
    private ConfigServiceImpl configService;

    LoadingCache<Config.ConfigEnum, Integer> configCache;


    @Override
    public void afterPropertiesSet() {
        configCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build(
                        new CacheLoader<Config.ConfigEnum, Integer>() {
                            @Override
                            public Integer load(Config.ConfigEnum configEnum) {
                                String moutaiStr = configService.find(configEnum)
                                        .orElseThrow(() -> new RuntimeException("请设置茅台价格")).getValue();
                                return Integer.valueOf(moutaiStr);
                            }
                        });
    }

    public Integer getMoutai() {
        return configCache.getUnchecked(Config.ConfigEnum.MOUTAI);
    }

    public Integer getProfit() {
        return configCache.getUnchecked(Config.ConfigEnum.PROFIT);
    }

    public void refreshConfig(Config.ConfigEnum configEnum) {
        configCache.refresh(configEnum);
    }

}
