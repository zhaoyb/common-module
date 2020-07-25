package com.commonmodule.ratelimiter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;

public class LimitUtil {

    private static JedisPool pool;
    private static String sha1 = "";

    public static void init(String ip, int port, HashMap<String, String> config) {
        if (config == null) {
            config = Maps.newHashMap();
        }
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(Integer.parseInt(config.getOrDefault("maxTotal", "8")));
        jedisPoolConfig.setMaxIdle(Integer.parseInt(config.getOrDefault("maxIdle", "8")));
        jedisPoolConfig.setMaxWaitMillis(Long.parseLong(config.getOrDefault("maxWaitMillis", "-1")));
        jedisPoolConfig.setTestOnBorrow(Boolean.parseBoolean(config.getOrDefault("testOnBorrow", "false")));
        pool = new JedisPool(jedisPoolConfig, ip, port, 10000);
        try (Jedis jedis = pool.getResource()) {
            sha1 = jedis.scriptLoad(CharStreams.toString(new InputStreamReader(LimitUtil.class.getResourceAsStream("/concurrent_request_rate_limiter.lua"))));
            System.out.println(sha1);
            if (StringUtils.isBlank(sha1)) {
                throw new RuntimeException("script init error");
            }
        } catch (IOException e) {
            throw new RuntimeException("ratelimiter init error", e);
        }
    }


    public static boolean rateLimit(String key, int max, int rate) {
        try (Jedis jedis = pool.getResource()) {
            return Integer.parseInt(jedis.evalsha(sha1, Collections.singletonList(key), Lists.newArrayList(Integer.toString(max), Integer.toString(rate),
                                                                                                           Long.toString(System.currentTimeMillis()))).toString()) > 0;
        }
    }


}
