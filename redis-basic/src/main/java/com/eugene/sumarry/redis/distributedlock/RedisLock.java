package com.eugene.sumarry.redis.distributedlock;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Collections;

public class RedisLock {

    private static final String DISTRIBUTED_LOCK_NAME = "redisLock";

     static JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

     static {
         jedisPoolConfig.setMaxTotal(-1);
         jedisPoolConfig.setMaxWaitMillis(-1);
         // 设置连接最大等待时间
         // jedisPoolConfig.setMaxWaitMillis(500);
     }

    private static final JedisPool jedisPool = new JedisPool(jedisPoolConfig,"127.0.0.1", 6379);

    private LockStrategy lockStrategy;

    private final static ThreadLocal<Jedis> threadLocal = new ThreadLocal<Jedis>() {
        @Override
        protected Jedis initialValue() {
            return jedisPool.getResource();
        }
    };

    public RedisLock(LockStrategy lockStrategy) {
        this.lockStrategy = lockStrategy;
    }

    public boolean tryLock(String value) {
        return lockStrategy.tryLock(value);
    }

    public boolean unlock(String value) {
        return lockStrategy.unlock(value);
    }

    /**
     * 加锁策略类
     */
    static abstract class LockStrategy {
        public abstract boolean tryLock(String value);
        public abstract boolean unlock(String value);
    }

    /**
     * 加锁/解锁策略1
     *
     * 加锁/解锁策略1 总结:
     * 1. 加锁过程没有问题，利用的是redis中的原子性
     * 2. 解锁过程中有问题:
     *    1. 判断key和删除key这是两步操作，不具备原子性操作。
     *       有可能在删除锁的一瞬间，
     *       所以我们得想一下将判断存在 + 删除key的操作变成
     *       原子性的。于是，可以使用lua脚本
     *    2. 解锁的过程中，若判断key存在的过程中应用程序挂掉了,
     *       此时这把锁将变成死锁
     *
     * 针对出现的问题，在LockStrategyTwo中进行了修改, 使用lua脚本接触这两个问题
     */
    static class LockStrategyOne extends LockStrategy {
        /**
         * 加锁策略1:
         *   使用setnx
         *
         * 因为redis的单线程特性以及setnx的原子性操作，可以实现分布式锁
         */
        public boolean tryLock(String value) {
            return 1 == threadLocal.get().setnx(DISTRIBUTED_LOCK_NAME, value);
        }

        /**
         * 解锁策略1:
         *   执行完逻辑后释放锁
         */
        public boolean unlock(String value) {
            if (threadLocal.get().exists(DISTRIBUTED_LOCK_NAME)) {
                return 1 == threadLocal.get().del(DISTRIBUTED_LOCK_NAME);
            }

            return false;
        }
    }


    /**
     * 加锁解锁策略2: 解锁部分使用lua脚本确保删除时为原子性
     * 以及防止在校验key是否存在时正准备删除锁的过程中应用程
     * 序挂掉导致变成死锁
     *
     * 此策略也可能出现的问题:
     *   1. 若应用程序还没执行到释放锁的过程
     *      (即外部调用unlock)中挂掉了，此时也会变成死锁
     *      所以我们需要将锁设置过期时间
     *   2. 在设置锁过期时间的过程中，redis有一个叫expire指令，
     *      但是，将setnx和expire一起使用，它是两个指令也不具有
     *      原子性。所以我们需要一个setnx和expire一起操作具有原子
     *      性功能。 由LockStrategyTwo的启发，可能你也会想到用
     *      lua脚本。但是很巧的是，redis中有一个指令能完成
     *      setnx 和 expire的事情，且具有原子性。
     *      具体可查看LockStrategyThree
     */
    static class LockStrategyTwo extends LockStrategy {

        private static String LUA_SCRIPT;

        static {
            //
            LUA_SCRIPT =
                    "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then " +
                            "return redis.call('del', KEYS[1]) " +
                    "else " +
                            "return 0 end";
        }

        /**
         * 加锁策略2:
         *   使用setnx
         *
         * 因为redis的单线程特性以及setnx的原子性操作，可以实现分布式锁
         */
        public boolean tryLock(String value) {
            return 1 == threadLocal.get().setnx(DISTRIBUTED_LOCK_NAME, value);
        }

        /**
         * 解锁策略2:
         *   使用lua脚本进行解锁
         */
        public boolean unlock(String value) {
            return "1".equals(threadLocal.get().eval(LUA_SCRIPT, Collections.singletonList(DISTRIBUTED_LOCK_NAME), Collections.singletonList(value)).toString());
        }
    }


    /**
     * 加锁/解锁策略3
     *
     * 可能出现的问题:
     *   因为在解锁的过程中可能会出现一个问题:
     *   在集群部署情况下，第一个实例A拿到锁，因为业务逻辑执行的比较长，
     *   导致锁过期释放掉了。此时，另外一个实例B拿着相同的key来加锁，
     *   发现加成功了。然后开始执行业务逻辑。但是呢，此时实例A执行完了，
     *   我去校验时，发现锁也存在，于是打算去删除锁，而此时呢，实例C也
     *   进来加锁了，发现锁没有，它也加了锁。如此循环下去，最终会乱套
     *
     *   所以在解锁(删除锁)的过程中还要判断value是不是跟我以前设置的
     *   一样，如果不是一样则不释放锁。就是为了防止他人把自己的锁给
     *   删除了。当然，这个在lua脚本中也体现出来了，
     *   所以在调用锁的时候，需要传递一个唯一的值进来
     *
     *   即: lock.truLock(UUID.randomUUID())
     */
    static class LockStrategyThree extends LockStrategy {

        private static String LUA_SCRIPT;

        static {
            //
            LUA_SCRIPT =
                    "if redis.call('get', KEYS[1]) == ARGV[1] " +
                            "then " +
                            "return redis.call('del', KEYS[1]) " +
                            "else " +
                            "return 0 end";
        }

        /**
         * 加锁策略3:
         *   使用set指令来完成setnx 和 expire的操作。
         *   set key value nx ex 5000
         *   相当于setnx(key, value) + expire(5000)
         *   5s后过期
         */
        public boolean tryLock(String value) {
            return "OK".equals(threadLocal.get().set(
                    DISTRIBUTED_LOCK_NAME,
                    value,
                    "nx",
                    "ex",
                    10000));
        }

        /**
         * 解锁策略3:
         *   使用lua脚本进行解锁
         */
        public boolean unlock(String value) {
            return "1".equals(
                    threadLocal.get().eval(
                        LUA_SCRIPT, Collections.singletonList(DISTRIBUTED_LOCK_NAME),
                        Collections.singletonList(value)
                    ).toString());
        }
    }


}
