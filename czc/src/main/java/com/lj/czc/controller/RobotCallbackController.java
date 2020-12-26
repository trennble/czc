package com.lj.czc.controller;

import com.alibaba.fastjson.JSON;
import com.lj.czc.pojo.DingRequestBody;
import com.lj.czc.service.RobotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: jiangbo
 * @create: 2020-12-26
 **/
@RestController
@RequestMapping("callback")
public class RobotCallbackController {

    @Autowired
    private RobotServiceImpl robotService;

    @PostMapping("ding-robot")
    public void dingRobot(@RequestBody String dingRequestBody){
        System.out.println(JSON.toJSONString(dingRequestBody));
        robotService.send("已收到：\n"+dingRequestBody);
    }
}
