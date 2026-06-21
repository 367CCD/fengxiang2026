package com.txq.xiaohongshu.pojo;

import lombok.Data;

/**
 * 用户视图对象 — 用于关注/粉丝列表展示
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;
    /** 该用户的关注数 */
    private Long followCount;
    /** 该用户的粉丝数 */
    private Long fansCount;
    /** 当前登录用户是否已关注该用户（未登录返回 false） */
    private Boolean isFollowed;
    /** 是否互相关注（未登录返回 false） */
    private Boolean isMutual;
}