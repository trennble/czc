package com.lj.czc.service;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.lj.czc.pojo.SkuInfo;
import com.taobao.api.ApiException;
import org.springframework.stereotype.Service;

import java.util.Collections;

import static com.lj.czc.service.MonitorServiceImpl.SKU_URL;

/**
 * @author: jiangbo
 * @create: 2020-12-22
 **/
@Service
public class RobotServiceImpl {

    public static final String serverUrl = "https://oapi.dingtalk.com/robot/send?access_token=bdbcb8673d95f1eb2d2287f7861d28655d2ecda5943202fe335a0b2b203f81ca";

    public void send(String msg){
        DingTalkClient client = new DefaultDingTalkClient(serverUrl);
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
        DingTalkClient client = new DefaultDingTalkClient(serverUrl);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("actionCard");
        OapiRobotSendRequest.Actioncard actioncard = new OapiRobotSendRequest.Actioncard();
        OapiRobotSendRequest.Btns btn = new OapiRobotSendRequest.Btns();
        btn.setTitle("重启");
        btn.setActionURL("http://czc.trennble.xyz/sku/monitor-list");
        actioncard.setBtnOrientation("0");
        actioncard.setText(msg);
        actioncard.setBtns(Collections.singletonList(btn));
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
