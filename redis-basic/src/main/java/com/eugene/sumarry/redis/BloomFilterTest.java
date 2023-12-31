package com.eugene.sumarry.redis;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * 布隆过滤器特点：
 * 只能用来判断一个元素是否存在，不能获取元素的具体信息。它只能确定一个元素一定不存在，能确定某个元素可能存在。
 *
 * 影响布隆过滤器的因素：
 *  1、预期插入元素的数量(n)：预期插入元素的数量越大，误报率越高（能够理解，因为元素越多，那bit位置被标记成1的地方就越多）
 *  2、位数组的大小(m): 位数组是布隆过滤器用于存储信息的数据结构。 位数组越大，误报率越小（可以理解，因为约大，那就不会把所有的位置都标为1）
 *  3、hash函数的数量(k): 布隆过滤器使用多个hash函数来决定元素在位数组的未知。hash函数越多，误报率越小。但当数量超过某个最优点之后，增加hash函数的数量会导致误报率上升。
 *  4、hash函数的质量：理想情况下，hash函数应该能够将输入均匀分布在位数组中。如果hash函数质量不高，可能导致某些位被过度使用，从而增加误报率。
 *  核心点：尽可能的将所有的数据均匀的分布在位数组中。要实现这个功能可以是：增加位数组的大小、减少预期插入元素数量、增加hash函数质量（保证都散列在每个位中）
 *
 * 通常：为了设计一个具有低误报率的布隆过滤器，在给定数组大小和元素数量的前提下，需要平衡hash函数的数量并确保hash函数的质量。此外，布隆过滤器的设计往往是
 *      根据预期的误报率和元素数量来方向计算所需的位数组大小和hash函数数量。
 *      举个例子：我现在想玩布隆过滤器中添加1w个元素，并且希望误报率为0.01。那我需要计算出数组大小和hash函数数量。
 *      数组大小: 因为期待1w个元素，我们可以根据业务去判断增长量，并放大一定的倍数。比如放大50倍，那数组大小为：50w
 *      计算公式：hash函数数量 = (int) Math.round((bitSetSize / expectedNumber) * Math.log(2.0))
 *      但我们一般用的都是guava现成的布隆过滤器api，我们只需要填写期望数量和误差率即可。guava内部会在此方法中计算出数组大小和hash函数的数量：
 *        com.google.common.hash.BloomFilter#create(com.google.common.hash.Funnel<? super T>, long, double, com.google.common.hash.BloomFilter.Strategy)
 *
 *
 * 布隆过滤器测试类：
 *
 * 错误率和空间、时间成反比
 *
 * 源码中:
 *   numBits: 二进制向量的长度, 会根据传入的要存入数据的个数算出来的
 *   numHashFunctions: hash函数的个数，根据传入的容错率算出来的
 *
 */
public class BloomFilterTest {

    private final static int EXCEPT_COUNT = 1000000;

    /**
     * 创建一个布隆过滤器，预期元素数量为100w，误报率为0.01
     * 存储的元素为integer类型
     *
     *
     */
    static BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), EXCEPT_COUNT, 0.01);

    // 添加容错率后，出错的只有98个了
    // static BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), TOTAL, 0.01);

    static {
        for (int i = 0; i < EXCEPT_COUNT; i++) {
            bloomFilter.put(i);
        }
    }

    public static void main(String[] args) {
        // 校验存在的key会不会找不出来
        for (int i = 0; i < EXCEPT_COUNT; i++) {
            if (!bloomFilter.mightContain(i)) {
                System.out.println("出错，把存在的数据当成不存在了, 存在的数据: " + i);
            }
        }

        // 校验不存在的key是否会被命中。从1000000 - 1010000的数字。只有98个数据被布隆过滤器判断成可能存在  98 / 10000 = 0.0098的概率，是跟0.01差不多的。符合要求
        int count = 0;
        for (int i = EXCEPT_COUNT; i < EXCEPT_COUNT + 10000; i++) {
            if (bloomFilter.mightContain(i)) {
                count++;
                //System.out.println("出错。把不存在的数据" + i + "误当成存在了");
            }
        }

        System.out.println("不存在的数据命中了" + count + "个");

    }
}
