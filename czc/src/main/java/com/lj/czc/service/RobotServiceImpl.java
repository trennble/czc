package com.lj.czc.service;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.taobao.api.ApiException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author: jiangbo
 * @create: 2020-12-22
 **/
@Service
public class RobotServiceImpl {

    public static final String serverUrl = "https://oapi.dingtalk.com/robot/send?access_token=bdbcb8673d95f1eb2d2287f7861d28655d2ecda5943202fe335a0b2b203f81ca";
    public static final String robot_v2 = "https://oapi.dingtalk.com/robot/send?access_token=69b742fa784536ef70687e4571122b5b9fbff682d7ba590282f2c917e2fa3a9a";

    public void send(String msg){
        DingTalkClient client = new DefaultDingTalkClient(robot_v2);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("text");
        OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
        text.setContent(msg);
        request.setText(text);
        try {
            client.execute(request);
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    public void sendRestartCard(String msg){
        DingTalkClient client = new DefaultDingTalkClient(robot_v2);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("actionCard");
        OapiRobotSendRequest.Actioncard actioncard = new OapiRobotSendRequest.Actioncard();
        OapiRobotSendRequest.Btns newBtn = new OapiRobotSendRequest.Btns();
        newBtn.setTitle("重启新品监控");
        newBtn.setActionURL("http://czc.trennble.xyz/api/sku/monitor-list");
        OapiRobotSendRequest.Btns btn = new OapiRobotSendRequest.Btns();
        btn.setTitle("重启价格监控");
        btn.setActionURL("http://czc.trennble.xyz/api/sku/monitor-price");
        actioncard.setBtnOrientation("0");
        actioncard.setText(msg);
        actioncard.setBtns(Arrays.asList(btn, newBtn));
        OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
        at.setIsAtAll(true);
        request.setAt(at);
        request.setActionCard(actioncard);
        try {
            client.execute(request);
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }
}
