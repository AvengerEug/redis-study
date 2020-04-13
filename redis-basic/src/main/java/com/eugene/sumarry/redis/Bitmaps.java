package com.eugene.sumarry.redis;

/**
 * 位图:
 *
 * 在redis中，一个字符串存储的底层是使用二进制来存储的，假设
 * redis存入了test1="abc"
 *
 * a的二进制为1100001
 * b的二进制为1100010
 * c的二进制为1100011
 * 那么abc在底层存储的样子为: 110000111000101100011
 */
public class Bitmaps extends Base {

    private static void modifiedBitmapsTest() {
        jedis.setbit("bitmapsTest", 6L, "1");
        jedis.setbit("bitmapsTest", 7L, "0");
    }

    private static void init() {
        jedis.set("bitmapsTest", "abc");
    }

    public static void main(String[] args) {
        // 使用bitmaps修改字符串二进制数据
        init();
        logger.info("修改前: {}", jedis.get("bitmapsTest"));
        modifiedBitmapsTest();
        logger.info("修改后: {}", jedis.get("bitmapsTest"));

        // 统计二进制中有多少个1
        logger.info("二进制中有{}个1", jedis.bitcount("bitmapsTest"));

        // 因为abc这一串字符串对应的二进制只有21位，若使用setbit命令设置的偏移量超过了21
        // jedis.setbit("bitmapsTest", 100L, "1");
        // 那么redis会在abc这一串字符串后面的79位全部变为0(不管设置的是1还是0)，
        // 所以偏移量超过了长度，则会自动扩容，且用0代替
    }
}
