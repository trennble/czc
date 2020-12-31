package com.lj.czc.pojo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

/**
 * @author: jiangbo
 * @create: 2020-12-28
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Config {
    @Id
    private String key;
    private String value;

    public enum ConfigEnum{
        PROFIT("profit","利润"),
        MOUTAI("moutai","茅台");

        String key;
        String name;

        ConfigEnum(String key, String name){
            this.key = key;
            this.name = name;
        }

        public String getKey(){
            return key;
        }

        public String getName(){
            return name;
        }

        public static ConfigEnum ofKey(String key){
            for (ConfigEnum configEnum : ConfigEnum.values()) {
                if (configEnum.key.equals(key)){
                    return configEnum;
                }
            }
            return null;
        }

        public static ConfigEnum ofName(String value){
            for (ConfigEnum configEnum : ConfigEnum.values()) {
                if (configEnum.name.equals(value)){
                    return configEnum;
                }
            }
            return null;
        }
    }
}
