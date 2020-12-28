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
}
