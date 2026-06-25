package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.pojo.Notification;
import com.txq.xiaohongshu.pojo.NotificationCreateRequest;
import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.Result;
import com.txq.xiaohongshu.service.NotificationService;
import com.txq.xiaohongshu.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 1. 分页查询通知列表（支持按类型和已读状态筛选）
     */
    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int pageSize,
                       @RequestParam(required = false) Integer type,
                       @RequestParam(required = false) Integer isRead,
                       HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            PageResult<Notification> result = notificationService.getNotifications(userId, type, isRead, page, pageSize);
            Result r = Result.sucess(result);
            r.setMsg("success");
            return r;
        } catch (RuntimeException e) {
            log.warn("查询通知列表失败: userId={}, {}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 5. 创建通知（由其他业务操作触发，如点赞、评论、关注、收藏）
     */
    @PostMapping
    public Result create(@RequestBody NotificationCreateRequest req,
                         HttpServletRequest request) {
        String token = request.getHeader("token");
        Long senderId = jwtUtils.getUserId(token);
        if (senderId == null) {
            return Result.error("请先登录");
        }
        // 不给自己发通知
        if (senderId.equals(req.getRecipientId())) {
            return Result.sucess();
        }
        try {
            notificationService.createNotification(senderId, req);
            log.info("创建通知: sender={}, recipient={}, type={}", senderId, req.getRecipientId(), req.getType());
            Result r = Result.sucess();
            r.setMsg("success");
            return r;
        } catch (RuntimeException e) {
            log.warn("创建通知失败: sender={}, {}", senderId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 2. 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result unreadCount(HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            Map<String, Integer> result = notificationService.getUnreadCount(userId);
            Result r = Result.sucess(result);
            r.setMsg("success");
            return r;
        } catch (RuntimeException e) {
            log.warn("获取未读数量失败: userId={}, {}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 3. 标记单条通知为已读（仅可操作本人的通知）
     */
    @PutMapping("/{id}/read")
    public Result markRead(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            notificationService.markRead(id, userId);
            log.info("标记通知已读: id={}, userId={}", id, userId);
            Result r = Result.sucess();
            r.setMsg("操作成功");
            return r;
        } catch (RuntimeException e) {
            log.warn("标记已读失败: id={}, userId={}, {}", id, userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 4. 标记全部通知为已读
     */
    @PutMapping("/read-all")
    public Result markAllRead(HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            notificationService.markAllRead(userId);
            log.info("全部通知标记已读: userId={}", userId);
            Result r = Result.sucess();
            r.setMsg("全部标记为已读");
            return r;
        } catch (RuntimeException e) {
            log.warn("全部标记已读失败: userId={}, {}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
