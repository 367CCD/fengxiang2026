package com.txq.xiaohongshu.service.ImplService;

import com.txq.xiaohongshu.mapper.CollectionMapper;
import com.txq.xiaohongshu.pojo.Collection;
import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.PostVO;
import com.txq.xiaohongshu.service.CollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CollectionImplService implements CollectionService {

    @Autowired
    private CollectionMapper collectionMapper;

    @Override
    public List<Collection> getCollections(Long userId, Long viewerId) {
        return collectionMapper.selectByUserId(userId, viewerId);
    }

    @Override
    public PageResult<PostVO> getCollectionPosts(Long collectionId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<PostVO> list = collectionMapper.selectPostsByCollectionId(collectionId, offset, pageSize);
        long total = collectionMapper.countPostsByCollectionId(collectionId);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public Collection createCollection(Collection collection, Long userId) {
        collection.setUserId(userId);
        collectionMapper.insert(collection);
        return collection;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCollection(Long collectionId, Long userId) {
        Collection c = collectionMapper.selectById(collectionId);
        if (c == null) {
            throw new RuntimeException("收藏夹不存在");
        }
        if (!c.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除他人收藏夹");
        }
        // 先清空收藏夹内的帖子关联，再删收藏夹
        collectionMapper.deletePostsByCollectionId(collectionId);
        collectionMapper.deleteById(collectionId);
    }
}