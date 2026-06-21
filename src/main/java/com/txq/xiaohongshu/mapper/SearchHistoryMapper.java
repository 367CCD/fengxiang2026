package com.txq.xiaohongshu.mapper;

import com.txq.xiaohongshu.pojo.SearchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SearchHistoryMapper {

    /**
     * Upsert 写入搜索历史：关键词已存在则更新最后搜索时间，不存在则新增。
     * 依赖 uk_user_keyword 唯一索引保证原子性。
     */
    int upsert(@Param("userId") Long userId, @Param("keyword") String keyword);

    /** 查询用户搜索历史（按最后搜索时间倒序，限制条数） */
    List<SearchHistory> selectByUserId(@Param("userId") Long userId,
                                       @Param("limit") int limit);

    /** 清空用户搜索历史 */
    int deleteByUserId(@Param("userId") Long userId);

    /** 删除用户最旧的历史记录（超出上限时调用） */
    int deleteOldestByUserId(@Param("userId") Long userId,
                             @Param("keepCount") int keepCount);

    // ── 管理端 ──

    /** 热门搜索关键词排行（按时间范围） */
    List<java.util.Map<String, Object>> selectHotKeywords(@Param("startTime") String startTime,
                                                           @Param("endTime") String endTime,
                                                           @Param("limit") int limit);

    /** 搜索趋势（按天聚合） */
    List<java.util.Map<String, Object>> selectSearchTrend(@Param("startTime") String startTime,
                                                           @Param("endTime") String endTime);

    /** 管理端搜索历史列表 */
    List<SearchHistory> selectAdminList(@Param("userId") Long userId,
                                         @Param("keyword") String keyword,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    long countAdminList(@Param("userId") Long userId,
                        @Param("keyword") String keyword);

    /** 按关键词删除搜索历史 */
    int deleteByKeyword(@Param("keyword") String keyword);
}
