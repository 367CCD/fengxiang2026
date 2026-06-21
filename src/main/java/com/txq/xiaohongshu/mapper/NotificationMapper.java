package com.txq.xiaohongshu.mapper;

import com.txq.xiaohongshu.pojo.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    /** 插入通知 */
    int insert(Notification notification);

    /** 根据 ID 查询单条通知 */
    Notification selectById(@Param("id") Long id);

    /** 查询用户通知列表（支持按类型和已读状态筛选） */
    List<Notification> selectByUserId(@Param("userId") Long userId,
                                      @Param("type") Integer type,
                                      @Param("isRead") Integer isRead,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /** 通知总数（支持按类型和已读状态筛选） */
    long countByUserId(@Param("userId") Long userId,
                       @Param("type") Integer type,
                       @Param("isRead") Integer isRead);

    /** 未读通知数 */
    int countUnread(@Param("userId") Long userId);

    /** 标记单条已读 */
    int markRead(@Param("id") Long id);

    /** 全部标记已读 */
    int markAllRead(@Param("userId") Long userId);

    // ── 管理端 ──

    /** 批量插入通知（用于广播） */
    int insertBatch(@Param("list") List<Notification> list);

    /** 管理端通知记录查询 */
    List<Notification> selectAdminList(@Param("type") Integer type,
                                        @Param("userId") Long userId,
                                        @Param("startTime") String startTime,
                                        @Param("endTime") String endTime,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    long countAdminList(@Param("type") Integer type,
                        @Param("userId") Long userId,
                        @Param("startTime") String startTime,
                        @Param("endTime") String endTime);

    /** 查询所有用户ID（用于全站广播） */
    List<Long> selectAllUserIds();
}
