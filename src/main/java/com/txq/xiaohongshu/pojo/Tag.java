package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Tag {
    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private Long postCount;
    private Long followCount;
    private Integer status;
    private LocalDateTime createdAt;
}