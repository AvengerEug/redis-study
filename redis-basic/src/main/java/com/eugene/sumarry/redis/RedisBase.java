package com.eugene.sumarry.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisBase {

    protected static final Logger logger = LoggerFactory.getLogger(RedisBase.class);

    public static Jedis jedis;

    static {

        jedis = new JedisPool("192.168.111.145", 6379).getResource();
    }
}
