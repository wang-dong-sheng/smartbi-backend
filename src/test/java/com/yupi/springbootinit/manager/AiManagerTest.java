package com.yupi.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mr.Wang
 * @create 2023-09-03-19:59
 */
@SpringBootTest
class AiManagerTest {

    @Resource
    private AiManager aiManager;
    @Test
    void doChart() {
        String s = aiManager.doChart(1698271403607916545L,"需求分析：\n" +
                "分析网站用户的增长情况\n" +
                "原始数据如下：\n" +
                "日期,用户数\n" +
                "1,10\n" +
                "2,20\n" +
                "3,30");
        System.out.println(s);
    }
}