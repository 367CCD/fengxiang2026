package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.util.List;

/**
 * 禁言请求参数
 */
@Data
public class MuteRequest {
    /** 要禁言的用户ID列表 */
    private List<Long> userIds;
    /** 禁言原因 */
    private String reason;
    /** 禁言天数: null=永久, 1/3/7/30 */
    private Integer duration;
}
