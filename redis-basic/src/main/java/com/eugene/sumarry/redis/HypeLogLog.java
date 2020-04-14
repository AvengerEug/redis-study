package com.eugene.sumarry.redis;

/**
 * 基数统计算法:
 *  比如我们统计下某个key下有多少种不同的值
 */
public class HypeLogLog extends RedisBase {


    /**
     * 统计一个集合中有多少种不同的值，可以使用HypeLogLog
     * 它的本质是字符串
     */
    public static void countDiffNumbers() {
        jedis.pfadd("diffNumbers","1", "3", "1", "eug", "avg", "eug");
        System.out.println(jedis.pfcount("diffNumbers"));
    }

    /**
     * 合并两个结合
     */
    public static void merge() {
        jedis.pfadd("testMerge", "1", "2", "3");

        System.out.println(jedis.pfmerge("diffNumbers", "testMerge"));
        System.out.println(jedis.get("diffNumbers"));
        System.out.println(jedis.get("testMerge"));
    }

    public static void main(String[] args) {
        countDiffNumbers();
        merge();
    }
}
