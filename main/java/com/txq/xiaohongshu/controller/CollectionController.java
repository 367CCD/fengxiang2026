package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.pojo.Collection;
import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.PostVO;
import com.txq.xiaohongshu.pojo.Result;
import com.txq.xiaohongshu.service.CollectionService;
import com.txq.xiaohongshu.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/collections")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 5.1 获取收藏夹列表
     * 可传 userId 查询他人公开收藏夹（无需登录）；不传则查当前用户全部收藏夹（需登录）
     */
    @GetMapping
    public Result list(@RequestParam(required = false) Long userId,
                       HttpServletRequest request) {
        String token = request.getHeader("token");
        Long viewerId = (token != null && !token.isEmpty()) ? jwtUtils.getUserId(token) : null;

        // 未指定 userId → 查当前用户自己（需登录）
        if (userId == null) {
            if (viewerId == null) {
                return Result.error("请先登录");
            }
            userId = viewerId;
        }

        List<Collection> collections = collectionService.getCollections(userId, viewerId);
        Map<String, Object> data = new HashMap<>();
        data.put("list", collections);
        return Result.sucess(data);
    }

    /**
     * 5.2 获取收藏夹中的帖子
     */
    @GetMapping("/{id}/posts")
    public Result posts(@PathVariable Long id,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<PostVO> result = collectionService.getCollectionPosts(id, page, pageSize);
        return Result.sucess(result);
    }

    /**
     * 5.3 创建收藏夹
     */
    @PostMapping
    public Result create(@RequestBody Collection collection, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (collection.getName() == null || collection.getName().trim().isEmpty()) {
            return Result.error("收藏夹名称不能为空");
        }
        Collection saved = collectionService.createCollection(collection, userId);
        log.info("用户 {} 创建了收藏夹 {} id={}", userId, saved.getName(), saved.getId());
        return Result.sucess(saved);
    }

    /**
     * 删除收藏夹（仅所有者）
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            collectionService.deleteCollection(id, userId);
            log.info("用户 {} 删除了收藏夹 {}", userId, id);
            return Result.sucess();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}