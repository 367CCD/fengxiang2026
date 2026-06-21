package com.txq.xiaohongshu.service;

import com.txq.xiaohongshu.pojo.Comment;
import com.txq.xiaohongshu.pojo.CommentVO;
import com.txq.xiaohongshu.pojo.PageResult;

public interface CommentService {

    /** 发表评论 */
    Comment addComment(Comment comment, Long userId);

    /** 删除评论（仅评论者或管理员） */
    void deleteComment(Long commentId, Long userId);

    /** 帖子一级评论列表 */
    PageResult<CommentVO> getCommentList(Long postId, int page, int pageSize);

    /** 某条一级评论的二级回复列表 */
    PageResult<CommentVO> getReplies(Long postId, Long parentId, int page, int pageSize);

    /** 点赞评论 */
    void likeComment(Long commentId, Long userId);

    /** 取消点赞评论 */
    void unlikeComment(Long commentId, Long userId);
}