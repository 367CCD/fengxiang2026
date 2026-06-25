package com.txq.xiaohongshu.service.ImplService;

import com.txq.xiaohongshu.mapper.CommentMapper;
import com.txq.xiaohongshu.mapper.PostMapper;
import com.txq.xiaohongshu.mapper.ReportMapper;
import com.txq.xiaohongshu.mapper.UserMapper;
import com.txq.xiaohongshu.pojo.*;
import com.txq.xiaohongshu.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportImplService implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public Report submitReport(Report report, Long userId) {
        // 校验目标类型
        if (report.getTargetType() == null || (report.getTargetType() < 1 || report.getTargetType() > 3)) {
            throw new RuntimeException("无效的举报目标类型");
        }
        // 校验举报原因
        if (report.getReason() == null || report.getReason().trim().isEmpty()) {
            throw new RuntimeException("举报原因不能为空");
        }
        if (report.getReason().length() > 500) {
            throw new RuntimeException("举报原因不能超过500个字符");
        }
        report.setUserId(userId);
        reportMapper.insert(report);
        return report;
    }

    @Override
    public PageResult<Report> getMyReports(Long userId, int pageNum, int pageSize, Integer status) {
        int offset = (pageNum - 1) * pageSize;
        List<Report> list = reportMapper.selectByUserId(userId, status, offset, pageSize);
        long total = reportMapper.countByUserId(userId, status);
        return PageResult.of(list, total, pageNum, pageSize);
    }

    @Override
    public PageResult<Report> getReportList(int pageNum, int pageSize, Integer status, Integer targetType) {
        int offset = (pageNum - 1) * pageSize;
        List<Report> list = reportMapper.selectList(status, targetType, offset, pageSize);
        long total = reportMapper.countList(status, targetType);
        return PageResult.of(list, total, pageNum, pageSize);
    }

    @Override
    public Report getReportDetail(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new RuntimeException("举报记录不存在");
        }
        return report;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long reportId, Integer status, String handleRemark, Long handlerId) {
        // 校验处理结果
        if (status == null || (status != 1 && status != 2)) {
            throw new RuntimeException("无效的处理结果，必须为1（已处理）或2（已驳回）");
        }
        if (handleRemark != null && handleRemark.length() > 500) {
            throw new RuntimeException("处理备注不能超过500个字符");
        }

        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("举报记录不存在");
        }
        if (report.getStatus() != 0) {
            throw new RuntimeException("该举报已处理，无法重复操作");
        }

        // 举报成立 → 执行对应处罚
        if (status == 1) {
            executePenalty(report.getTargetType(), report.getTargetId());
        }

        // 更新举报记录
        reportMapper.handleReport(reportId, status, handleRemark, handlerId);
    }

    /**
     * 执行举报处罚
     * targetType=1 → 下架帖子(status=3)
     * targetType=2 → 隐藏评论(status=0)
     * targetType=3 → 禁用用户(status=0)
     */
    private void executePenalty(Integer targetType, Long targetId) {
        switch (targetType) {
            case 1:
                // 下架帖子
                postMapper.updateStatus(targetId, 3);
                break;
            case 2:
                // 隐藏评论（软删除）
                commentMapper.softDeleteById(targetId);
                break;
            case 3:
                // 禁用用户
                userMapper.updateStatus(targetId, 0);
                break;
        }
    }
}
