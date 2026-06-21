package com.txq.xiaohongshu.mapper;

import com.txq.xiaohongshu.pojo.Collection;
import com.txq.xiaohongshu.pojo.PostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollectionMapper {

    /** 插入收藏夹 */
    int insert(Collection collection);

    /** 根据ID查询 */
    Collection selectById(@Param("id") Long id);

    /** 查询用户的收藏夹列表（所有者查看全部，访客只看公开） */
    List<Collection> selectByUserId(@Param("userId") Long userId,
                                    @Param("viewerId") Long viewerId);

    /** 查询收藏夹中的帖子 */
    List<PostVO> selectPostsByCollectionId(@Param("collectionId") Long collectionId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /** 收藏夹中帖子总数 */
    long countPostsByCollectionId(@Param("collectionId") Long collectionId);

    /** 收藏夹帖子数 +1 */
    int incrementPostCount(@Param("collectionId") Long collectionId);

    /** 收藏夹帖子数 -1 */
    int decrementPostCount(@Param("collectionId") Long collectionId);

    /** 删除收藏夹 */
    int deleteById(@Param("id") Long id);

    /** 删除收藏夹下所有帖子关联 */
    int deletePostsByCollectionId(@Param("collectionId") Long collectionId);

}