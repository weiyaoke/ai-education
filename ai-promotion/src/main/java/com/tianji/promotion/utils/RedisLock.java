package com.tianji.promotion.utils;

import cn.hutool.core.util.BooleanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RedisLock {

    private final String key;
    private final StringRedisTemplate redisTemplate;
    public boolean tryLock(long timeOut, TimeUnit unit){
        //1、获取线程名称
        String value = Thread.currentThread().getName();
        //2、存储到redis中
        Boolean bool = this.redisTemplate.opsForValue().setIfAbsent(key, value, timeOut, unit);
        //3、判断是否成功获取锁
        return BooleanUtil.isTrue(bool);
    }
    public void unlock(){
        this.redisTemplate.delete(key);
    }
}
