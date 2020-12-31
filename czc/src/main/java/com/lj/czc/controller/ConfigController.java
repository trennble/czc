package com.lj.czc.controller;

import com.lj.czc.pojo.bean.Config;
import com.lj.czc.service.ConfigServiceImpl;
import com.lj.czc.service.RobotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * @author: jiangbo
 * @create: 2020-12-28
 **/
@RestController
@RequestMapping("config")
public class ConfigController {

    @Autowired
    private ConfigServiceImpl configService;

    @GetMapping("map")
    public Map<String, String> map(){
        List<Config> all = configService.findAll();
        return all.stream().collect(toMap(Config::getKey, Config::getValue));
    }

}
