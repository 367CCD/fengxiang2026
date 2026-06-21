package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.pojo.Result;
import com.txq.xiaohongshu.pojo.User;
import com.txq.xiaohongshu.service.UserService;
import com.txq.xiaohongshu.utils.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class Login {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public Result login(@RequestBody User u, HttpSession session, HttpServletResponse response) {
        // 1. 验证码校验
        String sessionCode = (String) session.getAttribute("code");
        String clientCaptcha = u.getCaptcha();
        if (sessionCode == null) {
            log.info("验证码已过期或未获取");
            response.setStatus(400);
            return Result.error("验证码已过期，请刷新后重试");
        }
        if (clientCaptcha == null || clientCaptcha.trim().isEmpty()) {
            log.info("验证码为空");
            response.setStatus(400);
            return Result.error("请输入验证码");
        }
        if (!sessionCode.equalsIgnoreCase(clientCaptcha.trim())) {
            log.info("验证码校验失败: session={}, client={}", sessionCode, clientCaptcha);
            response.setStatus(400);
            return Result.error("验证码错误");
        }
        // 验证码使用后立即清除，防止重复使用
        session.removeAttribute("code");

        // 2. 用户名密码校验
        String username = u.getUsername();
        String password = u.getPasswordHash();
        User user = userService.selectByUsername(username);
        log.info("用户申请登录:{}", username);
        if (user == null || !password.equals(user.getPasswordHash())) {
            log.info("尝试登录{}账号失败", username);
            response.setStatus(401);
            return Result.error("用户名或密码错误!");
        }

        // 3. 账号状态校验
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.info("{} 账号已被禁用，无法登录", username);
            response.setStatus(403);
            return Result.error("账号已被禁用，请联系管理员");
        }

        // 4. 登录类型校验
        Integer loginType = u.getLoginType();
        if (loginType != null) {
            int userRole = user.getRole() != null ? user.getRole() : 0;
            if (!loginType.equals(userRole)) {
                log.info("{} 登录类型不匹配: loginType={}, userRole={}", username, loginType, userRole);
                response.setStatus(403);
                return Result.error(loginType == 1 ? "该账号不是管理员账号" : "该账号是管理员账号，请选择管理端登录");
            }
        }

        // 5. 生成 JWT
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        log.info("{}登录成功, role={}", username, user.getRole());
        log.info("生成token {}",token);

        // 返回用户基本信息 + token
        java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", user.getRole());
        userInfo.put("avatarUrl", user.getAvatarUrl());
        return Result.sucess(new Object[]{userInfo, token});
    }
}
