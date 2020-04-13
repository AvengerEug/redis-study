package com.eugene.sumarry.redis;

import redis.clients.jedis.GeoUnit;

/**
 * GEO: ZSET类型的的一个扩展
 * 用来计算两个点的距离
 * 比如说我给北京市天安门的经纬度和长沙黄花机场的经纬度
 * 然后利用redis的api来计算他们的直线距离
 *
 */
public class GEO extends Base {

    static class Location {
        double longitude;
        double latitude;

        public Location(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }

    public static void main(String[] args) {
        // 经纬度只能保留6位小数
        // 116.397470, 39.908823
        Location bjtam = new Location(116.397470, 39.908823);

        // 113.222190, 28.188660
        Location cshhjc = new Location(113.222190,28.188660 );

        jedis.geoadd("commonKey", bjtam.longitude, bjtam.latitude, "bjtam");
        jedis.geoadd("commonKey", cshhjc.longitude, cshhjc.latitude, "cshhjc");
        logger.info("北京天安门距离长沙黄花机场: {} KM", jedis.geodist("commonKey", "bjtam", "cshhjc", GeoUnit.KM));
    }
}
