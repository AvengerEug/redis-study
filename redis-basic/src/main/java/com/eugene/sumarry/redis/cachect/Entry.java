package com.eugene.sumarry.redis.cachect;

import com.eugene.sumarry.redis.cachect.dao.UserDao;
import com.eugene.sumarry.redis.cachect.model.User;

/**
 * 测试缓存击穿
 *
 * 缓存击穿是因为key不存在，导致一直访问db
 * 解决方案: 使用布隆过滤器，在应用启动过程中将数据预热
 * (做的好的话也可以添加api进行重新预热)
 */
public class Entry {


    public static void main(String[] args) {
        UserDao userDao = new UserDao();
        // 数据预热，将db中的数据存入布隆过滤器中
        userDao.init();

        for (Long i = 95L; i < 150l; i++) {
            User user = userDao.execGetById(i);
            if (user == null) {
                System.err.println("db中无id: " + i + "的用户");
            } else {
                System.out.println("布隆过滤器中存在，用户id为: " + user.getUserId());
            }
        }

        // 最后会出现： 布隆过滤器中存在，用户id为: 122
        // 没关系，这就是布隆过滤器的容错率，不存在的有可能会被布隆过滤器表示为存在
        // 存在的也可能是被布隆过滤器标识为不存在，
        // 所以此时我们可以维护一个白名单，每次布隆过滤器认为在的数据，我们就去
        // 验证下白名单中是否存在，这个白名单可以是map、也可以是其他数据结构

    }
}
