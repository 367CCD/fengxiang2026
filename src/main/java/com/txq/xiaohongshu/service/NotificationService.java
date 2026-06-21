package com.txq.xiaohongshu.service;

import com.txq.xiaohongshu.pojo.Notification;
import com.txq.xiaohongshu.pojo.NotificationCreateRequest;
import com.txq.xiaohongshu.pojo.PageResult;

import java.util.Map;

public interface NotificationService {

    /** 通知列表（支持按类型和已读状态筛选） */
    PageResult<Notification> getNotifications(Long userId, Integer type, Integer isRead, int page, int pageSize);

    /** 未读通知数 */
    Map<String, Integer> getUnreadCount(Long userId);

    /** 根据 ID 查询单条通知（用于归属校验） */
    Notification getNotificationById(Long notificationId);

    /** 标记单条已读（校验归属） */
    void markRead(Long notificationId, Long userId);

    /** 全部标记已读 */
    void markAllRead(Long userId);

    /** 创建通知（sender 从 token 中获取） */
    void createNotification(Long senderId, NotificationCreateRequest req);
}
