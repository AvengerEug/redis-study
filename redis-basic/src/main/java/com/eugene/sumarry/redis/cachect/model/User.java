package com.eugene.sumarry.redis.cachect.model;

public class User {

    private Long userId;

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public User(Long userId) {
        this.userId = userId;
    }
}
