package com.eugene.sumarry.redis;

public class Pipeline extends Base {

    /**
     * 使用pipeline和不使用pipeline操作大量指令的区别
     */
    public static void testNoPipeline() {
        Long start = System.currentTimeMillis();
        logger.info("开始时间: {}", start);
        for (int i = 0; i < 1000; i++) {
            jedis.sadd("testNoPipeline", i + "");
        }

        Long end = System.currentTimeMillis();
        logger.info("结束时间: {}", end);

        // 若连接云端服务器，那么此操作将非常耗时  --- 16712毫秒
        // 因为jedis每次操作都会连接redis服务器
        logger.warn("耗时{}毫秒", (end - start));
    }

    /**
     * 使用pipeline可以大量提升运行时间
     * 但是它是非原子性操作。也就是说后面若有其他的命令要执行的话
     * 它有可能中途会去执行执行其他的命令
     */
    public static void testWithPipeline() {
        Long start = System.currentTimeMillis();
        logger.info("开始时间: {}", start);
        redis.clients.jedis.Pipeline pipeline = jedis.pipelined();
        for (int i = 0; i < 1000; i++) {
            pipeline.sadd("testWithPipeline",i + "");
        }
        // 连接服务器后批处理命令
        pipeline.syncAndReturnAll();
        Long end = System.currentTimeMillis();
        logger.info("结束时间: {}", end);

        // 103毫秒
        logger.warn("耗时{}毫秒", (end - start));
    }

    public static void main(String[] args) {
        //testNoPipeline();

        // 调用此方法不知道为什么，数量变少了
        testWithPipeline();
    }
}
