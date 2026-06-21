package com.txq.xiaohongshu.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Post {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String coverUrl;
    private String location;
    /** 1=图文, 2=视频 */
    private Integer type;
    private String videoUrl;
    /** 发布状态: 1=发布 */
    private Integer status;
    private LocalDateTime publishedAt;

    // ── 以下字段由 OSS 上传后回填 ──
    /** 上传后的图片 URL 列表 */
    private List<String> imageUrls;


    /** 关联标签 ID 列表 */
    private List<Long> tagIds;
}
