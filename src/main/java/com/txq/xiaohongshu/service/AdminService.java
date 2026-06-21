package com.txq.xiaohongshu.service;

import com.txq.xiaohongshu.pojo.*;

import java.util.List;
import java.util.Map;

public interface AdminService {

    // ── 用户管理 ──

    PageResult<User> getUserList(String keyword, Integer status, Integer isMuted,
                                  Integer role, String startTime, String endTime,
                                  int page, int pageSize);

    User getUserDetail(Long userId);

    void batchUpdateUserStatus(List<Long> ids, Integer status);

    void muteUsers(Long operatorId, MuteRequest request);

    void unmuteUsers(Long operatorId, List<Long> ids);

    void resetPassword(Long operatorId, Long userId, String newPassword);

    void resetAvatar(Long operatorId, Long userId);

    PageResult<UserMuteHistory> getUserMuteHistory(Long userId, int page, int pageSize);

    List<User> getMutedExpiredUsers();

    // ── 内容管理 ──

    PageResult<PostVO> getPostList(AdminPostQuery query);

    PostDetailVO getPostDetail(Long postId);

    void updatePostStatus(Long operatorId, Long postId, Integer status, String reason);

    void batchUpdatePostStatus(Long operatorId, List<Long> ids, Integer status, String reason);

    void batchSetTop(List<Long> ids, Integer isTop);

    void batchSetEssence(List<Long> ids, Integer isEssence);

    void deletePost(Long postId);

    // ── 评论管理 ──

    PageResult<CommentVO> getCommentList(AdminCommentQuery query);

    CommentVO getCommentDetail(Long commentId);

    void deleteComment(Long operatorId, Long commentId);

    void restoreComment(Long commentId);

    void batchDeleteComments(List<Long> ids);

    void batchRestoreComments(List<Long> ids);

    // ── 举报处理 ──

    PageResult<AdminReportDetailVO> getReportList(Integer status, Integer targetType,
                                                    int page, int pageSize);

    AdminReportDetailVO getReportDetail(Long reportId);

    void handleReport(Long operatorId, Long reportId, Integer status, String remark,
                      Integer penaltyAction, Integer penaltyDuration, String penaltyReason);

    ReportStatsVO getReportStats();

    // ── 标签管理 ──

    PageResult<Tag> getTagList(String keyword, Integer status, String sortBy,
                                int page, int pageSize);

    Tag createTag(Tag tag);

    Tag updateTag(Tag tag);

    void toggleTagStatus(Long tagId, Integer status);

    // ── 通知管理 ──

    void sendNotification(Long operatorId, AdminNotificationRequest request);

    PageResult<Notification> getNotificationRecords(Integer type, Long userId,
                                                      String startTime, String endTime,
                                                      int page, int pageSize);

    // ── 搜索运营管理 ──

    List<Map<String, Object>> getHotKeywords(String range);

    List<Map<String, Object>> getSearchTrend(String range);

    PageResult<com.txq.xiaohongshu.pojo.SearchHistory> getSearchHistoryList(
            Long userId, String keyword, int page, int pageSize);

    void cleanSearchKeywords(String keyword);

    // ── 数据统计看板 ──

    Map<String, Object> getDashboardOverview();

    Map<String, Object> getDashboardTrends(String range);

    List<Map<String, Object>> getDashboardRankings(String type);
}
