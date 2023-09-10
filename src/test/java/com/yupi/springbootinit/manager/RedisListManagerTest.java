package com.yupi.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author Mr.Wang
 * @create 2023-09-10-12:50
 */
@SpringBootTest
class RedisListManagerTest {
    @Resource
    private RedisLimitManager redisListManager;
    @Test
    public void doRateLimit() throws InterruptedException {
        String userId="1";
        for (int i = 0; i < 2; i++) {
            redisListManager.doRateLimit(userId);
            System.out.println("请求成功");
        }

        //睡眠1s钟
        Thread.sleep(1000);
        for (int i = 0; i < 5; i++) {
            redisListManager.doRateLimit(userId);
            System.out.println("请求成功");
        }
    }
}