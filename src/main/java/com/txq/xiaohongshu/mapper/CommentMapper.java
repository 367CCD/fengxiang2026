package com.txq.xiaohongshu.mapper;

import com.txq.xiaohongshu.pojo.Comment;
import com.txq.xiaohongshu.pojo.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    /** 插入评论，主键回填 */
    int insert(Comment comment);

    /** 根据ID查询 */
    Comment selectById(@Param("id") Long id);

    /** 软删除：将 status 设为 0 */
    int softDeleteById(@Param("id") Long id);

    /** 帖子的一级评论列表（含用户信息和回复数） */
    List<CommentVO> selectByPostId(@Param("postId") Long postId,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /** 帖子一级评论总数 */
    long countByPostId(@Param("postId") Long postId);

    /** 某条一级评论的二级回复列表 */
    List<CommentVO> selectReplies(@Param("postId") Long postId,
                                  @Param("parentId") Long parentId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /** 某条一级评论的二级回复总数 */
    long countReplies(@Param("postId") Long postId,
                      @Param("parentId") Long parentId);

    /** 帖子评论数 +1 */
    int incrementCommentCount(@Param("postId") Long postId);

    /** 帖子评论数 -1 */
    int decrementCommentCount(@Param("postId") Long postId);

    // ── 评论点赞 ──

    /** 插入评论点赞 */
    int insertLike(@Param("userId") Long userId,
                   @Param("targetType") Integer targetType,
                   @Param("commentId") Long commentId);

    /** 删除评论点赞 */
    int deleteLike(@Param("userId") Long userId,
                   @Param("targetType") Integer targetType,
                   @Param("commentId") Long commentId);

    /** 评论点赞数 +1 */
    int incrementLikeCount(@Param("commentId") Long commentId);

    /** 评论点赞数 -1 */
    int decrementLikeCount(@Param("commentId") Long commentId);

    // ── 管理端 ──

    List<CommentVO> selectAdminList(@Param("keyword") String keyword,
                                     @Param("userId") Long userId,
                                     @Param("postId") Long postId,
                                     @Param("status") Integer status,
                                     @Param("startTime") String startTime,
                                     @Param("endTime") String endTime,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    long countAdminList(@Param("keyword") String keyword,
                        @Param("userId") Long userId,
                        @Param("postId") Long postId,
                        @Param("status") Integer status,
                        @Param("startTime") String startTime,
                        @Param("endTime") String endTime);

    int batchSoftDelete(@Param("ids") List<Long> ids);

    int batchRestore(@Param("ids") List<Long> ids);

    /** 评论详情含上下文 */
    CommentVO selectByIdWithContext(@Param("id") Long id);

    /** 根据评论ID查询作者ID */
    Long selectUserIdByCommentId(@Param("commentId") Long commentId);

    /** 统计某用户评论数 */
    long countByUserId(@Param("userId") Long userId);
}