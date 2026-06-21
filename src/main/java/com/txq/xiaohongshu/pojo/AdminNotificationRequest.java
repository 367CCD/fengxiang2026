package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.util.List;

/**
 * 管理端发送通知请求
 */
@Data
public class AdminNotificationRequest {
    /** 定向发送的目标用户ID列表（broadcast=true时忽略） */
    private List<Long> recipientIds;
    /** 是否全站广播 */
    private Boolean broadcast;
    /** 通知类型（通常为7=系统通知） */
    private Integer type;
    /** 通知内容 */
    private String content;
}
