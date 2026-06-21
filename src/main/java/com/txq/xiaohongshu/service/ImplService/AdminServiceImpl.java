package com.txq.xiaohongshu.service.ImplService;

import cn.hutool.crypto.digest.BCrypt;
import com.txq.xiaohongshu.mapper.*;
import com.txq.xiaohongshu.pojo.*;
import com.txq.xiaohongshu.service.AdminService;
import com.txq.xiaohongshu.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private com.txq.xiaohongshu.mapper.CollectionMapper collectionMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMuteHistoryMapper muteHistoryMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.txq.xiaohongshu.mapper.SearchHistoryMapper searchHistoryMapper;

    @Autowired
    private com.txq.xiaohongshu.mapper.DashboardMapper dashboardMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ══════════════════════════════════════════════
    // 用户管理
    // ══════════════════════════════════════════════

    @Override
    public PageResult<User> getUserList(String keyword, Integer status, Integer isMuted,
                                         Integer role, String startTime, String endTime,
                                         int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<User> list = userMapper.selectAdminList(keyword, status, isMuted, role,
                startTime, endTime, offset, pageSize);
        // 清除敏感字段
        list.forEach(u -> u.setPasswordHash(null));
        long total = userMapper.countAdminList(keyword, status, isMuted, role, startTime, endTime);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public User getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPasswordHash(null);
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateUserStatus(List<Long> ids, Integer status) {
        userMapper.batchUpdateStatus(ids, status);
        log.info("批量更新用户状态: ids={}, status={}", ids, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void muteUsers(Long operatorId, MuteRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String mutedAt = now.format(DT_FMT);
        String muteEndTimeStr = null;
        String durationDesc;

        if (request.getDuration() == null) {
            // 永久禁言
            durationDesc = "永久";
        } else {
            int days = request.getDuration();
            durationDesc = days + "天";
            muteEndTimeStr = now.plusDays(days).format(DT_FMT);
        }

        // 批量设置禁言
        userMapper.batchUpdateMute(request.getUserIds(), 1, request.getReason(),
                mutedAt, muteEndTimeStr);

        // 写入禁言历史
        for (Long userId : request.getUserIds()) {
            UserMuteHistory history = new UserMuteHistory();
            history.setUserId(userId);
            history.setOperatorId(operatorId);
            history.setAction(1); // 禁言
            history.setReason(request.getReason());
            history.setMuteDuration(durationDesc);
            if (muteEndTimeStr != null) {
                history.setMuteEndTime(LocalDateTime.parse(muteEndTimeStr, DT_FMT));
            }
            muteHistoryMapper.insert(history);

            // 发送禁言通知
            try {
                NotificationCreateRequest notifReq = new NotificationCreateRequest();
                notifReq.setRecipientId(userId);
                notifReq.setType(7); // 系统通知
                notifReq.setContent(String.format("您因「%s」被禁言%s，到期时间：%s",
                        request.getReason(),
                        durationDesc,
                        muteEndTimeStr != null ? muteEndTimeStr : "永久"));
                notificationService.createNotification(operatorId, notifReq);
            } catch (Exception e) {
                log.warn("禁言通知发送失败: userId={}, {}", userId, e.getMessage());
            }
        }

        log.info("管理员 {} 禁言用户: ids={}, duration={}, reason={}",
                operatorId, request.getUserIds(), durationDesc, request.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unmuteUsers(Long operatorId, List<Long> ids) {
        userMapper.batchClearMute(ids);

        for (Long userId : ids) {
            UserMuteHistory history = new UserMuteHistory();
            history.setUserId(userId);
            history.setOperatorId(operatorId);
            history.setAction(2); // 解封
            history.setReason("手动解除禁言");
            history.setUnmuteType(1); // 手动解除
            muteHistoryMapper.insert(history);

            // 发送解封通知
            try {
                NotificationCreateRequest notifReq = new NotificationCreateRequest();
                notifReq.setRecipientId(userId);
                notifReq.setType(7);
                notifReq.setContent("您的禁言已被管理员解除，现在可以正常发言了。");
                notificationService.createNotification(operatorId, notifReq);
            } catch (Exception e) {
                log.warn("解封通知发送失败: userId={}, {}", userId, e.getMessage());
            }
        }

        log.info("管理员 {} 手动解除禁言: ids={}", operatorId, ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long operatorId, Long userId, String newPassword) {
        String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userMapper.updatePassword(userId, hash);
        log.info("管理员 {} 重置了用户 {} 的密码", operatorId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetAvatar(Long operatorId, Long userId) {
        userMapper.resetAvatar(userId);
        log.info("管理员 {} 清除了用户 {} 的头像", operatorId, userId);
    }

    @Override
    public PageResult<UserMuteHistory> getUserMuteHistory(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<UserMuteHistory> list = muteHistoryMapper.selectByUserId(userId, offset, pageSize);
        long total = muteHistoryMapper.countByUserId(userId);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public List<User> getMutedExpiredUsers() {
        return userMapper.selectMutedExpired();
    }

    // ══════════════════════════════════════════════
    // 内容管理
    // ══════════════════════════════════════════════

    @Override
    public PageResult<PostVO> getPostList(AdminPostQuery query) {
        int offset = (query.getPage() - 1) * query.getPageSize();
        List<PostVO> list = postMapper.selectAdminList(
                query.getKeyword(), query.getAuthorId(), query.getType(),
                query.getStatus(), query.getIsTop(), query.getIsEssence(),
                query.getStartTime(), query.getEndTime(),
                offset, query.getPageSize());
        long total = postMapper.countAdminList(
                query.getKeyword(), query.getAuthorId(), query.getType(),
                query.getStatus(), query.getIsTop(), query.getIsEssence(),
                query.getStartTime(), query.getEndTime());
        return PageResult.of(list, total, query.getPage(), query.getPageSize());
    }

    @Override
    public PostDetailVO getPostDetail(Long postId) {
        PostDetailVO detail = postMapper.selectAdminDetailById(postId);
        if (detail != null) {
            detail.setImages(postMapper.selectImagesByPostId(postId));
            detail.setTags(postMapper.selectTagsByPostId(postId));
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePostStatus(Long operatorId, Long postId, Integer status, String reason) {
        // 查询当前状态
        PostDetailVO detail = postMapper.selectAdminDetailById(postId);
        Integer fromStatus = detail != null ? detail.getStatus() : null;

        postMapper.updateStatus(postId, status);

        // 记录状态变更日志
        postMapper.insertStatusLog(postId, operatorId, fromStatus, status, reason);

        // 下架时：清理该帖子的所有收藏 + 通知收藏者
        if (status != null && status == 3) {
            List<java.util.Map<String, Object>> collectors = postMapper.selectCollectorsByPostId(postId);
            if (collectors != null && !collectors.isEmpty()) {
                for (java.util.Map<String, Object> entry : collectors) {
                    Long collectionId = ((Number) entry.get("collectionId")).longValue();
                    Long collectorId = ((Number) entry.get("userId")).longValue();
                    // 从收藏夹中移除
                    postMapper.deleteFromCollection(collectionId, postId);
                    // 收藏夹帖子数 -1
                    collectionMapper.decrementPostCount(collectionId);
                    // 帖子收藏数 -1
                    postMapper.decrementCollectCount(postId);
                    // 通知收藏者
                    try {
                        NotificationCreateRequest notifReq = new NotificationCreateRequest();
                        notifReq.setRecipientId(collectorId);
                        notifReq.setType(7);
                        notifReq.setTargetType(1);
                        notifReq.setTargetId(postId);
                        notifReq.setContent(String.format("您收藏的帖子已被下架，原因：%s",
                                reason != null ? reason : "违规"));
                        notificationService.createNotification(operatorId, notifReq);
                    } catch (Exception e) {
                        log.warn("收藏者通知发送失败: userId={}, {}", collectorId, e.getMessage());
                    }
                }
            }
        }

        // 通知作者
        Long authorId = postMapper.selectUserIdByPostId(postId);
        if (authorId != null) {
            String statusName = status == 3 ? "下架" : status == 1 ? "恢复" : "状态变更";
            try {
                NotificationCreateRequest notifReq = new NotificationCreateRequest();
                notifReq.setRecipientId(authorId);
                notifReq.setType(7);
                notifReq.setTargetType(1);      // 目标类型: 帖子
                notifReq.setTargetId(postId);   // 目标ID: 帖子ID
                notifReq.setContent(String.format("您的帖子已被%s，原因：%s", statusName,
                        reason != null ? reason : "无"));
                notificationService.createNotification(operatorId, notifReq);
            } catch (Exception e) {
                log.warn("帖子状态通知发送失败: postId={}, {}", postId, e.getMessage());
            }
        }

        log.info("管理员 {} 更新帖子 {} 状态: {} → {}, reason={}",
                operatorId, postId, fromStatus, status, reason);
    }

    @Override
    public void batchUpdatePostStatus(Long operatorId, List<Long> ids, Integer status, String reason) {
        for (Long postId : ids) {
            updatePostStatus(operatorId, postId, status, reason);
        }
        log.info("批量更新帖子状态: ids={}, status={}", ids, status);
    }

    @Override
    public void batchSetTop(List<Long> ids, Integer isTop) {
        postMapper.batchSetTop(ids, isTop);
        log.info("批量设置置顶: ids={}, isTop={}", ids, isTop);
    }

    @Override
    public void batchSetEssence(List<Long> ids, Integer isEssence) {
        postMapper.batchSetEssence(ids, isEssence);
        log.info("批量设置精华: ids={}, isEssence={}", ids, isEssence);
    }

    @Override
    public void deletePost(Long postId) {
        postMapper.deleteById(postId);
        postMapper.deleteImagesByPostId(postId);
        postMapper.deleteTagsByPostId(postId);
        log.info("物理删除帖子: postId={}", postId);
    }

    // ══════════════════════════════════════════════
    // 评论管理
    // ══════════════════════════════════════════════

    @Override
    public PageResult<CommentVO> getCommentList(AdminCommentQuery query) {
        int offset = (query.getPage() - 1) * query.getPageSize();
        List<CommentVO> list = commentMapper.selectAdminList(
                query.getKeyword(), query.getUserId(), query.getPostId(),
                query.getStatus(), query.getStartTime(), query.getEndTime(),
                offset, query.getPageSize());
        long total = commentMapper.countAdminList(
                query.getKeyword(), query.getUserId(), query.getPostId(),
                query.getStatus(), query.getStartTime(), query.getEndTime());
        return PageResult.of(list, total, query.getPage(), query.getPageSize());
    }

    @Override
    public CommentVO getCommentDetail(Long commentId) {
        return commentMapper.selectByIdWithContext(commentId);
    }

    @Override
    public void deleteComment(Long operatorId, Long commentId) {
        commentMapper.softDeleteById(commentId);
        // 更新帖子评论数 -1
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null && comment.getPostId() != null) {
            commentMapper.decrementCommentCount(comment.getPostId());
        }
        log.info("管理员 {} 删除了评论 {}", operatorId, commentId);
    }

    @Override
    public void restoreComment(Long commentId) {
        // 恢复评论：设 status=1
        // 由于 softDeleteById 是设 status=0，恢复需用 batchRestore
        List<Long> singleList = List.of(commentId);
        commentMapper.batchRestore(singleList);
        log.info("恢复评论: commentId={}", commentId);
    }

    @Override
    public void batchDeleteComments(List<Long> ids) {
        commentMapper.batchSoftDelete(ids);
        log.info("批量删除评论: ids={}", ids);
    }

    @Override
    public void batchRestoreComments(List<Long> ids) {
        commentMapper.batchRestore(ids);
        log.info("批量恢复评论: ids={}", ids);
    }

    // ══════════════════════════════════════════════
    // 举报处理
    // ══════════════════════════════════════════════

    @Override
    public PageResult<AdminReportDetailVO> getReportList(Integer status, Integer targetType,
                                                           int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<AdminReportDetailVO> list = reportMapper.selectAdminListWithDetail(
                status, targetType, offset, pageSize);
        long total = reportMapper.countAdminListWithDetail(status, targetType);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public AdminReportDetailVO getReportDetail(Long reportId) {
        // 使用列表查询并过滤，因为我们没有单独的详情查询方法
        // 实际上使用原始 ReportMapper.selectById 获取基本信息
        return null; // 简化处理，前端可组合调用
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long operatorId, Long reportId, Integer status, String remark,
                              Integer penaltyAction, Integer penaltyDuration, String penaltyReason) {
        // 1. 更新举报状态
        reportMapper.handleReport(reportId, status, remark, operatorId);

        // 2. 获取举报信息，确定处罚目标
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            log.warn("举报不存在: reportId={}", reportId);
            return;
        }

        // 3. 执行处罚联动（举报通过时）
        if (status != null && status == 1 && penaltyAction != null) {
            executePenalty(operatorId, report, penaltyAction, penaltyDuration, penaltyReason);
        }

        // 4. 通知举报人处理结果
        try {
            String resultMsg = status == 1 ? "已通过，感谢您的举报" : "已驳回";
            NotificationCreateRequest notifReq = new NotificationCreateRequest();
            notifReq.setRecipientId(report.getUserId());
            notifReq.setType(7);
            notifReq.setContent(String.format("您的举报已被处理：%s。处理备注：%s",
                    resultMsg, remark != null ? remark : "无"));
            notificationService.createNotification(operatorId, notifReq);
        } catch (Exception e) {
            log.warn("举报处理通知发送失败: reportId={}, {}", reportId, e.getMessage());
        }

        log.info("管理员 {} 处理举报 {}: status={}, penaltyAction={}",
                operatorId, reportId, status, penaltyAction);
    }

    /**
     * 执行举报处罚联动
     * penaltyAction: 1=下架帖子 2=删除评论 3=禁言用户 4=禁用账号
     */
    private void executePenalty(Long operatorId, Report report, Integer penaltyAction,
                                 Integer penaltyDuration, String penaltyReason) {
        switch (penaltyAction) {
            case 1: // 下架帖子
                if (report.getTargetType() == 1) {
                    postMapper.updateStatus(report.getTargetId(), 3);
                    postMapper.insertStatusLog(report.getTargetId(), operatorId,
                            null, 3, penaltyReason != null ? penaltyReason : report.getReason());
                }
                break;
            case 2: // 删除评论
                if (report.getTargetType() == 2) {
                    commentMapper.softDeleteById(report.getTargetId());
                }
                break;
            case 3: // 禁言用户
                Long muteUserId = report.getTargetType() == 3 ?
                        report.getTargetId() : getUserIdFromTarget(report);
                if (muteUserId != null) {
                    MuteRequest muteReq = new MuteRequest();
                    muteReq.setUserIds(List.of(muteUserId));
                    muteReq.setReason(penaltyReason != null ? penaltyReason : report.getReason());
                    muteReq.setDuration(penaltyDuration);
                    muteUsers(operatorId, muteReq);
                }
                break;
            case 4: // 禁用账号
                Long disableUserId = report.getTargetType() == 3 ?
                        report.getTargetId() : getUserIdFromTarget(report);
                if (disableUserId != null) {
                    userMapper.updateStatus(disableUserId, 0);
                }
                break;
        }
    }

    /**
     * 从举报目标获取目标用户ID（帖子作者或评论作者）
     */
    private Long getUserIdFromTarget(Report report) {
        if (report.getTargetType() == 1) {
            return postMapper.selectUserIdByPostId(report.getTargetId());
        } else if (report.getTargetType() == 2) {
            return commentMapper.selectUserIdByCommentId(report.getTargetId());
        }
        return null;
    }

    @Override
    public ReportStatsVO getReportStats() {
        ReportStatsVO stats = new ReportStatsVO();
        stats.setTotalCount(reportMapper.countAdminListWithDetail(null, null));
        stats.setPendingCount(reportMapper.countByStatus(0));
        stats.setHandledCount(reportMapper.countByStatus(1));
        stats.setRejectedCount(reportMapper.countByStatus(2));
        stats.setAvgHandleHours(reportMapper.selectAvgHandleHours());

        long handled = stats.getHandledCount();
        long total = handled + stats.getRejectedCount();
        stats.setPassRate(total > 0 ? (double) handled / total : 0.0);

        return stats;
    }

    // ══════════════════════════════════════════════
    // 标签管理
    // ══════════════════════════════════════════════

    @Override
    public PageResult<Tag> getTagList(String keyword, Integer status, String sortBy,
                                       int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Tag> list = tagMapper.selectAdminList(keyword, status, sortBy, offset, pageSize);
        long total = tagMapper.countAdminList(keyword, status);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public Tag createTag(Tag tag) {
        tagMapper.insertTag(tag);
        log.info("创建标签: id={}, name={}", tag.getId(), tag.getName());
        return tag;
    }

    @Override
    public Tag updateTag(Tag tag) {
        tagMapper.updateTag(tag);
        log.info("更新标签: id={}", tag.getId());
        return tagMapper.selectById(tag.getId());
    }

    @Override
    public void toggleTagStatus(Long tagId, Integer status) {
        tagMapper.updateTagStatus(tagId, status);
        log.info("切换标签状态: id={}, status={}", tagId, status);
    }

    // ══════════════════════════════════════════════
    // 通知管理
    // ══════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long operatorId, AdminNotificationRequest request) {
        if (Boolean.TRUE.equals(request.getBroadcast())) {
            // 全站广播
            List<Long> allUserIds = notificationMapper.selectAllUserIds();
            if (allUserIds.isEmpty()) {
                log.warn("全站广播失败：没有有效用户");
                return;
            }
            List<Notification> batch = new ArrayList<>();
            for (Long userId : allUserIds) {
                Notification n = new Notification();
                n.setUserId(userId);
                n.setType(request.getType() != null ? request.getType() : 7);
                n.setContent(request.getContent());
                n.setSenderId(operatorId);
                batch.add(n);
            }
            // 分批插入，每批500条
            int batchSize = 500;
            for (int i = 0; i < batch.size(); i += batchSize) {
                int end = Math.min(i + batchSize, batch.size());
                notificationMapper.insertBatch(batch.subList(i, end));
            }
            log.info("管理员 {} 发送全站广播: content={}, recipients={}",
                    operatorId, request.getContent(), batch.size());
        } else if (request.getRecipientIds() != null && !request.getRecipientIds().isEmpty()) {
            // 定向通知
            List<Notification> batch = new ArrayList<>();
            for (Long userId : request.getRecipientIds()) {
                Notification n = new Notification();
                n.setUserId(userId);
                n.setType(request.getType() != null ? request.getType() : 7);
                n.setContent(request.getContent());
                n.setSenderId(operatorId);
                batch.add(n);
            }
            notificationMapper.insertBatch(batch);
            log.info("管理员 {} 发送定向通知: content={}, recipients={}",
                    operatorId, request.getContent(), request.getRecipientIds());
        }
    }

    @Override
    public PageResult<Notification> getNotificationRecords(Integer type, Long userId,
                                                            String startTime, String endTime,
                                                            int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Notification> list = notificationMapper.selectAdminList(
                type, userId, startTime, endTime, offset, pageSize);
        long total = notificationMapper.countAdminList(type, userId, startTime, endTime);
        return PageResult.of(list, total, page, pageSize);
    }

    // ══════════════════════════════════════════════
    // 搜索运营管理
    // ══════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> getHotKeywords(String range) {
        String startTime = getRangeStart(range);
        return searchHistoryMapper.selectHotKeywords(startTime, getNowStr(), 50);
    }

    @Override
    public List<Map<String, Object>> getSearchTrend(String range) {
        String startTime = getRangeStart(range);
        return searchHistoryMapper.selectSearchTrend(startTime, getNowStr());
    }

    @Override
    public PageResult<com.txq.xiaohongshu.pojo.SearchHistory> getSearchHistoryList(
            Long userId, String keyword, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<com.txq.xiaohongshu.pojo.SearchHistory> list =
                searchHistoryMapper.selectAdminList(userId, keyword, offset, pageSize);
        long total = searchHistoryMapper.countAdminList(userId, keyword);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public void cleanSearchKeywords(String keyword) {
        searchHistoryMapper.deleteByKeyword(keyword);
        log.info("清理搜索关键词: {}", keyword);
    }

    // ══════════════════════════════════════════════
    // 数据统计看板
    // ══════════════════════════════════════════════

    @Override
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        String today = java.time.LocalDate.now().toString();
        String todayStart = today + " 00:00:00";
        String nowStr = getNowStr();

        // ── 用户数据 ──
        data.put("totalUsers", userMapper.countAdminList(null, null, null, null, null, null));
        data.put("todayNewUsers", countToday(todayStart, "users", "created_at"));
        data.put("mutedUsers", userMapper.countAdminList(null, null, 1, null, null, null));
        Map<String, Long> userStats = new java.util.HashMap<>();
        userStats.put("total", (long) userMapper.countAdminList(null, null, null, null, null, null));
        userStats.put("todayNew", countToday(todayStart, "users", "created_at"));
        userStats.put("muted", (long) userMapper.countAdminList(null, null, 1, null, null, null));
        userStats.put("disabled", (long) userMapper.countAdminList(null, 0, null, null, null, null));
        data.put("userStats", userStats);

        // ── 内容数据 ──
        long totalPosts = postMapper.countAdminList(null, null, null, null, null, null, null, null);
        long todayPosts = countToday(todayStart, "posts", "created_at");
        long removedPosts = postMapper.countAdminList(null, null, null, 3, null, null, null, null);
        data.put("totalPosts", totalPosts);
        data.put("todayPosts", todayPosts);
        data.put("removedPosts", removedPosts);
        Map<String, Long> postStats = new java.util.HashMap<>();
        postStats.put("total", totalPosts);
        postStats.put("todayNew", todayPosts);
        postStats.put("removed", removedPosts);
        postStats.put("image", postMapper.countAdminList(null, null, 1, null, null, null, null, null));
        postStats.put("video", postMapper.countAdminList(null, null, 2, null, null, null, null, null));
        data.put("postStats", postStats);

        // ── 互动数据（今日） ──
        Map<String, Long> interact = new java.util.HashMap<>();
        interact.put("todayLikes", countToday(todayStart, "likes", "created_at"));
        interact.put("todayComments", countToday(todayStart, "comments", "created_at"));
        interact.put("todayCollects", countToday(todayStart, "collection_posts", "created_at"));
        data.put("interactStats", interact);

        // ── 举报数据 ──
        Map<String, Object> reportStats = new java.util.HashMap<>();
        reportStats.put("todayNew", countToday(todayStart, "reports", "created_at"));
        reportStats.put("pending", reportMapper.countByStatus(0));
        reportStats.put("total", reportMapper.countAdminListWithDetail(null, null));
        // 处理率 = 已处理 / (已处理 + 驳回)
        long handled = reportMapper.countByStatus(1);
        long rejected = reportMapper.countByStatus(2);
        long totalDecided = handled + rejected;
        reportStats.put("handleRate", totalDecided > 0 ?
                String.format("%.1f%%", (double) handled / totalDecided * 100) : "0%");
        long all = handled + rejected + reportMapper.countByStatus(0);
        reportStats.put("passRate", all > 0 ?
                String.format("%.1f%%", (double) handled / all * 100) : "0%");
        data.put("reportStats", reportStats);

        return data;
    }

    @Override
    public Map<String, Object> getDashboardTrends(String range) {
        String startTime = getRangeStart(range);
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("userTrend", getTableTrend("users", "created_at", startTime));
        data.put("postTrend", getTableTrend("posts", "created_at", startTime));
        data.put("interactTrend", getInteractTrend(startTime));
        data.put("reportTrend", getTableTrend("reports", "created_at", startTime));
        return data;
    }

    @Override
    public List<Map<String, Object>> getDashboardRankings(String type) {
        switch (type) {
            case "hotPosts": return dashboardMapper.selectHotPosts(20);
            case "activeCreators": return dashboardMapper.selectActiveCreators(20);
            case "hotTags": return dashboardMapper.selectHotTags(20);
            case "topViolators": return dashboardMapper.selectTopViolators(20);
            default: return java.util.Collections.emptyList();
        }
    }

    // ══════════════════════════════════════════════
    // 工具方法
    // ══════════════════════════════════════════════

    private String getRangeStart(String range) {
        java.time.LocalDate today = java.time.LocalDate.now();
        if ("today".equals(range)) return today + " 00:00:00";
        if ("7d".equals(range)) return today.minusDays(7) + " 00:00:00";
        if ("30d".equals(range)) return today.minusDays(30) + " 00:00:00";
        return today.minusDays(7) + " 00:00:00";
    }

    private String getNowStr() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private long countToday(String todayStart, String table, String col) {
        return dashboardMapper.countToday(table, col, todayStart);
    }

    private List<Map<String, Object>> getTableTrend(String table, String col, String startTime) {
        return dashboardMapper.selectTrend(table, col, startTime);
    }

    private List<Map<String, Object>> getInteractTrend(String startTime) {
        return dashboardMapper.selectInteractTrend(startTime);
    }
}
