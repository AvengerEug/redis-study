package com.eugene.sumarry.redis.cachejc.dao;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public abstract class DaoTemplate<T> {

    private static final Integer TOTAL = 100;

    // 初始化布隆过滤器
    protected BloomFilter<Long> bloomFilter =  BloomFilter.create(Funnels.longFunnel(), TOTAL, 0.01);

    public T execGetById(Long id) {
        if (exist(id)) {
            return getById(id);
        }

        System.err.println("布隆过滤器中认为当前id" + id + "不存在, 那DB中肯定也不存在");

        return null;
    }

    protected abstract T getById(Long id);

    private boolean exist(Long id) {
        return bloomFilter.mightContain(id);
    }

    protected abstract void init();
}
