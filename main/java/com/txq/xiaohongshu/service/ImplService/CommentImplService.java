package com.txq.xiaohongshu.service.ImplService;

import com.txq.xiaohongshu.mapper.CommentMapper;
import com.txq.xiaohongshu.mapper.PostMapper;
import com.txq.xiaohongshu.pojo.*;
import com.txq.xiaohongshu.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentImplService implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private com.txq.xiaohongshu.mapper.UserMapper userMapper;

    @Autowired
    private com.txq.xiaohongshu.mapper.UserMuteHistoryMapper muteHistoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Comment addComment(Comment comment, Long userId) {
        // 校验帖子存在
        PostDetailVO post = postMapper.selectDetailById(comment.getPostId(), null);
        if (post == null) {
            throw new RuntimeException("帖子不存在或已下架");
        }
        // 二级回复：校验父评论存在
        if (comment.getParentId() != null) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent == null || parent.getParentId() != null) {
                throw new RuntimeException("父评论不存在或不是一级评论");
            }
        }

        // 禁言检查（含懒解封）
        checkMuteStatus(userId);

        comment.setUserId(userId);
        commentMapper.insert(comment);
        // 更新帖子评论数
        commentMapper.incrementCommentCount(comment.getPostId());
        return comment;
    }

    /**
     * 检查用户禁言状态，处理懒解封机制
     * - isMuted=0 → 正常放行
     * - isMuted=1 且 muteEndTime 已过期 → 自动懒解封，放行
     * - isMuted=1 且 未到期/永久 → 拒绝
     */
    private void checkMuteStatus(Long userId) {
        com.txq.xiaohongshu.pojo.User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getIsMuted() == null || user.getIsMuted() == 0) {
            return; // 正常状态
        }

        // isMuted == 1，检查到期时间
        if (user.getMuteEndTime() != null) {
            // 有到期时间，检查是否已过期
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (user.getMuteEndTime().isBefore(now)) {
                // 已过期 → 懒解封：清除禁言状态
                userMapper.batchClearMute(java.util.List.of(userId));

                // 记录懒解封历史
                com.txq.xiaohongshu.pojo.UserMuteHistory history =
                        new com.txq.xiaohongshu.pojo.UserMuteHistory();
                history.setUserId(userId);
                history.setOperatorId(userId); // 系统自动操作
                history.setAction(2);          // 解封
                history.setReason("禁言到期，系统自动懒解封");
                history.setUnmuteType(2);       // 系统自动懒解封
                muteHistoryMapper.insert(history);

                return; // 解封完成，放行
            }
            // 未过期 → 拒绝
            String endTime = user.getMuteEndTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            throw new RuntimeException("您已被禁言至 " + endTime + "，原因：" +
                    (user.getMutedReason() != null ? user.getMutedReason() : "违规"));
        }

        // muteEndTime 为 NULL → 永久禁言
        throw new RuntimeException("您已被永久禁言，原因：" +
                (user.getMutedReason() != null ? user.getMutedReason() : "违规"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        // 只能删除自己的评论
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除他人评论");
        }
        // 软删除
        commentMapper.softDeleteById(commentId);
        // 更新帖子评论数
        commentMapper.decrementCommentCount(comment.getPostId());
    }

    @Override
    public PageResult<CommentVO> getCommentList(Long postId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<CommentVO> list = commentMapper.selectByPostId(postId, offset, pageSize);
        long total = commentMapper.countByPostId(postId);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public PageResult<CommentVO> getReplies(Long postId, Long parentId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<CommentVO> list = commentMapper.selectReplies(postId, parentId, offset, pageSize);
        long total = commentMapper.countReplies(postId, parentId);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        try {
            commentMapper.insertLike(userId, 2, commentId);
            commentMapper.incrementLikeCount(commentId);
        } catch (DuplicateKeyException ignored) {
            // 已点赞，幂等
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        int deleted = commentMapper.deleteLike(userId, 2, commentId);
        if (deleted > 0) {
            commentMapper.decrementLikeCount(commentId);
        }
    }
}