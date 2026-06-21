package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论列表视图对象 — 用于一级评论 & 二级回复展示
 */
@Data
public class CommentVO {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private Long parentId;          // 一级评论ID（二级回复时使用）
    private Long replyToUserId;     // 被回复用户ID
    private Integer status;          // 0删除 1正常
    private Long likeCount;
    private Integer replyCount;     // 二级回复总数（仅一级评论填充）
    private LocalDateTime createdAt;
    private String postTitle;       // 所属帖子标题（管理端用）

    // ── 发布者信息 ──
    private String username;
    private String avatarUrl;

    // ── 被回复用户信息（仅二级回复填充，用于前端 @用户名） ──
    private String replyToUsername;
}