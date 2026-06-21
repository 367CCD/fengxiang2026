package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子详情视图 — GET /posts/{id}
 */
@Data
public class PostDetailVO {
    private Long id;
    private String title;
    private String content;
    private String coverUrl;
    /** 1=图文, 2=视频 */
    private Integer type;
    private String videoUrl;
    private String location;
    private Integer status;          // 帖子状态（管理端使用）
    private Integer isTop;           // 是否置顶（管理端使用）
    private Integer isEssence;       // 是否精华（管理端使用）
    private Long viewCount;
    private Long likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private Integer shareCount;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private Long userId;
    private String username;
    private String avatarUrl;

    /** 当前登录用户是否已点赞 收藏*/
    private Boolean isLiked;
    private Boolean isCollected;
    /** 当前登录用户是否已关注帖子作者 */
    private Boolean isFollowed;
    /** 帖子图片列表 */
    private List<ImageItem> images;

    /** 关联标签列表 */
    private List<TagItem> tags;

    @Data
    public static class ImageItem {
        private Long id;
        private Long postId;
        private String url;
        private Integer sortOrder;
        private Integer width;
        private Integer height;
    }

    @Data
    public static class TagItem {
        private Long id;
        private String name;
    }
}