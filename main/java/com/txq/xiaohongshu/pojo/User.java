package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String passwordHash;
    private String avatarUrl;
    private String bio;
    private Integer gender;
    private LocalDate birthday;
    private String location;
    private Integer status;
    private Integer role;
    private Integer isMuted;         // 0=正常 1=禁言中
    private String mutedReason;      // 禁言原因
    private LocalDateTime mutedAt;   // 禁言开始时间
    private LocalDateTime muteEndTime; // 禁言到期时间；NULL=永久禁言
    private String captcha;          // 验证码（仅用于登录请求，非数据库字段）
    private Integer loginType;       // 登录类型：0=用户端 1=管理端（仅用于登录请求，非数据库字段）
    private String adminCode;        // 管理端注册码（仅用于注册请求，非数据库字段）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}