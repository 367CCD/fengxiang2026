package com.txq.xiaohongshu.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    /** 统计某表今日新增数 */
    long countToday(@Param("tableName") String tableName,
                    @Param("colName") String colName,
                    @Param("todayStart") String todayStart);

    /** 表趋势（按天聚合） */
    List<Map<String, Object>> selectTrend(@Param("tableName") String tableName,
                                           @Param("colName") String colName,
                                           @Param("startTime") String startTime);

    /** 互动趋势（点赞+评论+收藏按天聚合） */
    List<Map<String, Object>> selectInteractTrend(@Param("startTime") String startTime);

    /** 热门帖子 TOP N */
    List<Map<String, Object>> selectHotPosts(@Param("limit") int limit);

    /** 活跃创作者 TOP N */
    List<Map<String, Object>> selectActiveCreators(@Param("limit") int limit);

    /** 热门标签 TOP N */
    List<Map<String, Object>> selectHotTags(@Param("limit") int limit);

    /** 高频违规用户 TOP N */
    List<Map<String, Object>> selectTopViolators(@Param("limit") int limit);
}
