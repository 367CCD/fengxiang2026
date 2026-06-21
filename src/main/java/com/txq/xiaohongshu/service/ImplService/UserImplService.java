package com.txq.xiaohongshu.service.ImplService;

import cn.hutool.crypto.digest.BCrypt;
import com.txq.xiaohongshu.mapper.UserMapper;
import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.User;
import com.txq.xiaohongshu.pojo.UserProfileVO;
import com.txq.xiaohongshu.pojo.UserVO;
import com.txq.xiaohongshu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserImplService implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User selectByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public User selectByPhone(String phone) {
        return userMapper.selectByPhone(phone);
    }

    @Override
    public User selectByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    @Override
    public User selectById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public int insert(User user) {
        return userMapper.insert(user);
    }

    @Override
    public int updateById(User user) {
        return userMapper.updateById(user);
    }

    @Override
    public UserProfileVO getProfile(Long id, Long currentUserId) {
        return userMapper.selectProfileById(id, currentUserId);
    }

    /**
     * 使用 BCrypt 对密码进行哈希
     */
    @Override
    public String encodePassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * 校验明文密码是否匹配 BCrypt 哈希
     */
    @Override
    public boolean checkPassword(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }

    // ── 关注相关 ──

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new RuntimeException("不能关注自己");
        }
        // 检查目标用户是否存在
        User target = userMapper.selectById(followeeId);
        if (target == null) {
            throw new RuntimeException("用户不存在");
        }
        try {
            userMapper.insertFollow(followerId, followeeId);
        } catch (DuplicateKeyException ignored) {
            // 已关注，幂等
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long followerId, Long followeeId) {
        userMapper.deleteFollow(followerId, followeeId);
    }

    @Override
    public boolean isFollowed(Long followerId, Long followeeId) {
        return userMapper.checkFollowed(followerId, followeeId) > 0;
    }

    @Override
    public List<Long> getFollowedIds(Long userId) {
        return userMapper.selectFollowedIds(userId);
    }

    @Override
    public PageResult<UserVO> getFollowers(Long userId, Long currentUserId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<UserVO> list = userMapper.selectFollowers(userId, currentUserId, offset, pageSize);
        long total = userMapper.countFollowers(userId);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public PageResult<UserVO> getFollowing(Long userId, Long currentUserId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<UserVO> list = userMapper.selectFollowing(userId, currentUserId, offset, pageSize);
        long total = userMapper.countFollowing(userId);
        return PageResult.of(list, total, page, pageSize);
    }

    @Override
    public boolean isAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getRole() != null && user.getRole() == 1;
    }
}
