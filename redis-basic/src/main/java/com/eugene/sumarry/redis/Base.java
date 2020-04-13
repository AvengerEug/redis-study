package com.eugene.sumarry.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

public class Base {

    protected static final Logger logger = LoggerFactory.getLogger(Base.class);

    static Jedis jedis;

    static {
        jedis = new Jedis("192.168.111.145", 6379);
    }
}
