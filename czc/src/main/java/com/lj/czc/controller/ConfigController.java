package com.lj.czc.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lj.czc.pojo.DingRequestBody;
import com.lj.czc.pojo.bean.Config;
import com.lj.czc.service.ConfigServiceImpl;
import com.lj.czc.service.RobotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: jiangbo
 * @create: 2020-12-28
 **/
@RestController
@RequestMapping("config")
public class ConfigController {

    @Autowired
    private ConfigServiceImpl configService;

    @Autowired
    private RobotServiceImpl robotService;

}
