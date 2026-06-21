package com.txq.xiaohongshu.service;

import com.txq.xiaohongshu.pojo.SearchHistory;

import java.util.List;

public interface SearchHistoryService {

    /** 最大保留条数 */
    int MAX_HISTORY = 50;

    /** 关键词最大长度 */
    int MAX_KEYWORD_LENGTH = 100;

    /**
     * 记录搜索历史（Upsert + 上限控制）
     * @param userId  用户ID
     * @param keyword 搜索关键词（原始输入，内部会做清洗截断）
     */
    void record(Long userId, String keyword);

    /** 查询用户搜索历史 */
    List<SearchHistory> getHistory(Long userId);

    /** 清空用户搜索历史 */
    void clearHistory(Long userId);
}
