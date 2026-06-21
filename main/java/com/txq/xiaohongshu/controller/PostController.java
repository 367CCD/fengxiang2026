package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.Post;
import com.txq.xiaohongshu.pojo.PostDetailVO;
import com.txq.xiaohongshu.pojo.PostVO;
import com.txq.xiaohongshu.pojo.Result;
import com.txq.xiaohongshu.service.PostService;
import com.txq.xiaohongshu.utils.ALiYunOSSUtils;
import com.txq.xiaohongshu.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private JwtUtils jwtUtils;



    /**
     * 新增帖子
     * 请求体为 JSON，文件先通过上传接口获取 OSS URL 后再传
     * Header: token = JWT
     */
@PostMapping("/posts")
    public Result addPost(@RequestBody Post p, HttpServletRequest request) {
        // 1. 从请求头提取 JWT，解析当前用户 ID
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            log.warn("新增帖子失败：token 无效或已过期");
            return Result.error("请先登录");
        }

        // 2. 参数校验（草稿跳过媒体校验）
        boolean isDraft = p.getStatus() != null && p.getStatus() == 0;
        if (p.getType() == null || (p.getType() != 1 && p.getType() != 2)) {
            return Result.error("帖子类型不正确：1=图文, 2=视频");
        }
        if (!isDraft && p.getType() == 1 && (p.getImageUrls() == null || p.getImageUrls().isEmpty())) {
            return Result.error("图文帖子必须提供至少一个图片 URL");
        }
        if (!isDraft && p.getType() == 2 && (p.getVideoUrl() == null || p.getVideoUrl().isEmpty())) {
            return Result.error("视频帖子必须提供视频 URL");
        }

        // 3. 调用服务层：入库 + 关联标签
        try {
            Post saved;
            if (isDraft) {
                saved = postService.saveDraft(p, userId);
                log.info("用户 {} 保存草稿成功, postId={}", userId, saved.getId());
            } else {
                saved = postService.addPost(p, userId);
                log.info("用户 {} 发布帖子成功, postId={}, type={}", userId, saved.getId(), saved.getType());
            }
            return Result.sucess(saved);
        } catch (Exception e) {
            log.error("发布帖子失败: {}", e.getMessage(), e);
            return Result.error("发布失败，请稍后重试");
        }
    }

    /**
     * 瀑布流帖子列表（公开接口，无需登录）
     */
    @GetMapping("/posts")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int pageSize,
                       @RequestParam(defaultValue = "latest") String sort,
                       @RequestParam(required = false) Long tagId,
                       @RequestParam(required = false) String q) {
        PageResult<PostVO> result = postService.getPostList(page, pageSize, sort, tagId, q);
        return Result.sucess(result);
    }

    /**
     * 帖子详情（公开接口，无需登录，但登录后可查看是否已点赞）
     */
    @GetMapping("/posts/{id}")
    public Result detail(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long currentUserId = (token != null && !token.isEmpty()) ? jwtUtils.getUserId(token) : null;
        PostDetailVO detail = postService.getPostDetail(id, currentUserId);
        if (detail == null) {
            return Result.error("帖子不存在或已下架");
        }
        return Result.sucess(detail);
    }

    /**
     * 上传图片到 OSS
     * 前端发帖前先调此接口上传图片，拿到 URL 后再调 POST /posts 创建帖子
     */
    @PostMapping("/upload/image")
    public Result uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 1. 校验登录
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            log.warn("上传图片失败：token 无效或已过期");
            return Result.error("请先登录");
        }

        // 2. 文件非空校验
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }

        // 3. 上传到OSS，返回可访问URL和尺寸
        try {
            byte[] content = file.getBytes();
            String url = ALiYunOSSUtils.upload(content, file.getOriginalFilename());

            // 读取图片尺寸
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(content));
            int width = img != null ? img.getWidth() : 0;
            int height = img != null ? img.getHeight() : 0;

            log.info("用户 {} 上传图片成功: {}", userId, url);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("url", url);
            result.put("width", width);
            result.put("height", height);
            return Result.sucess(result);
        } catch (Exception e) {
            log.error("图片上传失败: {}", e.getMessage(), e);
            return Result.error("上传失败，请稍后重试");
        }
    }

    /**
     * 上传视频到 OSS
     * 前端发帖前先调此接口上传视频和封面，拿到 URL 后再调 POST /posts 创建帖子
     */
    @PostMapping("/upload/video")
    public Result uploadVideo(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "cover", required = false) MultipartFile cover,
                              HttpServletRequest request) {
        // 1. 校验登录
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            log.warn("上传视频失败：token 无效或已过期");
            return Result.error("请先登录");
        }

        // 2. 视频文件非空校验
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的视频");
        }

        // 3. 上传视频和封面到OSS，返回 videoUrl 和 coverUrl
        try {
            byte[] videoContent = file.getBytes();
            String videoUrl = ALiYunOSSUtils.upload(videoContent, file.getOriginalFilename());
            log.info("用户 {} 上传视频成功: {}", userId, videoUrl);

            String coverUrl = null;
            if (cover != null && !cover.isEmpty()) {
                byte[] coverContent = cover.getBytes();
                coverUrl = ALiYunOSSUtils.upload(coverContent, cover.getOriginalFilename());
                log.info("用户 {} 上传视频封面成功: {}", userId, coverUrl);
            }

            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("videoUrl", videoUrl);
            result.put("coverUrl", coverUrl);
            return Result.sucess(result);
        } catch (Exception e) {
            log.error("视频上传失败: {}", e.getMessage(), e);
            return Result.error("上传失败，请稍后重试");
        }
    }

    /**
     * 点赞
     * Header: token = JWT
     */
    @PostMapping("/posts/{id}/like")
    public Result like(@PathVariable Long id, HttpServletRequest request) {
        // 1. 从请求头提取 JWT，解析当前用户 ID
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            log.warn("点赞失败：token 无效或已过期");
            return Result.error("请先登录");
        }

        // 2. 执行点赞
        try {
            postService.like(id, userId);
            log.info("用户 {} 点赞帖子 {}", userId, id);
            return Result.sucess();
        } catch (RuntimeException e) {
            log.error("点赞失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("点赞失败: {}", e.getMessage(), e);
            return Result.error("操作失败，请稍后重试");
        }
    }

    /**
     * 取消点赞
     * Header: token = JWT
     */
    @DeleteMapping("/posts/{id}/like")
    public Result unlike(@PathVariable Long id, HttpServletRequest request) {
        // 1. 从请求头提取 JWT，解析当前用户 ID
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            log.warn("取消点赞失败：token 无效或已过期");
            return Result.error("请先登录");
        }

        // 2. 执行取消点赞
        try {
            postService.unlike(id, userId);
            log.info("" +
                    "用户 {} 取消点赞帖子 {}", userId, id);
            return Result.sucess();
        } catch (RuntimeException e) {
            log.error("取消点赞失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("取消点赞失败: {}", e.getMessage(), e);
            return Result.error("操作失败，请稍后重试");
        }
    }

    /**
     * 获取指定用户的帖子列表（公开接口，未传 userId 则查当前用户）
     * 参数: userId(可选), page, pageSize
     */
    @GetMapping("/posts/mine")
    public Result mine(@RequestParam(required = false) Long userId,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "50") int pageSize,
                       HttpServletRequest request) {
        String token = request.getHeader("token");
        Long currentUserId = (token != null && !token.isEmpty()) ? jwtUtils.getUserId(token) : null;

        // 未传 userId 且未登录 → 报错
        Long targetUserId = (userId != null) ? userId : currentUserId;
        if (targetUserId == null) {
            return Result.error("请先登录或指定用户ID");
        }

        log.info("查询用户 {} 的帖子列表", targetUserId);
        PageResult<PostVO> result = postService.getPostsByUserId(targetUserId, page, pageSize);
        return Result.sucess(result);
    }

    /**
     * 获取当前用户的草稿列表
     */
    @GetMapping("/posts/drafts")
    public Result drafts(@RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int pageSize,
                         HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return Result.error("请先登录");
        PageResult<PostVO> result = postService.getDraftsByUserId(userId, page, pageSize);
        return Result.sucess(result);
    }

    /**
     * 更新帖子（发布草稿或修改帖子）
     */
    @PutMapping("/posts/{id}")
    public Result update(@PathVariable Long id, @RequestBody Post p,
                         HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return Result.error("请先登录");
        try {
            Post updated = postService.publishDraft(p, id, userId);
            log.info("用户 {} 更新了帖子 {}", userId, id);
            return Result.sucess(updated);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 2.4 删除帖子
     * Header: token = JWT（仅作者或管理员）
     */
    @DeleteMapping("/posts/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            postService.deletePost(id, userId);
            log.info("用户 {} 删除了帖子 {}", userId, id);
            return Result.sucess();
        } catch (RuntimeException e) {
            log.error("删除帖子失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 2.7 收藏帖子
     * Header: token = JWT
     */
    @PostMapping("/posts/{id}/collect")
    public Result collect(@PathVariable Long id, @RequestBody java.util.Map<String, Long> body,
                          HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        Long collectionId = body.get("collectionId");
        if (collectionId == null) {
            return Result.error("请选择收藏夹");
        }
        try {
            postService.collectPost(id, userId, collectionId);
            log.info("用户 {} 收藏了帖子 {} 到收藏夹 {}", userId, id, collectionId);
            return Result.sucess();
        } catch (RuntimeException e) {
            log.error("收藏失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 2.8 取消收藏帖子
     * Header: token = JWT
     */
    @DeleteMapping("/posts/{id}/collect")
    public Result uncollect(@PathVariable Long id,
                            HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            postService.uncollectPost(id, userId);
            log.info("用户 {} 取消收藏帖子 {}", userId, id);
            return Result.sucess();
        } catch (RuntimeException e) {
            log.error("取消收藏失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 2.1 分页获取关注流帖子
     * Header: token = JWT
     */
    @GetMapping("/posts/follow")
    public Result followFeed(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int pageSize,
                              HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        PageResult<PostVO> result = postService.getFollowPostList(userId, page, pageSize);
        return Result.sucess(result);
    }

    /**
     * 2.2 获取关注流新内容数量
     * Header: token = JWT
     */
    @GetMapping("/posts/follow/new-count")
    public Result followNewCount(@RequestParam(required = false) String lastTime,
                                  HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        // 不传则默认取当前时间前 1 天
        if (lastTime == null || lastTime.isEmpty()) {
            lastTime = java.time.LocalDateTime.now().minusDays(1)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        long count = postService.getNewFollowPostCount(userId, lastTime);
        return Result.sucess(count);
    }
}