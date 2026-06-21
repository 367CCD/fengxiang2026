package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.Result;
import com.txq.xiaohongshu.pojo.User;
import com.txq.xiaohongshu.pojo.UserProfileVO;
import com.txq.xiaohongshu.pojo.UserVO;
import com.txq.xiaohongshu.service.UserService;
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
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 1.4 获取当前登录用户信息
     */
//    @GetMapping("/")
//    public Result getCurrentUser(HttpServletRequest request) {
//        String token = request.getHeader("token");
//        Long userId = jwtUtils.getUserId(token);
//        if (userId == null) {
//            return Result.error("请先登录");
//        }
//        User user = userService.selectById(userId);
//        if (user == null) {
//            return Result.error("用户不存在");
//        }
//        // 清除敏感字段
//        user.setPasswordHash(null);
//        return Result.sucess(user);
//    }

    /**
     * 1.4 获取指定用户信息
     * @param userId 目标用户ID（可选，不传则查当前登录用户）
     */
    @GetMapping("/profile")
    public Result getUser(@RequestParam(required = false) Long userId,
                          HttpServletRequest request) {
        String token = request.getHeader("token");
        Long currentUserId = jwtUtils.getUserId(token);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }
        // 未指定 userId 则查询当前用户自己
        Long targetUserId = (userId != null) ? userId : currentUserId;
        UserProfileVO profile = userService.getProfile(targetUserId, currentUserId);
        if (profile == null || profile.getStatus() == 0) {
            return Result.error("用户不存在");
        }
        return Result.sucess(profile);
    }

    /**
     * 1.5 更新用户资料
     */
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody User u, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setAvatarUrl(u.getAvatarUrl());
        updateUser.setBio(u.getBio());
        updateUser.setGender(u.getGender());
        updateUser.setLocation(u.getLocation());
        updateUser.setBirthday(u.getBirthday());

        int rows = userService.updateById(updateUser);
        if (rows != 1) {
            log.error("更新用户资料失败, userId={}", userId);
            return Result.error("更新失败，请稍后重试");
        }
        log.info("用户 {} 更新了个人资料", userId);
        return Result.sucess();
    }

    /**
     * 1.6 关注用户
     */
    @PostMapping("/{userId}/follow")
    public Result follow(@PathVariable Long userId, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long currentUserId = jwtUtils.getUserId(token);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }
        try {
            userService.follow(currentUserId, userId);
            log.info("用户 {} 关注了用户 {}", currentUserId, userId);
            return Result.sucess();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 1.7 取消关注
     */
    @DeleteMapping("/{userId}/follow")
    public Result unfollow(@PathVariable Long userId, HttpServletRequest request) {
        String token = request.getHeader("token");
        Long currentUserId = jwtUtils.getUserId(token);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }
        userService.unfollow(currentUserId, userId);
        log.info("用户 {} 取消关注了用户 {}", currentUserId, userId);
        return Result.sucess();
    }

    /**
     * 1.3 获取当前用户关注 ID 集合
     */
    @GetMapping("/follow/ids")
    public Result getFollowedIds(HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        List<Long> ids = userService.getFollowedIds(userId);
        return Result.sucess(ids);
    }

    /**
     * 1.8 关注者列表（粉丝）
     * 公开接口，登录后返回关注状态、互关状态
     */
    @GetMapping("/{userId}/followers")
    public Result followers(@PathVariable Long userId,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int pageSize,
                            HttpServletRequest request) {
        String token = request.getHeader("token");
        Long currentUserId = (token != null && !token.isEmpty()) ? jwtUtils.getUserId(token) : null;
        PageResult<UserVO> result = userService.getFollowers(userId, currentUserId, page, pageSize);
        return Result.sucess(result);
    }

    /**
     * 1.9 关注列表
     * 公开接口，登录后返回关注状态、互关状态
     */
    @GetMapping("/{userId}/following")
    public Result following(@PathVariable Long userId,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int pageSize,
                            HttpServletRequest request) {
        String token = request.getHeader("token");
        Long currentUserId = (token != null && !token.isEmpty()) ? jwtUtils.getUserId(token) : null;
        PageResult<UserVO> result = userService.getFollowing(userId, currentUserId, page, pageSize);
        return Result.sucess(result);
    }
}