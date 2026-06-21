package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子列表视图对象 — 瀑布流展示用，含发帖用户信息
 */
@Data
public class PostVO {
    private Long id;
    private String title;
    private String content;
    private String coverUrl;
    /** 1=图文, 2=视频 */
    private Integer type;
    /** 帖子图片数组（关注流使用） */
    private List<String> images;
    private Integer status;          // 帖子状态（管理端用）
    private Long viewCount;          // 浏览数（管理端用）
    private Long shareCount;         // 分享数（管理端用）
    private Integer isTop;           // 是否置顶（管理端用）
    private Integer isEssence;       // 是否精华（管理端用）
    private LocalDateTime publishedAt; // 发布时间（管理端用）
    private LocalDateTime updatedAt; // 更新时间（草稿用）
    private Long likeCount;
    private Integer commentCount;
    private Integer collectCount;
    /** 当前用户是否已点赞 */
    private Boolean isLiked;
    /** 当前用户是否已收藏 */
    private Boolean isCollected;
    private LocalDateTime createdAt;
    private Long userId;
    private String username;
    private String avatarUrl;

}