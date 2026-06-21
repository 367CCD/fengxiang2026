package com.txq.xiaohongshu.service;

import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.Post;
import com.txq.xiaohongshu.pojo.PostDetailVO;
import com.txq.xiaohongshu.pojo.PostVO;

public interface PostService {
    /** 新增帖子：上传文件 → 写入数据库 → 关联标签 */
    Post addPost(Post p, Long userId) throws Exception;

    /** 瀑布流分页查询 */
    PageResult<PostVO> getPostList(int page, int pageSize, String sort,
                                  Long tagId, String q);

    /** 帖子详情 */
    PostDetailVO getPostDetail(Long id, Long currentUserId);

    /**
     * 点赞
     * @param postId 帖子 ID
     * @param userId 当前用户 ID
     */
    void like(Long postId, Long userId);

    /**
     * 取消点赞
     * @param postId 帖子 ID
     * @param userId 当前用户 ID
     */
    void unlike(Long postId, Long userId);

    /**
     * 删除帖子（仅作者或管理员）
     */
    void deletePost(Long postId, Long userId);

    /**
     * 收藏帖子到指定收藏夹
     */
    void collectPost(Long postId, Long userId, Long collectionId);

    /**
     * 取消收藏帖子
     */
    void uncollectPost(Long postId, Long userId);

    /** 获取某用户的帖子列表 */
    PageResult<PostVO> getPostsByUserId(Long userId, int page, int pageSize);

    /** 保存草稿（status=0） */
    Post saveDraft(Post p, Long userId);

    /** 获取某用户的草稿列表 */
    PageResult<PostVO> getDraftsByUserId(Long userId, int page, int pageSize);

    /** 将草稿发布（status: 0→1）或更新草稿 */
    Post publishDraft(Post p, Long postId, Long userId);

    /** 分页获取关注流帖子 */
    PageResult<PostVO> getFollowPostList(Long userId, int page, int pageSize);

    /** 获取关注流新增帖子数量 */
    long getNewFollowPostCount(Long userId, String lastTime);
}
