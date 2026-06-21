package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.PostVO;
import com.txq.xiaohongshu.pojo.Result;
import com.txq.xiaohongshu.pojo.SearchHistory;
import com.txq.xiaohongshu.service.PostService;
import com.txq.xiaohongshu.service.SearchHistoryService;
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
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private PostService postService;

    @Autowired
    private SearchHistoryService searchHistoryService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 7.1 搜索帖子（登录用户自动写入搜索历史，游客不记录）
     */
    @GetMapping
    public Result search(@RequestParam String q,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int pageSize,
                         HttpServletRequest request) {
        // 登录用户自动写入搜索历史（失败不影响主流程）
        String token = request.getHeader("token");
        if (token != null && !token.isEmpty()) {
            Long userId = jwtUtils.getUserId(token);
            if (userId != null) {
                try {
                    searchHistoryService.record(userId, q);
                } catch (Exception e) {
                    log.warn("记录搜索历史失败: userId={}, keyword={}", userId, q, e);
                }
            }
        }

        PageResult<PostVO> result = postService.getPostList(page, pageSize, "latest", null, q);
        return Result.sucess(result);
    }

    /**
     * 7.2 获取搜索历史（只能查自己的）
     */
    @GetMapping("/history")
    public Result history(HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        List<SearchHistory> history = searchHistoryService.getHistory(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("list", history);
        return Result.sucess(data);
    }

    /**
     * 7.3 清空搜索历史（只能清自己的）
     */
    @DeleteMapping("/history")
    public Result clearHistory(HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        searchHistoryService.clearHistory(userId);
        return Result.sucess();
    }
}
