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

    public void send(String msg){
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/robot/send?access_token=566cc69da782ec******");
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

    public void sendFail(){
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/robot/send?access_token=566cc69da782ec******");
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("text");
        OapiRobotSendRequest.Actioncard actioncard = new OapiRobotSendRequest.Actioncard();
        OapiRobotSendRequest.Btns btn = new OapiRobotSendRequest.Btns();
        btn.setTitle("重启");
        btn.setActionURL("http://www.baidu.com");
        actioncard.setBtnOrientation("0");
        actioncard.setText("监控终止，请尝试重启");
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
