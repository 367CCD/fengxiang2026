package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.mapper.TagMapper;
import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.PostVO;
import com.txq.xiaohongshu.pojo.Result;
import com.txq.xiaohongshu.pojo.Tag;
import com.txq.xiaohongshu.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class Tags {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private PostService postService;

    @GetMapping("/tags")
    public Result tags() {
        List<Tag> tags = tagMapper.selectAll();
        log.info("获取标签列表, 共 {} 条", tags.size());
        Map<String, Object> data = new HashMap<>();
        data.put("list", tags);
        return Result.sucess(data);
    }

    /**
     * 4.2 按标签浏览帖子
     */
    @GetMapping("/tags/{tagId}/posts")
    public Result postsByTag(@PathVariable Long tagId,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<PostVO> result = postService.getPostList(page, pageSize, "latest", tagId, null);
        return Result.sucess(result);
    }
}