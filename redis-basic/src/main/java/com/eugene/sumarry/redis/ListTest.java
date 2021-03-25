package com.eugene.sumarry.redis;

import redis.clients.jedis.Jedis;

/**
 * redis中的list可以实现很多功能：
 * 因为有lpush、lpop、rpush、rpop
 * 我们可以轻松实现一个队列（lpush & rpop）
 * 也可以轻松实现一个栈：rpush、rpop
 *
 */
public class ListTest {

    private static Jedis jedis = RedisBase.jedis;

    private static final String LIST_LEY = "avengerEug:list:test";

    public static void main(String[] args) {
        Long lpush = jedis.lpush(LIST_LEY, "a", "v", "e", "n", "g", "e", "r", "e", "u", "g");
        System.out.println("添加进去后，list的数量：" + lpush);

        System.out.println("=========模拟队列，左进右出========");
        String lpop = jedis.lpop(LIST_LEY);
        jedis.lpush(LIST_LEY, lpop);

        System.out.println("使用lua脚本，保证队列的右出左进操作是一个原子性操作。模拟环形队列");
        System.out.println("未执行完lua脚本时的队列内容：" + jedis.lrange(LIST_LEY, 0, -1));
        String luaScript = "local element = redis.call('rpop', KEYS[1]) \n " +
                " if (element) then redis.call('lpush', KEYS[1], element) end";
        jedis.eval(luaScript, 1, LIST_LEY);
        System.out.println("执行完lua脚本时的队列内容：" + jedis.lrange(LIST_LEY, 0, -1));
    }
}
