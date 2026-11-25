package com.example.healthexam.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/** 分页返回结构：总数 + 页码 + 每页条数 + 列表 */
@Data
public class PageResult<T> {
    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> p) {
        PageResult<T> r = new PageResult<>();
        r.total = p.getTotal();
        r.pageNum = p.getCurrent();
        r.pageSize = p.getSize();
        r.records = p.getRecords();
        return r;
    }

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> records) {
        PageResult<T> r = new PageResult<>();
        r.total = total;
        r.pageNum = pageNum;
        r.pageSize = pageSize;
        r.records = records;
        return r;
    }
}
