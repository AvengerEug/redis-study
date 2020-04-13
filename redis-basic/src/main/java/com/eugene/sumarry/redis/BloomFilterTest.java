package com.eugene.sumarry.redis;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * 布隆过滤器测试类
 *
 * 错误率和空间、时间成反比
 *
 *
 * 源码中:
 *   numBits: 二进制向量的长度, 会根据传入的要存入数据的个数算出来的
 *   numHashFunctions: hash函数的个数，根据传入的容错率算出来的
 *
 * 若时间、空间越小，错误率越大
 * 时间、空间越大，错误率越小
 */
public class BloomFilterTest {

    private final static int TOTAL = 1000000;

    // 出错的有320个
    static BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), TOTAL);

    // 添加容错率后，出错的只有98个了
    // static BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), TOTAL, 0.01);

    static {
        for (int i = 0; i < TOTAL; i++) {
            bloomFilter.put(i);
        }
    }

    public static void main(String[] args) {
        // 校验存在的key会不会找不出来
        for (int i = 0; i < TOTAL; i++) {
            if (!bloomFilter.mightContain(i)) {
                System.out.println("出错，把存在的数据当成不存在了, 存在的数据: " + i);
            }
        }

        // 校验不存在的key是否会被命中
        int count = 0;
        for (int i = TOTAL; i < TOTAL + 10000; i++) {
            if (bloomFilter.mightContain(i)) {
                count++;
                //System.out.println("出错。把不存在的数据" + i + "误当成存在了");
            }
        }

        System.out.println("不存在的数据命中了" + count + "个");

    }
}
