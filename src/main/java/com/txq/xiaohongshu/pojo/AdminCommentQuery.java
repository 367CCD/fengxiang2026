package com.txq.xiaohongshu.pojo;

import lombok.Data;

/**
 * 管理端评论列表查询参数
 */
@Data
public class AdminCommentQuery {
    private String keyword;
    private Long userId;
    private Long postId;
    private Integer status;
    private String startTime;
    private String endTime;
    private int page = 1;
    private int pageSize = 20;
}
