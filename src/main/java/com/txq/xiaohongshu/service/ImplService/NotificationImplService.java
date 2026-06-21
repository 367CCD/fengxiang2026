package com.txq.xiaohongshu.service.ImplService;

import com.txq.xiaohongshu.mapper.NotificationMapper;
import com.txq.xiaohongshu.pojo.Notification;
import com.txq.xiaohongshu.pojo.NotificationCreateRequest;
import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class
NotificationImplService implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public PageResult<Notification> getNotifications(Long userId, Integer type, Integer isRead, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Notification> list = notificationMapper.selectByUserId(userId, type, isRead, offset, pageSize);
        long total = notificationMapper.countByUserId(userId, type, isRead);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public Map<String, Integer> getUnreadCount(Long userId) {
        int count = notificationMapper.countUnread(userId);
        Map<String, Integer> result = new HashMap<>();
        result.put("count", count);
        return result;
    }

    @Override
    public Notification getNotificationById(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        return notification;
    }

    @Override
    public void markRead(Long notificationId, Long userId) {
        Notification notification = getNotificationById(notificationId);
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该通知");
        }
        notificationMapper.markRead(notificationId);
    }

    @Override
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
    }

    @Override
    public void createNotification(Long senderId, NotificationCreateRequest req) {
        Notification notification = new Notification();
        notification.setUserId(req.getRecipientId());   // 接收者
        notification.setSenderId(senderId);              // 触发者（来自 token）
        notification.setType(req.getType());
        notification.setTargetId(req.getTargetId());
        notification.setTargetType(req.getTargetType());
        notification.setContent(req.getContent());
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }
}
