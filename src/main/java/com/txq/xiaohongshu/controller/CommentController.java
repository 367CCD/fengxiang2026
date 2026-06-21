package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.pojo.Comment;
import com.txq.xiaohongshu.pojo.CommentVO;
import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.Result;
import com.txq.xiaohongshu.service.CommentService;
import com.txq.xiaohongshu.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 3.1 获取帖子评论列表
     */
    @GetMapping("/posts/{postId}/comments")
    public Result list(@PathVariable Long postId,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<CommentVO> result = commentService.getCommentList(postId, page, pageSize);
        return Result.sucess(result);
    }

    /**
     * 3.2 获取指定一级评论的二级回复列表
     */
    @GetMapping("/posts/{postId}/comments/{parentId}/replies")
    public Result replies(@PathVariable Long postId,
                          @PathVariable Long parentId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<CommentVO> result = commentService.getReplies(postId, parentId, page, pageSize);
        return Result.sucess(result);
    }

    /**
     * 3.3 发表评论/回复
     */
    @PostMapping("/comments")
    public Result create(@RequestBody Comment comment, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            Comment saved = commentService.addComment(comment, userId);
            log.info("用户 {} 发表了评论 commentId={}", userId, saved.getId());
            return Result.sucess(saved);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 3.4 删除评论
     */
    @DeleteMapping("/comments/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            commentService.deleteComment(id, userId);
            log.info("用户 {} 删除了评论 commentId={}", userId, id);
            return Result.sucess();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 3.5 点赞评论
     */
    @PostMapping("/comments/{id}/like")
    public Result like(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            commentService.likeComment(id, userId);
            log.info("用户 {} 点赞评论 {}", userId, id);
            return Result.sucess();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 3.6 取消点赞评论
     */
    @DeleteMapping("/comments/{id}/like")
    public Result unlike(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            commentService.unlikeComment(id, userId);
            log.info("用户 {} 取消点赞评论 {}", userId, id);
            return Result.sucess();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}