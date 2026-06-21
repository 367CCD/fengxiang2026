package com.txq.xiaohongshu.pojo;

import lombok.Data;

/**
 * 管理端帖子列表查询参数
 */
@Data
public class AdminPostQuery {
    private String keyword;
    private Long authorId;
    private Integer type;
    private Integer status;
    private Integer isTop;
    private Integer isEssence;
    private String startTime;
    private String endTime;
    private int page = 1;
    private int pageSize = 20;
}
