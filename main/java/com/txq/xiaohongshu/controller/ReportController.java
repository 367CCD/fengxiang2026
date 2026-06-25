package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.Report;
import com.txq.xiaohongshu.pojo.Result;
import com.txq.xiaohongshu.service.ReportService;
import com.txq.xiaohongshu.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 1.1 提交举报
     */
    @PostMapping("/reports")
    public Result submitReport(@RequestBody Report report, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            Report saved = reportService.submitReport(report, userId);
            log.info("用户 {} 提交了举报 reportId={}, targetType={}, targetId={}",
                    userId, saved.getId(), report.getTargetType(), report.getTargetId());

            Map<String, Object> data = new HashMap<>();
            data.put("id", saved.getId());
            return Result.sucess(data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 1.2 我的举报列表
     */
    @GetMapping("/reports/my")
    public Result myReports(@RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "10") int pageSize,
                            @RequestParam(required = false) Integer status,
                            HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        PageResult<Report> result = reportService.getMyReports(userId, pageNum, pageSize, status);
        return Result.sucess(result);
    }
}
