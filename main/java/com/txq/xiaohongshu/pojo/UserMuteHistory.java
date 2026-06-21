package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户禁言历史记录
 */
@Data
public class UserMuteHistory {
    private Long id;
    private Long userId;
    private Long operatorId;
    /** 1=禁言 2=解封 */
    private Integer action;
    private String reason;
    /** 禁言时长描述: 1天/3天/7天/30天/永久 */
    private String muteDuration;
    private LocalDateTime muteEndTime;
    /** 解封方式: 1=手动解除 2=系统自动懒解封 */
    private Integer unmuteType;
    private LocalDateTime createdAt;

    // ── 联表查询字段 ──
    private String operatorName;
}
