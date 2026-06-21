package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户资料视图对象 — 用于 GET /users/profile 返回
 * 包含用户基本信息 + 帖子数/粉丝数/关注数（单次查询完成，减少数据库连接）
 */
@Data
public class UserProfileVO {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String avatarUrl;
    private String bio;
    private Integer gender;
    private LocalDate birthday;
    private String location;
    private Integer status;
    private Integer role;
    private Integer isMuted;
    private String mutedReason;
    private LocalDateTime mutedAt;
    private LocalDateTime muteEndTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── 统计字段（通过子查询一次性获取） ──
    private Long postCount;       // 帖子总数
    private Long followerCount;   // 粉丝数
    private Long followingCount;  // 关注数
    private Boolean isFollowed;   // 当前登录用户是否已关注该用户
}