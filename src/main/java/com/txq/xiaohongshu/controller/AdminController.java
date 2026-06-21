package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.pojo.*;
import com.txq.xiaohongshu.service.AdminService;
import com.txq.xiaohongshu.service.UserService;
import com.txq.xiaohongshu.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    // ══════════════════════════════════════════════
    // 权限校验
    // ══════════════════════════════════════════════

    private Long requireAdmin(HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return null;
        }
        if (!userService.isAdmin(userId)) {
            return null;
        }
        return userId;
    }

    // ══════════════════════════════════════════════
    // 用户管理
    // ══════════════════════════════════════════════

    /** 用户账号列表 */
    @GetMapping("/users")
    public Result userList(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Integer status,
                           @RequestParam(required = false) Integer isMuted,
                           @RequestParam(required = false) Integer role,
                           @RequestParam(required = false) String startTime,
                           @RequestParam(required = false) String endTime,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int pageSize,
                           HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        PageResult<User> result = adminService.getUserList(keyword, status, isMuted, role,
                startTime, endTime, page, pageSize);
        return Result.sucess(result);
    }

    /** 用户详情 */
    @GetMapping("/users/{id}")
    public Result userDetail(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        User user = adminService.getUserDetail(id);
        if (user == null) return Result.error("用户不存在");
        return Result.sucess(user);
    }

    /** 批量禁用/启用账号 */
    @PutMapping("/users/batch/status")
    public Result batchUpdateUserStatus(@RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        Integer status = (Integer) body.get("status");
        if (ids == null || ids.isEmpty() || status == null) {
            return Result.error("参数错误");
        }
        adminService.batchUpdateUserStatus(ids, status);
        log.info("管理员 {} 批量更新用户状态: ids={}, status={}", adminId, ids, status);
        return Result.sucess();
    }

    /** 禁言用户 */
    @PostMapping("/users/mute")
    public Result muteUsers(@RequestBody MuteRequest muteReq, HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        if (muteReq.getUserIds() == null || muteReq.getUserIds().isEmpty()) {
            return Result.error("请选择要禁言的用户");
        }
        adminService.muteUsers(adminId, muteReq);
        return Result.sucess();
    }

    /** 手动解除禁言 */
    @PostMapping("/users/unmute")
    public Result unmuteUsers(@RequestBody Map<String, List<Long>> body,
                               HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要解除禁言的用户");
        }
        adminService.unmuteUsers(adminId, ids);
        return Result.sucess();
    }

    /** 禁言已到期用户列表（懒解封检查） */
    @GetMapping("/users/muted-expired")
    public Result mutedExpired(HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        List<User> users = adminService.getMutedExpiredUsers();
        users.forEach(u -> u.setPasswordHash(null));
        return Result.sucess(users);
    }

    /** 用户禁言历史 */
    @GetMapping("/users/{id}/mute-history")
    public Result muteHistory(@PathVariable Long id,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int pageSize,
                               HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        PageResult<UserMuteHistory> result = adminService.getUserMuteHistory(id, page, pageSize);
        return Result.sucess(result);
    }

    /** 管理员重置密码 */
    @PutMapping("/users/{id}/reset-password")
    public Result resetPassword(@PathVariable Long id,
                                 @RequestBody Map<String, String> body,
                                 HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("密码至少6位");
        }
        adminService.resetPassword(adminId, id, newPassword);
        return Result.sucess();
    }

    /** 管理员清除用户头像 */
    @PutMapping("/users/{id}/reset-avatar")
    public Result resetAvatar(@PathVariable Long id,
                               HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        adminService.resetAvatar(adminId, id);
        log.info("管理员 {} 清除了用户 {} 的头像", adminId, id);
        return Result.sucess();
    }

    // ══════════════════════════════════════════════
    // 内容管理
    // ══════════════════════════════════════════════

    /** 帖子管理列表 */
    @GetMapping("/posts")
    public Result postList(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long authorId,
                           @RequestParam(required = false) Integer type,
                           @RequestParam(required = false) Integer status,
                           @RequestParam(required = false) Integer isTop,
                           @RequestParam(required = false) Integer isEssence,
                           @RequestParam(required = false) String startTime,
                           @RequestParam(required = false) String endTime,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int pageSize,
                           HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        AdminPostQuery query = new AdminPostQuery();
        query.setKeyword(keyword);
        query.setAuthorId(authorId);
        query.setType(type);
        query.setStatus(status);
        query.setIsTop(isTop);
        query.setIsEssence(isEssence);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPage(page);
        query.setPageSize(pageSize);
        PageResult<PostVO> result = adminService.getPostList(query);
        return Result.sucess(result);
    }

    /** 帖子详情（管理端，不限状态） */
    @GetMapping("/posts/{id}")
    public Result postDetail(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        PostDetailVO detail = adminService.getPostDetail(id);
        if (detail == null) return Result.error("帖子不存在");
        return Result.sucess(detail);
    }

    /** 更新帖子状态（违规下架/恢复） */
    @PutMapping("/posts/{id}/status")
    public Result updatePostStatus(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body,
                                    HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        Integer status = (Integer) body.get("status");
        String reason = (String) body.get("reason");
        if (status == null) return Result.error("请指定状态");
        adminService.updatePostStatus(adminId, id, status, reason);
        return Result.sucess();
    }

    /** 批量下架/恢复 */
    @PutMapping("/posts/batch/status")
    public Result batchUpdatePostStatus(@RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        Integer status = (Integer) body.get("status");
        String reason = (String) body.get("reason");
        if (ids == null || ids.isEmpty() || status == null) return Result.error("参数错误");
        adminService.batchUpdatePostStatus(adminId, ids, status, reason);
        return Result.sucess();
    }

    /** 批量置顶/取消置顶 */
    @PutMapping("/posts/batch/top")
    public Result batchSetTop(@RequestBody Map<String, Object> body,
                               HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        Integer isTop = (Integer) body.get("isTop");
        if (ids == null || ids.isEmpty() || isTop == null) return Result.error("参数错误");
        adminService.batchSetTop(ids, isTop);
        return Result.sucess();
    }

    /** 批量加精/取消精华 */
    @PutMapping("/posts/batch/essence")
    public Result batchSetEssence(@RequestBody Map<String, Object> body,
                                   HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        Integer isEssence = (Integer) body.get("isEssence");
        if (ids == null || ids.isEmpty() || isEssence == null) return Result.error("参数错误");
        adminService.batchSetEssence(ids, isEssence);
        return Result.sucess();
    }

    /** 物理删除帖子 */
    @DeleteMapping("/posts/{id}")
    public Result deletePost(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        adminService.deletePost(id);
        return Result.sucess();
    }

    // ══════════════════════════════════════════════
    // 评论管理
    // ══════════════════════════════════════════════

    /** 评论列表 */
    @GetMapping("/comments")
    public Result commentList(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Long userId,
                              @RequestParam(required = false) Long postId,
                              @RequestParam(required = false) Integer status,
                              @RequestParam(required = false) String startTime,
                              @RequestParam(required = false) String endTime,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int pageSize,
                              HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        AdminCommentQuery query = new AdminCommentQuery();
        query.setKeyword(keyword);
        query.setUserId(userId);
        query.setPostId(postId);
        query.setStatus(status);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPage(page);
        query.setPageSize(pageSize);
        PageResult<CommentVO> result = adminService.getCommentList(query);
        return Result.sucess(result);
    }

    /** 评论详情 */
    @GetMapping("/comments/{id}")
    public Result commentDetail(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        CommentVO comment = adminService.getCommentDetail(id);
        if (comment == null) return Result.error("评论不存在");
        return Result.sucess(comment);
    }

    /** 删除评论 */
    @DeleteMapping("/comments/{id}")
    public Result deleteComment(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        adminService.deleteComment(adminId, id);
        return Result.sucess();
    }

    /** 恢复评论 */
    @PutMapping("/comments/{id}/restore")
    public Result restoreComment(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        adminService.restoreComment(id);
        return Result.sucess();
    }

    /** 批量删除评论 */
    @PutMapping("/comments/batch/delete")
    public Result batchDeleteComments(@RequestBody Map<String, List<Long>> body,
                                       HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) return Result.error("参数错误");
        adminService.batchDeleteComments(ids);
        return Result.sucess();
    }

    /** 批量恢复评论 */
    @PutMapping("/comments/batch/restore")
    public Result batchRestoreComments(@RequestBody Map<String, List<Long>> body,
                                        HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) return Result.error("参数错误");
        adminService.batchRestoreComments(ids);
        return Result.sucess();
    }

    // ══════════════════════════════════════════════
    // 举报处理
    // ══════════════════════════════════════════════

    /** 举报工单列表 */
    @GetMapping("/reports")
    public Result reportList(@RequestParam(required = false) Integer status,
                             @RequestParam(required = false) Integer targetType,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int pageSize,
                             HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        PageResult<AdminReportDetailVO> result = adminService.getReportList(status, targetType, page, pageSize);
        return Result.sucess(result);
    }

    /** 举报详情 */
    @GetMapping("/reports/{id}")
    public Result reportDetail(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        AdminReportDetailVO detail = adminService.getReportDetail(id);
        if (detail == null) return Result.error("举报不存在");
        return Result.sucess(detail);
    }

    /** 处理举报（联动处罚） */
    @PutMapping("/reports/{id}/handle")
    public Result handleReport(@PathVariable Long id,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        Integer status = (Integer) body.get("status");
        String remark = (String) body.get("handleRemark");
        Integer penaltyAction = (Integer) body.get("penaltyAction");
        Integer penaltyDuration = (Integer) body.get("penaltyDuration");
        String penaltyReason = (String) body.get("penaltyReason");
        if (status == null) return Result.error("请指定处理结果");
        adminService.handleReport(adminId, id, status, remark,
                penaltyAction, penaltyDuration, penaltyReason);
        return Result.sucess();
    }

    /** 举报数据统计 */
    @GetMapping("/reports/stats")
    public Result reportStats(HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        ReportStatsVO stats = adminService.getReportStats();
        return Result.sucess(stats);
    }

    // ══════════════════════════════════════════════
    // 标签管理
    // ══════════════════════════════════════════════

    /** 标签列表 */
    @GetMapping("/tags")
    public Result tagList(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "postCount") String sortBy,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int pageSize,
                          HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        PageResult<Tag> result = adminService.getTagList(keyword, status, sortBy, page, pageSize);
        return Result.sucess(result);
    }

    /** 新增标签 */
    @PostMapping("/tags")
    public Result createTag(@RequestBody Tag tag, HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        if (tag.getName() == null || tag.getName().isEmpty()) {
            return Result.error("标签名不能为空");
        }
        Tag created = adminService.createTag(tag);
        return Result.sucess(created);
    }

    /** 编辑标签 */
    @PutMapping("/tags/{id}")
    public Result updateTag(@PathVariable Long id, @RequestBody Tag tag,
                             HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        tag.setId(id);
        Tag updated = adminService.updateTag(tag);
        return Result.sucess(updated);
    }

    /** 启用/禁用标签 */
    @PutMapping("/tags/{id}/status")
    public Result toggleTagStatus(@PathVariable Long id,
                                   @RequestBody Map<String, Integer> body,
                                   HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        Integer status = body.get("status");
        if (status == null) return Result.error("请指定状态");
        adminService.toggleTagStatus(id, status);
        return Result.sucess();
    }

    // ══════════════════════════════════════════════
    // 通知管理
    // ══════════════════════════════════════════════

    /** 发送系统通知 */
    @PostMapping("/notifications")
    public Result sendNotification(@RequestBody AdminNotificationRequest notifReq,
                                    HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        if (notifReq.getContent() == null || notifReq.getContent().isEmpty()) {
            return Result.error("通知内容不能为空");
        }
        adminService.sendNotification(adminId, notifReq);
        return Result.sucess();
    }

    /** 通知记录查询 */
    @GetMapping("/notifications")
    public Result notificationRecords(@RequestParam(required = false) Integer type,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) String startTime,
                                       @RequestParam(required = false) String endTime,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int pageSize,
                                       HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        PageResult<Notification> result = adminService.getNotificationRecords(
                type, userId, startTime, endTime, page, pageSize);
        return Result.sucess(result);
    }

    // ══════════════════════════════════════════════
    // 搜索运营管理
    // ══════════════════════════════════════════════

    @GetMapping("/search/hot-keywords")
    public Result hotKeywords(@RequestParam(defaultValue = "7d") String range,
                               HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        return Result.sucess(adminService.getHotKeywords(range));
    }

    @GetMapping("/search/trend")
    public Result searchTrend(@RequestParam(defaultValue = "7d") String range,
                               HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        return Result.sucess(adminService.getSearchTrend(range));
    }

    @GetMapping("/search/history")
    public Result searchHistory(@RequestParam(required = false) Long userId,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int pageSize,
                                 HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        PageResult<com.txq.xiaohongshu.pojo.SearchHistory> result =
                adminService.getSearchHistoryList(userId, keyword, page, pageSize);
        return Result.sucess(result);
    }

    @DeleteMapping("/search/keywords")
    public Result cleanKeywords(@RequestBody Map<String, String> body,
                                 HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        adminService.cleanSearchKeywords(body.get("keyword"));
        return Result.sucess();
    }

    // ══════════════════════════════════════════════
    // 数据统计看板
    // ══════════════════════════════════════════════

    @GetMapping("/dashboard/overview")
    public Result dashboardOverview(HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        return Result.sucess(adminService.getDashboardOverview());
    }

    @GetMapping("/dashboard/trends")
    public Result dashboardTrends(@RequestParam(defaultValue = "7d") String range,
                                   HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        return Result.sucess(adminService.getDashboardTrends(range));
    }

    @GetMapping("/dashboard/rankings")
    public Result dashboardRankings(@RequestParam(defaultValue = "hotPosts") String type,
                                     HttpServletRequest request) {
        Long adminId = requireAdmin(request);
        if (adminId == null) return Result.error("无权访问");
        return Result.sucess(adminService.getDashboardRankings(type));
    }
}
