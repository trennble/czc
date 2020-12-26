package com.lj.czc.pojo;

import lombok.Data;

import java.util.List;

/**
 * @author: jiangbo
 * @create: 2020-12-26
 **/
@Data
public class DingRequestBody {
    private String msgtype;
    private String content;
    private String msgId;
    private String createAt;
    private String conversationType;
    private String conversationId;
    private String conversationTitle;
    private String senderId;
    private String senderNick;
    private String senderCorpId;
    private String senderStaffId;
    private String chatbotUserId;
    private List<String> atUsers;
}
