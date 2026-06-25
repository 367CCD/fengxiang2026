package com.txq.xiaohongshu.mapper;

import com.txq.xiaohongshu.pojo.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {

    /** 查询所有启用状态的标签，按帖子数降序 */
    List<Tag> selectAll();

    // ── 管理端 ──

    List<Tag> selectAdminList(@Param("keyword") String keyword,
                               @Param("status") Integer status,
                               @Param("sortBy") String sortBy,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    long countAdminList(@Param("keyword") String keyword,
                        @Param("status") Integer status);

    int insertTag(Tag tag);

    int updateTag(Tag tag);

    int updateTagStatus(@Param("id") Long id, @Param("status") Integer status);

    Tag selectById(@Param("id") Long id);
}