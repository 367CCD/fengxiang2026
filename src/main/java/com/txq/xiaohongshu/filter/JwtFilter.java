package com.txq.xiaohongshu.filter;

import com.txq.xiaohongshu.mapper.UserMapper;
import com.txq.xiaohongshu.pojo.User;
import com.txq.xiaohongshu.utils.JwtUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
@Slf4j
@Component
public class JwtFilter implements Filter {

    private static final List<String> WHITE_LIST = List.of(
        "/login", "/register", "/captcha", "/tags"
    );

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        // 1. 白名单放行（注册、登录、公开接口）
        if (WHITE_LIST.contains(req.getRequestURI())) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        // GET 公开访问: /posts, /posts/{id}, /tags/{tagId}/posts
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            String uri = req.getRequestURI();
            if ("/posts".equals(uri)
                    || uri.matches("^/posts/\\d+$")
                    || uri.matches("^/tags/\\d+/posts$")
                    || uri.matches("^/posts/\\d+/comments.*$")) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }
        // 2. 提取令牌
        String token = req.getHeader("token");
        if (token == null || token.isEmpty()) {
            res.setStatus(401);
            return;
        }
        // 3. 校验令牌
        if (!jwtUtils.validate(token)) {
            res.setStatus(401);
            return;
        }
        // 4. 账号禁用检查
        Long userId = jwtUtils.getUserId(token);
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getStatus() != null && user.getStatus() == 0) {
                res.setStatus(403);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"code\":0,\"msg\":\"账号已被禁用，请联系管理员\"}");
                return;
            }
        }
        // 5. 校验通过，放行
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
