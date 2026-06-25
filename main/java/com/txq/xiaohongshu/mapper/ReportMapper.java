package com.txq.xiaohongshu.mapper;

import com.txq.xiaohongshu.pojo.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    /** 插入举报记录，主键回填 */
    int insert(Report report);

    /** 根据ID查询 */
    Report selectById(@Param("id") Long id);

    /** 查询用户自己的举报列表 */
    List<Report> selectByUserId(@Param("userId") Long userId,
                                @Param("status") Integer status,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    /** 用户举报总数 */
    long countByUserId(@Param("userId") Long userId,
                       @Param("status") Integer status);

    /** 管理员分页查询举报列表 */
    List<Report> selectList(@Param("status") Integer status,
                            @Param("targetType") Integer targetType,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    /** 管理员举报总数 */
    long countList(@Param("status") Integer status,
                   @Param("targetType") Integer targetType);

    /** 处理举报（更新状态、处理人、处理备注、处理时间） */
    int handleReport(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("handleRemark") String handleRemark,
                     @Param("handlerId") Long handlerId);

    // ── 管理端增强 ──

    List<com.txq.xiaohongshu.pojo.AdminReportDetailVO> selectAdminListWithDetail(
            @Param("status") Integer status,
            @Param("targetType") Integer targetType,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countAdminListWithDetail(@Param("status") Integer status,
                                  @Param("targetType") Integer targetType);

    /** 举报各状态数量统计 */
    long countByStatus(@Param("status") Integer status);

    /** 举报平均处理时长（小时） */
    Double selectAvgHandleHours();
}