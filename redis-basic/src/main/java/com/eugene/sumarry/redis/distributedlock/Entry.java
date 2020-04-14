package com.eugene.sumarry.redis.distributedlock;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.PhantomReference;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * 模拟场景: 100个线程去抢数量为50的商品
 *
 *
 * 1. 遇到的第一个问题: 使用多线程操作同一个jedis对象，报了很多奇怪的错:
 *   redis.clients.jedis.exceptions.JedisConnectionException: Unexpected end of stream.
 *   ERR Protocol error: invalid multibulk length
 *   read time out
 *   最终解决方案是每一个线程操作redis时，都从redisPool中去获取jedis对象。
 *   并使用ThreadLocal的特性保证每个jedis都是从redisPool中去获取jedis对象。
 */
public class Entry {

    private static final Logger logger = LoggerFactory.getLogger(Entry.class);

    private static final int THREAD_COUNT = 100;

    private static int GOODS_COUNT = 90;

    private static final CountDownLatch countDownLatch = new CountDownLatch(THREAD_COUNT);

    // 修改策略使用不同的加锁策略
    // 可选参数new RedisLock.LockStrategyOne()
    // 可选参数new RedisLock.LockStrategyTwo()
    // 可选参数new RedisLock.LockStrategyThree()
    private static RedisLock redisLock = new RedisLock(new RedisLock.LockStrategyTwo());

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                UUID uuid = UUID.randomUUID();
                // 自旋，获取锁。
                for (; ;) {
                    if (GOODS_COUNT <= 0) {
                        logger.info("货源不足, 当前线程{} 抢占资源失败",Thread.currentThread().getName());
                        break;
                    }

                    if (redisLock.tryLock(uuid.toString())) {
                        //logger.info("线程{} 拿到了锁", Thread.currentThread().getName());

                        // 最好再校验一下 > 0 时才进行消耗资源，稳妥一点。
                        // 如果秒杀情况下，超卖的话，咱们作为程序员，你懂的
                        // 其实是因为不加这段校验的话，还是有可能出现超卖的情况。。。。
                        // 还没定位到为什么会这样.. TODO
                        if (GOODS_COUNT > 0) {
                            logger.info("剩余数量{}", --GOODS_COUNT);
                        }

                        if (redisLock.unlock(uuid.toString())) {
                            // 释放锁后，当前线程就抢占完毕了，于是直接跳出，执行下面的countDownLatch.countDown();
                            //logger.info("线程{}释放了锁", Thread.currentThread().getName());
                            break;
                        }
                    }
                }

                countDownLatch.countDown();
            }, "thread-" + i).start();
        }

        countDownLatch.await();

        logger.warn("商品抢占结束，商品库存剩余: {}", GOODS_COUNT);
    }
}
