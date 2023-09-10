package com.yupi.springbootinit.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mr.Wang
 * @create 2023-09-10-10:43
 */
@SpringBootTest
class ChartMapperTest {

    @Resource
    private ChartMapper chartMapper;
    @Test
    void queryChartData(){
        String chartId="1699314576934268929";
        String querySql=String.format("select * from chart_%s",chartId);
        List<Map<String, Object>> resultData = chartMapper.queryChartData(querySql);
        System.out.println(resultData);
    }

}