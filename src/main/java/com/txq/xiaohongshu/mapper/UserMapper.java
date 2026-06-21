package com.txq.xiaohongshu.mapper;

import com.txq.xiaohongshu.pojo.User;
import com.txq.xiaohongshu.pojo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User selectById(@Param("id") Long id);

    User selectByUsername(@Param("username") String username);

    User selectByEmail(@Param("email") String email);

    User selectByPhone(@Param("phone") String phone);

    int insert(User user);

    int updateById(User user);

    int deleteById(@Param("id") Long id);

    List<User> selectList(@Param("keyword") String keyword,
                          @Param("status") Integer status,
                          @Param("offset") Integer offset,
                          @Param("limit") Integer limit);

    int count(@Param("keyword") String keyword,
              @Param("status") Integer status);

    // ── 关注相关 ──

    /** 关注用户 */
    int insertFollow(@Param("followerId") Long followerId,
                     @Param("followeeId") Long followeeId);

    /** 取消关注 */
    int deleteFollow(@Param("followerId") Long followerId,
                     @Param("followeeId") Long followeeId);

    /** 检查是否已关注 */
    int checkFollowed(@Param("followerId") Long followerId,
                      @Param("followeeId") Long followeeId);

    /** 获取当前用户所有已关注的用户 ID 列表 */
    List<Long> selectFollowedIds(@Param("userId") Long userId);

    /** 粉丝列表（含关注状态、互关状态） */
    List<UserVO> selectFollowers(@Param("userId") Long userId,
                                  @Param("currentUserId") Long currentUserId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /** 粉丝总数 */
    long countFollowers(@Param("userId") Long userId);

    /** 关注列表（含关注状态、互关状态） */
    List<UserVO> selectFollowing(@Param("userId") Long userId,
                                  @Param("currentUserId") Long currentUserId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /** 关注总数 */
    long countFollowing(@Param("userId") Long userId);

    /** 查询用户资料（含帖子数/粉丝数/关注数/是否已关注） */
    com.txq.xiaohongshu.pojo.UserProfileVO selectProfileById(@Param("id") Long id,
                                                               @Param("currentUserId") Long currentUserId);

    /** 更新用户状态（用于举报处罚：禁用账号） */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    // ── 管理端：用户列表（高级筛选） ──

    List<User> selectAdminList(@Param("keyword") String keyword,
                               @Param("status") Integer status,
                               @Param("isMuted") Integer isMuted,
                               @Param("role") Integer role,
                               @Param("startTime") String startTime,
                               @Param("endTime") String endTime,
                               @Param("offset") Integer offset,
                               @Param("limit") Integer limit);

    int countAdminList(@Param("keyword") String keyword,
                       @Param("status") Integer status,
                       @Param("isMuted") Integer isMuted,
                       @Param("role") Integer role,
                       @Param("startTime") String startTime,
                       @Param("endTime") String endTime);

    // ── 管理端：批量操作 ──

    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    int batchUpdateMute(@Param("ids") List<Long> ids,
                        @Param("isMuted") Integer isMuted,
                        @Param("mutedReason") String mutedReason,
                        @Param("mutedAt") String mutedAt,
                        @Param("muteEndTime") String muteEndTime);

    int batchClearMute(@Param("ids") List<Long> ids);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /** 清除用户头像（设为 NULL） */
    int resetAvatar(@Param("id") Long id);

    /** 查询禁言已到期但未触发懒解封的用户 */
    List<User> selectMutedExpired();

    /** 根据ID列表批量查询用户 */
    List<User> selectByIds(@Param("ids") List<Long> ids);
}