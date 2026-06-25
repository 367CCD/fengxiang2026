package com.txq.xiaohongshu.service;

import com.txq.xiaohongshu.pojo.Collection;
import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.PostVO;

import java.util.List;

public interface CollectionService {

    /** 获取用户收藏夹列表（viewerId=所有者看全部，访客只看公开） */
    List<Collection> getCollections(Long userId, Long viewerId);

    /** 获取收藏夹中的帖子 */
    PageResult<PostVO> getCollectionPosts(Long collectionId, int page, int pageSize);

    /** 创建收藏夹 */
    Collection createCollection(Collection collection, Long userId);

    /** 删除收藏夹（仅所有者） */
    void deleteCollection(Long collectionId, Long userId);
}