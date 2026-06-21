package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搜索历史
 */
@Data
public class SearchHistory {
    private Long id;
    private Long userId;
    private String keyword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String username;         // 用户名（管理端联表查询用）
}