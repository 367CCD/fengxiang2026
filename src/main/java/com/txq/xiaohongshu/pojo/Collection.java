package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏夹实体
 */
@Data
public class Collection {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String coverUrl;
    private Integer isPublic;
    private Integer postCount;
    private LocalDateTime createdAt;
}