package com.txq.xiaohongshu.pojo;

import lombok.Data;

import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> {
    /** 当前页数据列表 */
    private List<T> records;
    /** 总条数 */
    private long total;
    /** 总页数 */
    private long pages;
    /** 当前页码 */
    private int current;
    /** 每页条数 */
    private int size;

    public static <T> PageResult<T> of(List<T> records, long total, int current, int size) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(records);
        r.setTotal(total);
        r.setCurrent(current);
        r.setSize(size);
        r.setPages(size > 0 ? (total + size - 1) / size : 0);
        return r;
    }
}
