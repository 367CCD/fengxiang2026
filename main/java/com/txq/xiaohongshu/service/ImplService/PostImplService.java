package com.txq.xiaohongshu.service.ImplService;

import com.txq.xiaohongshu.mapper.CollectionMapper;
import com.txq.xiaohongshu.mapper.PostMapper;
import com.txq.xiaohongshu.mapper.UserMapper;
import com.txq.xiaohongshu.pojo.*;
import com.txq.xiaohongshu.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostImplService implements PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CollectionMapper collectionMapper;

    @Autowired
    private com.txq.xiaohongshu.mapper.UserMuteHistoryMapper muteHistoryMapper;

    /**
     * 新增帖子（文件已提前上传，此处只做入库 + 关联标签）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Post addPost(Post p, Long userId) {
        // ── 0. 禁言检查（已发布才检查，草稿不限制） ──
        if (p.getStatus() == null || p.getStatus() != 0) {
            checkMuteStatus(userId);
        }

        // ── 1. 写入 posts 表（主键回填到 p.id） ──
        p.setUserId(userId);
        p.setStatus(2);                       // 2 = 审核中
        p.setPublishedAt(LocalDateTime.now());
        postMapper.insert(p);
        // ── 2. 写入 post_images 表 ──
        if (p.getType() == 1 && p.getImageUrls() != null) {
            List<String> imageUrls = p.getImageUrls();
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.get(i);
                if (url != null && !url.isEmpty()) {
                    postMapper.insertImage(p.getId(), url, i);
                }
            }
        }

        // ── 3. 写入 post_tags 表 & 更新标签 post_count ──
        if (p.getTagIds() != null) {
            for (Long tagId : p.getTagIds()) {
                postMapper.insertPostTag(p.getId(), tagId);
                postMapper.incrementTagPostCount(tagId);
            }
        }

        return p;
    }

    @Override
    public PageResult<PostVO> getPostList(int page, int pageSize, String sort,
                                          Long tagId, String q) {
        int offset = (page - 1) * pageSize;
        List<PostVO> list = postMapper.selectList(offset, pageSize, sort, tagId, q);
        long total = postMapper.countList(tagId, q);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public PostDetailVO getPostDetail(Long id, Long currentUserId) {
        PostDetailVO detail = postMapper.selectDetailById(id, currentUserId);
        if (detail != null) {
            detail.setImages(postMapper.selectImagesByPostId(id));
            detail.setTags(postMapper.selectTagsByPostId(id));
            // 检查当前用户是否已点赞
            if (currentUserId != null) {
               LC lc= postMapper.checkPostLCByUser(id,currentUserId);
               if(lc.getIsLiked()>0)detail.setIsLiked(true);
               else detail.setIsLiked(false);
               if(lc.getIsCollected()>0)detail.setIsCollected(true);
               else detail.setIsCollected(false);
            }
        }
        return detail;
    }

    /**
     * 点赞
     * 利用数据库唯一约束防并发重复插入。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Long postId, Long userId) {
        // 1. 校验帖子是否存在且已发布
        PostDetailVO post = postMapper.selectDetailById(postId, null);
        if (post == null) {
            throw new RuntimeException("帖子不存在或已下架");
        }

        // 2. 尝试 INSERT，重复点赞静默忽略
        try {
            postMapper.insertLike(userId, 1, postId);
            postMapper.incrementLikeCount(postId);
        } catch (DuplicateKeyException ignored) {
            // 已点赞，无需操作
        }
    }

    /**
     * 取消点赞
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long postId, Long userId) {
        // 1. 校验帖子是否存在且已发布
        PostDetailVO post = postMapper.selectDetailById(postId, null);
        if (post == null) {
            throw new RuntimeException("帖子不存在或已下架");
        }

        // 2. 删除点赞记录，只有确实删除了才减计数
        int deleted = postMapper.deleteLike(userId, 1, postId);
        if (deleted > 0) {
            postMapper.decrementLikeCount(postId);
        }
    }

    /**
     * 删除帖子（仅作者或管理员）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId, Long userId) {
        PostDetailVO post = postMapper.selectDetailById(postId, null);
        if (post == null) {
            throw new RuntimeException("帖子不存在或已下架");
        }
        // 检查是否为作者或管理员
        User user = userMapper.selectById(userId);
        boolean isAdmin = user != null && user.getRole() != null && user.getRole() == 1;
        if (!post.getUserId().equals(userId) && !isAdmin) {
            throw new RuntimeException("无权删除他人帖子");
        }
        // 先删除关联数据
        postMapper.deleteImagesByPostId(postId);
        postMapper.deleteTagsByPostId(postId);
        // 删除帖子
        postMapper.deleteById(postId);
    }

    /**
     * 收藏帖子到指定收藏夹
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectPost(Long postId, Long userId, Long collectionId) {
        PostDetailVO post = postMapper.selectDetailById(postId, null);
        if (post == null) {
            throw new RuntimeException("帖子不存在或已下架");
        }
        try {
            postMapper.insertCollect(postId, collectionId);
            postMapper.incrementCollectCount(postId);
            collectionMapper.incrementPostCount(collectionId);
        } catch (DuplicateKeyException ignored) {
            // 已收藏，幂等
        }
    }

    @Override
    public Post saveDraft(Post p, Long userId) {
        p.setUserId(userId);
        p.setStatus(0);  // 草稿
        postMapper.insert(p);
        // 图片和标签也一并保存
        if (p.getType() != null && p.getType() == 1 && p.getImageUrls() != null) {
            int i = 0;
            for (String url : p.getImageUrls()) {
                if (url != null && !url.isEmpty()) {
                    postMapper.insertImage(p.getId(), url, i++);
                }
            }
        }
        if (p.getTagIds() != null) {
            for (Long tagId : p.getTagIds()) {
                postMapper.insertPostTag(p.getId(), tagId);
            }
        }
        return p;
    }

    @Override
    public PageResult<PostVO> getPostsByUserId(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<PostVO> list = postMapper.selectByUserId(userId, offset, pageSize);
        long total = postMapper.countByUserId(userId);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public PageResult<PostVO> getDraftsByUserId(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<PostVO> list = postMapper.selectDraftsByUserId(userId, offset, pageSize);
        long total = postMapper.countDraftsByUserId(userId);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Post publishDraft(Post p, Long postId, Long userId) {
        // 校验权限：仅作者本人
        PostDetailVO existing = postMapper.selectAdminDetailById(postId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        // 更新帖子
        postMapper.updatePost(postId, p.getTitle(), p.getContent(), p.getCoverUrl(),
                p.getType(), p.getVideoUrl(), p.getLocation(), p.getStatus());

        // 更新图片：先删后插
        if (p.getImageUrls() != null) {
            postMapper.deleteImagesByPostId(postId);
            int i = 0;
            for (String url : p.getImageUrls()) {
                if (url != null && !url.isEmpty()) {
                    postMapper.insertImage(postId, url, i++);
                }
            }
        }
        // 更新标签：先删后插
        if (p.getTagIds() != null) {
            postMapper.deleteTagsByPostId(postId);
            for (Long tagId : p.getTagIds()) {
                postMapper.insertPostTag(postId, tagId);
            }
        }

        postMapper.updateStatus(postId, p.getStatus() != null ? p.getStatus() : 1);
        return p;
    }

    @Override
    public PageResult<PostVO> getFollowPostList(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<PostVO> list = postMapper.selectFollowPosts(userId, offset, pageSize);
        long total = postMapper.countFollowPosts(userId);

        // 批量填充帖子图片
        if (!list.isEmpty()) {
            List<Long> postIds = list.stream().map(PostVO::getId).collect(Collectors.toList());
            List<PostDetailVO.ImageItem> allImages = postMapper.selectImagesByPostIds(postIds);
            // 按 postId 分组
            Map<Long, List<String>> imageMap = new HashMap<>();
            for (PostDetailVO.ImageItem img : allImages) {
                imageMap.computeIfAbsent(img.getPostId(), k -> new ArrayList<>()).add(img.getUrl());
            }
            for (PostVO vo : list) {
                List<String> images = imageMap.get(vo.getId());
                vo.setImages(images != null ? images : Collections.emptyList());
            }
        }

        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public long getNewFollowPostCount(Long userId, String lastTime) {
        return postMapper.countNewFollowPosts(userId, lastTime);
    }

    /**
     * 取消收藏帖子
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectPost(Long postId, Long userId) {
        PostDetailVO post = postMapper.selectDetailById(postId, null);
        if (post == null) {
            throw new RuntimeException("帖子不存在或已下架");
        }
        // 查到该帖子所属的收藏夹ID
        Long collectionId = postMapper.findCollectionIdByPostAndUser(postId, userId);
        if (collectionId == null) {
            return; // 未收藏，幂等返回
        }
        int deleted = postMapper.deleteCollect(postId);
        if (deleted > 0) {
            postMapper.decrementCollectCount(postId);
            collectionMapper.decrementPostCount(collectionId);
        }
    }

    /**
     * 检查用户禁言状态（懒解封机制）
     * - isMuted=0 → 正常放行
     * - isMuted=1 且 muteEndTime 已过期 → 自动懒解封，放行
     * - isMuted=1 且 未到期/永久 → 拒绝
     */
    private void checkMuteStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getIsMuted() == null || user.getIsMuted() == 0) {
            return;
        }

        if (user.getMuteEndTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (user.getMuteEndTime().isBefore(now)) {
                // 懒解封
                userMapper.batchClearMute(java.util.List.of(userId));
                UserMuteHistory history = new UserMuteHistory();
                history.setUserId(userId);
                history.setOperatorId(userId);
                history.setAction(2);
                history.setReason("禁言到期，系统自动懒解封");
                history.setUnmuteType(2);
                muteHistoryMapper.insert(history);
                return;
            }
            String endTime = user.getMuteEndTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            throw new RuntimeException("您已被禁言至 " + endTime + "，原因：" +
                    (user.getMutedReason() != null ? user.getMutedReason() : "违规"));
        }

        throw new RuntimeException("您已被永久禁言，原因：" +
                (user.getMutedReason() != null ? user.getMutedReason() : "违规"));
    }
}