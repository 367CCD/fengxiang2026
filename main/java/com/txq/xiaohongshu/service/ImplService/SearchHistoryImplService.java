package com.txq.xiaohongshu.service.ImplService;

import com.txq.xiaohongshu.mapper.SearchHistoryMapper;
import com.txq.xiaohongshu.pojo.SearchHistory;
import com.txq.xiaohongshu.service.SearchHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SearchHistoryImplService implements SearchHistoryService {

    @Autowired
    private SearchHistoryMapper searchHistoryMapper;

    @Override
    public void record(Long userId, String keyword) {
        // ── 1. 前置校验与清洗 ──
        if (userId == null || keyword == null) {
            return;
        }
        String cleaned = keyword.trim();
        if (cleaned.isEmpty()) {
            return;
        }
        // 超长截断
        if (cleaned.length() > MAX_KEYWORD_LENGTH) {
            cleaned = cleaned.substring(0, MAX_KEYWORD_LENGTH);
        }

        // ── 2. Upsert 原子写入 ──
        searchHistoryMapper.upsert(userId, cleaned);

        // ── 3. 上限控制：超出阈值删除最旧记录 ──
        searchHistoryMapper.deleteOldestByUserId(userId, MAX_HISTORY);
    }

    @Override
    public List<SearchHistory> getHistory(Long userId) {
        return searchHistoryMapper.selectByUserId(userId, MAX_HISTORY);
    }

    @Override
    public void clearHistory(Long userId) {
        searchHistoryMapper.deleteByUserId(userId);
    }
}
