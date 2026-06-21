package com.txq.xiaohongshu.service;

import com.txq.xiaohongshu.pojo.PageResult;
import com.txq.xiaohongshu.pojo.User;
import com.txq.xiaohongshu.pojo.UserProfileVO;
import com.txq.xiaohongshu.pojo.UserVO;

import java.util.List;

public interface UserService {

    User selectByUsername(String username);

    User selectByPhone(String phone);

    User selectByEmail(String email);

    User selectById(Long id);

    /** 查询用户资料（含帖子数/粉丝数/关注数/是否已关注） */
    UserProfileVO getProfile(Long id, Long currentUserId);

    int insert(User user);

    int updateById(User user);

    /** 使用 BCrypt 对明文密码进行哈希 */
    String encodePassword(String rawPassword);

    /** 校验明文密码是否匹配已存储的 BCrypt 哈希 */
    boolean checkPassword(String rawPassword, String hashedPassword);

    // ── 关注相关 ──

    /** 关注用户 */
    void follow(Long followerId, Long followeeId);

    /** 取消关注 */
    void unfollow(Long followerId, Long followeeId);

    /** 检查是否已关注 */
    boolean isFollowed(Long followerId, Long followeeId);

    /** 获取当前用户所有已关注的用户 ID 列表 */
    List<Long> getFollowedIds(Long userId);

    /** 粉丝列表 */
    PageResult<UserVO> getFollowers(Long userId, Long currentUserId, int page, int pageSize);

    /** 关注列表 */
    PageResult<UserVO> getFollowing(Long userId, Long currentUserId, int page, int pageSize);

    /** 检查用户是否为管理员 */
    boolean isAdmin(Long userId);
}
