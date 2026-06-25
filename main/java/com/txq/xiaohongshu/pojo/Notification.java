package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体
 */
@Data
public class Notification {
    private Long id;
    private Long userId;
    /** 1=点赞帖子 2=点赞评论 3=评论 4=回复 5=关注 6=收藏 7=系统 */
    private Integer type;
    private String content;
    private Integer isRead;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long targetId;
    private Integer targetType;
    private LocalDateTime createdAt;
    private String recipientName;    // 接收者用户名（管理端用）
}