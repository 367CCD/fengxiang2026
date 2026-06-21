package com.txq.xiaohongshu.mapper;

import com.txq.xiaohongshu.pojo.UserMuteHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMuteHistoryMapper {

    int insert(UserMuteHistory record);

    List<UserMuteHistory> selectByUserId(@Param("userId") Long userId,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    long countByUserId(@Param("userId") Long userId);
}
