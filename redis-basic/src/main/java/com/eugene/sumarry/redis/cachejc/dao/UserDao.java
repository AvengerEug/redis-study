package com.eugene.sumarry.redis.cachejc.dao;

import com.eugene.sumarry.redis.cachejc.model.User;

public class UserDao extends DaoTemplate<User> {

    @Override
    protected User getById(Long id) {
        // 1. 先从缓存中获取

        // 2. 再从db中获取

        // 这里不模拟了，直接返回用户对象

        return new User(id);
    }

    @Override
    public void init() {
        // 初始化UserDao中的布隆过滤器
        for (Long i = 0L; i < 100L; i++) {
            bloomFilter.put(i);
        }
    }
}
