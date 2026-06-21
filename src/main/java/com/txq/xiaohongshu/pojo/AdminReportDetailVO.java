package com.txq.xiaohongshu.pojo;

import lombok.Data;

/**
 * 管理端举报详情视图 — 含举报人、处理人、被举报对象摘要
 */
@Data
public class AdminReportDetailVO {
    private Long id;
    private Integer targetType;
    private Long targetId;
    private String reason;
    private Integer status;
    private String handleRemark;
    private String createdAt;
    private String handledAt;

    // ── 举报人信息 ──
    private Long reporterId;
    private String reporterName;

    // ── 处理人信息 ──
    private Long handlerId;
    private String handlerName;

    // ── 被举报对象摘要 ──
    private String targetSummary;
}
