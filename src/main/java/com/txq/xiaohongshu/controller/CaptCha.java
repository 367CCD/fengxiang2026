package com.txq.xiaohongshu.controller;

import cn.hutool.captcha.AbstractCaptcha;
import com.txq.xiaohongshu.utils.CaptchaUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
public class CaptCha {

    @GetMapping("/captcha")
    public void captcha(HttpSession session, HttpServletResponse response) throws IOException {
        log.info("验证码生成");
        // 1. 设置 session 过期时间 30 分钟
        session.setMaxInactiveInterval(30 * 60);

        // 2. 生成验证码
        AbstractCaptcha captcha = CaptchaUtils.createCaptcha();

        // 3. 验证码文本存 session，登录时校验
        session.setAttribute("code", CaptchaUtils.getCode(captcha));

        // 4. 设置响应头，告诉浏览器这是一张 PNG 图片
        response.setContentType("image/png");

        // 5. 把验证码图片流写入 response
        CaptchaUtils.write(captcha, response.getOutputStream());
    }
}