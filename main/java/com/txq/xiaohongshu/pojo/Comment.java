package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体
 */
@Data
public class Comment {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private Long parentId;
    private Long replyToUserId;
    private Long likeCount;
    private LocalDateTime createdAt;
}