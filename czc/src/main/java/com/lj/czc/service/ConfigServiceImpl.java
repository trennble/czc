package com.lj.czc.service;

import com.lj.czc.pojo.bean.Config;
import com.lj.czc.repo.ConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: jiangbo
 * @create: 2020-12-28
 **/
@Slf4j
@Service
public class ConfigServiceImpl {

    @Autowired
    private ConfigRepository configRepository;

    /**
     * 解析并存储配置
     * @param content 解析内容 格式为每个配置使用换行隔开，配置名称和值之间使用空格分隔
     * @return
     */
    public List<Config> parseAndSave(String content){
        if (Strings.isBlank(content)){
            return new ArrayList<>();
        }
        content = content.trim();
        List<Config> setConfigs = new ArrayList<>();
        for (String s : content.split("\n")) {
            String[] line = s.split(" ");
            String key = ConfigEnum.ofValue(line[0].trim()).getKey();
            String value = line[1].trim();
            Config config = setConfig(key, value);
            setConfigs.add(config);
        }
        return setConfigs;
    }

    public Config setConfig(String name, String value){
        Config config= new Config(name, value);
        return save(config);
    }

    public Config save(Config config){
        return configRepository.save(config);
    }

    public List<Config> findAll(){
        return configRepository.findAll();
    }

    public enum ConfigEnum{
        PROFIT("profit","利润"),
        MOUTAI("moutai","茅台");

        String key;
        String value;

        ConfigEnum(String key, String value){
            this.key = key;
            this.value = value;
        }

        public String getKey(){
            return key;
        }

        public String getValue(){
            return value;
        }

        public static ConfigEnum ofKey(String key){
            for (ConfigEnum configEnum : ConfigEnum.values()) {
                if (configEnum.key.equals(key)){
                    return configEnum;
                }
            }
            return null;
        }

        public static ConfigEnum ofValue(String value){
            for (ConfigEnum configEnum : ConfigEnum.values()) {
                if (configEnum.value.equals(value)){
                    return configEnum;
                }
            }
            return null;
        }
    }
}
