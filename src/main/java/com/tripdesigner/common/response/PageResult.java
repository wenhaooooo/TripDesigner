package com.tripdesigner.common.response;

import lombok.Getter;

import java.util.List;

/**
 * 通用分页结果封装。
 * 包含当前页码、每页大小、总记录数和数据列表。
 */
@Getter
public class PageResult<T> {
    private final int page;
    private final int size;
    private final long total;
    private final List<T> content;

    public PageResult(int page, int size, long total, List<T> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.content = records;
    }
}