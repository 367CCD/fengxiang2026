package com.txq.xiaohongshu.pojo;

import lombok.Data;

/**
 * 举报数据统计
 */
@Data
public class ReportStatsVO {
    /** 举报总数 */
    private Long totalCount;
    /** 待处理数 */
    private Long pendingCount;
    /** 已处理数 */
    private Long handledCount;
    /** 驳回数 */
    private Long rejectedCount;
    /** 平均处理时长(小时) */
    private Double avgHandleHours;
    /** 处理通过率 */
    private Double passRate;
}
