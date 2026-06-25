package com.txq.xiaohongshu.service;

import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.Report;

public interface ReportService {

    /** 提交举报 */
    Report submitReport(Report report, Long userId);

    /** 我的举报列表 */
    PageResult<Report> getMyReports(Long userId, int pageNum, int pageSize, Integer status);

    /** 管理端：举报列表 */
    PageResult<Report> getReportList(int pageNum, int pageSize, Integer status, Integer targetType);

    /** 管理端：举报详情 */
    Report getReportDetail(Long id);

    /** 管理端：处理举报 */
    void handleReport(Long reportId, Integer status, String handleRemark, Long handlerId);
}
