package com.txq.xiaohongshu.pojo;

import lombok.Data;

/**
 * 创建通知的请求体
 */
@Data
public class NotificationCreateRequest {
    /** 通知类型：1=点赞帖子 2=点赞评论 3=评论 4=回复 5=关注 6=收藏 */
    private Integer type;
    /** 接收通知的用户 ID */
    private Long recipientId;
    /** 被操作的目标 ID（帖子ID / 评论ID / 用户ID） */
    private Long targetId;
    /** 目标类型：1=帖子 2=评论 0=用户（关注时） */
    private Integer targetType;
    /** 通知内容（系统通知/管理端使用） */
    private String content;
}
