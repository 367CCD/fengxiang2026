package com.txq.xiaohongshu.mapper;

import com.txq.xiaohongshu.pojo.LC;
import com.txq.xiaohongshu.pojo.Post;
import com.txq.xiaohongshu.pojo.PostDetailVO;
import com.txq.xiaohongshu.pojo.PostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper {

    /** 新增帖子，主键回填到 post.id */
    int insert(Post post);

    /** 新增帖子图片 */
    int insertImage(@Param("postId") Long postId,
                    @Param("url") String url,
                    @Param("sortOrder") Integer sortOrder);

    /** 新增帖子-标签关联 */
    int insertPostTag(@Param("postId") Long postId,
                      @Param("tagId") Long tagId);

    /** 标签关联帖子数 +1 */
    int incrementTagPostCount(@Param("tagId") Long tagId);

    /** 瀑布流帖子列表 */
    List<PostVO> selectList(@Param("offset") int offset,
                            @Param("pageSize") int pageSize,
                            @Param("sort") String sort,
                            @Param("tagId") Long tagId,
                            @Param("q") String q);

    /** 瀑布流帖子总数 */
    long countList(@Param("tagId") Long tagId,
                   @Param("q") String q);

    /** 帖子详情（含用户信息 + 是否已关注） */
    PostDetailVO selectDetailById(@Param("id") Long id,
                                  @Param("currentUserId") Long currentUserId);

    /** 帖子图片列表 */
    List<PostDetailVO.ImageItem> selectImagesByPostId(@Param("postId") Long postId);

    /** 帖子关联标签列表 */
    List<PostDetailVO.TagItem> selectTagsByPostId(@Param("postId") Long postId);

    // ── 点赞相关 ──

    /** 插入点赞记录 */
    int insertLike(@Param("userId") Long userId,
                   @Param("targetType") Integer targetType,
                   @Param("targetId") Long targetId);

    /** 删除点赞记录 */
    int deleteLike(@Param("userId") Long userId,
                   @Param("targetType") Integer targetType,
                   @Param("targetId") Long targetId);

    /** 查询是否已点赞（返回计数，0=未点赞，1=已点赞） */
    int checkLiked(@Param("userId") Long userId,
                   @Param("targetType") Integer targetType,
                   @Param("targetId") Long targetId);

    /** 检查当前用户是否已点赞某帖子（返回0或1） */
    LC checkPostLCByUser(@Param("postId") Long postId,
                         @Param("userId") Long userId);

    /** 帖子点赞数 +1 */
    int incrementLikeCount(@Param("postId") Long postId);

    /** 帖子点赞数 -1 */
    int decrementLikeCount(@Param("postId") Long postId);

    /** 删除帖子（物理删除） */
    int deleteById(@Param("id") Long id);

    /** 删除帖子所有图片 */
    int deleteImagesByPostId(@Param("postId") Long postId);

    /** 删除帖子所有标签关联 */
    int deleteTagsByPostId(@Param("postId") Long postId);

    /** 插入收藏记录 */
    int insertCollect(@Param("postId") Long postId,
                      @Param("collectionId") Long collectionId);

    /** 帖子收藏数 +1 */
    int incrementCollectCount(@Param("postId") Long postId);

    /** 帖子收藏数 -1 */
    int decrementCollectCount(@Param("postId") Long postId);

    /** 根据帖子和用户查询所属收藏夹ID */
    Long findCollectionIdByPostAndUser(@Param("postId") Long postId,
                                       @Param("userId") Long userId);

    /** 删除收藏记录 */
    int deleteCollect(@Param("postId") Long postId);

    /** 查询收藏了指定帖子的所有用户ID和收藏夹ID */
    List<java.util.Map<String, Object>> selectCollectorsByPostId(@Param("postId") Long postId);

    /** 删除指定收藏夹中的指定帖子 */
    int deleteFromCollection(@Param("collectionId") Long collectionId,
                             @Param("postId") Long postId);

    /** 查询某用户的帖子列表 */
    List<PostVO> selectByUserId(@Param("userId") Long userId,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    /** 某用户帖子总数 */
    long countByUserId(@Param("userId") Long userId);

    /** 查询某用户的草稿列表 */
    List<PostVO> selectDraftsByUserId(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    /** 某用户草稿总数 */
    long countDraftsByUserId(@Param("userId") Long userId);

    /** 更新帖子（将草稿发布或修改） */
    int updatePost(@Param("id") Long id,
                   @Param("title") String title,
                   @Param("content") String content,
                   @Param("coverUrl") String coverUrl,
                   @Param("type") Integer type,
                   @Param("videoUrl") String videoUrl,
                   @Param("location") String location,
                   @Param("status") Integer status);

    /** 更新帖子状态（用于举报处罚：下架帖子） */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    // ── 关注流相关 ──

    /** 关注流帖子列表（含 isLiked、isCollected） */
    List<PostVO> selectFollowPosts(@Param("userId") Long userId,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    /** 关注流帖子总数 */
    long countFollowPosts(@Param("userId") Long userId);

    /** 关注流新增帖子数量（自 lastTime 以来） */
    long countNewFollowPosts(@Param("userId") Long userId,
                              @Param("lastTime") String lastTime);

    /** 批量查询帖子图片 */
    List<PostDetailVO.ImageItem> selectImagesByPostIds(@Param("postIds") List<Long> postIds);

    // ── 管理端 ──

    List<PostVO> selectAdminList(@Param("keyword") String keyword,
                                  @Param("authorId") Long authorId,
                                  @Param("type") Integer type,
                                  @Param("status") Integer status,
                                  @Param("isTop") Integer isTop,
                                  @Param("isEssence") Integer isEssence,
                                  @Param("startTime") String startTime,
                                  @Param("endTime") String endTime,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    long countAdminList(@Param("keyword") String keyword,
                        @Param("authorId") Long authorId,
                        @Param("type") Integer type,
                        @Param("status") Integer status,
                        @Param("isTop") Integer isTop,
                        @Param("isEssence") Integer isEssence,
                        @Param("startTime") String startTime,
                        @Param("endTime") String endTime);

    /** 管理端帖子详情（可选看所有状态） */
    PostDetailVO selectAdminDetailById(@Param("id") Long id);

    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    int batchSetTop(@Param("ids") List<Long> ids, @Param("isTop") Integer isTop);

    int batchSetEssence(@Param("ids") List<Long> ids, @Param("isEssence") Integer isEssence);

    int insertStatusLog(@Param("postId") Long postId,
                        @Param("operatorId") Long operatorId,
                        @Param("fromStatus") Integer fromStatus,
                        @Param("toStatus") Integer toStatus,
                        @Param("reason") String reason);

    /** 根据帖子ID查询作者ID */
    Long selectUserIdByPostId(@Param("postId") Long postId);
}