package com.txq.xiaohongshu.controller;

import com.txq.xiaohongshu.pojo.Result;
import com.txq.xiaohongshu.pojo.User;
import com.txq.xiaohongshu.service.UserService;
import com.txq.xiaohongshu.utils.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户注册控制器
 *
 * <p>前端传入 username、password、captcha（必填），phone、email（选填）。
 * 后端只负责：验证码校验 → 唯一性检查 → 密码哈希 → 入库 → 签发 JWT</p>
 */
@Slf4j
@RestController
public class Register {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${admin.register-code}")
    private String adminRegisterCode;

    @PostMapping("/register")
    public Result register(@RequestBody User u,
                           HttpSession session,
                           HttpServletResponse response) {

        // ────────────────── 1. 验证码校验 ──────────────────
        String sessionCode = (String) session.getAttribute("code");
        String clientCaptcha = u.getCaptcha();

        if (sessionCode == null) {
            log.info("注册验证码已过期或未获取");
            response.setStatus(400);
            return Result.error("验证码已过期，请刷新后重试");
        }
        if (clientCaptcha == null || clientCaptcha.trim().isEmpty()) {
            log.info("注册验证码为空");
            response.setStatus(400);
            return Result.error("请输入验证码");
        }
        if (!sessionCode.equalsIgnoreCase(clientCaptcha.trim())) {
            log.info("注册验证码校验失败: session={}, client={}", sessionCode, clientCaptcha);
            response.setStatus(400);
            return Result.error("验证码错误");
        }
        session.removeAttribute("code");

        // ────────────────── 2. 必填字段检查 ──────────────────
        String username = u.getUsername();
        String password = u.getPasswordHash();
        String phone    = u.getPhone();
        String email    = u.getEmail();

        if (username == null || username.trim().isEmpty()) {
            response.setStatus(400);
            return Result.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            response.setStatus(400);
            return Result.error("密码不能为空");
        }
        username = username.trim();
        log.info("用户:{}申请账号",u.getUsername());
        // 选填字段 trim
        if (phone != null) {
            phone = phone.trim();
            if (phone.isEmpty()) phone = null;
        }
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) email = null;
        }

        // ────────────────── 3. 唯一性检查 ──────────────────
        if (userService.selectByUsername(username) != null) {
            log.info("注册失败，用户名已存在: {}", username);
            response.setStatus(409);
            return Result.error("用户名已被注册");
        }
        if (phone != null && userService.selectByPhone(phone) != null) {
            log.info("注册失败，手机号已存在: {}", phone);
            response.setStatus(409);
            return Result.error("手机号已被注册");
        }
        if (email != null && userService.selectByEmail(email) != null) {
            log.info("注册失败，邮箱已存在: {}", email);
            response.setStatus(409);
            return Result.error("邮箱已被注册");
        }

//        // ────────────────── 4. 密码哈希 ──────────────────
//        String hashedPassword = userService.encodePassword(password);

        // ────────────────── 4.5 管理端注册校验 ──────────────────
        Integer requestRole = u.getRole();
        int finalRole = 0;  // 默认普通用户

        if (requestRole != null && requestRole == 1) {
            // 要注册管理端账号，必须提供正确的注册码
            String requestAdminCode = u.getAdminCode();
            if (requestAdminCode == null || requestAdminCode.trim().isEmpty()) {
                log.warn("管理端注册请求缺少注册码: username={}", username);
                response.setStatus(400);
                return Result.error("管理端注册码不能为空");
            }
            if (!requestAdminCode.trim().equals(adminRegisterCode)) {
                log.warn("管理端注册码校验失败: username={}, adminCode={}", username, requestAdminCode);
                response.setStatus(400);
                return Result.error("管理端注册码错误");
            }
            finalRole = 1;
            log.info("管理端注册码校验通过: username={}", username);
        }

        // ────────────────── 5. 入库 ──────────────────
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPhone(phone);
        newUser.setEmail(email);
        newUser.setPasswordHash(password);
        newUser.setStatus(1);
        newUser.setRole(finalRole);
        newUser.setGender(0);
        newUser.setIsMuted(0);

        int rows = userService.insert(newUser);
        if (rows != 1) {
            log.error("注册插入用户失败, username={}", username);
            response.setStatus(500);
            return Result.error("注册失败，请稍后重试");
        }
        log.info("新用户注册成功: id={}, username={}", newUser.getId(), username);

        // ────────────────── 6. 签发 JWT（注册即登录） ──────────────────
        String token = jwtUtils.generateToken(newUser.getId(), newUser.getUsername());

        // 返回用户基本信息 + token
        java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
        userInfo.put("id", newUser.getId());
        userInfo.put("username", newUser.getUsername());
        userInfo.put("role", newUser.getRole());
        userInfo.put("avatarUrl", newUser.getAvatarUrl());
        return Result.sucess(new Object[]{userInfo, token});
    }
}