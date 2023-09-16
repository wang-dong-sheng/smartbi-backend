package com.yupi.springbootinit.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Mr.Wang
 * @create 2023-09-11-11:26
 */
@RestController
@RequestMapping("/queue")
@Slf4j
@Profile({"dev","local"})//只针对本地和开发环境生效
public class QueueController {
    @Resource
    private ThreadPoolExecutor poolExecutor;
    @GetMapping("/add")
    public void add(String name){
        System.out.println();
        CompletableFuture.runAsync(()->{
            log.info("执行任务："+name+".执行人："+Thread.currentThread().getName());
            try {
                Thread.sleep(60000);//模拟该任务需要60s来处理
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },poolExecutor);//poolExecutor指定自定义线程池
    }

    @GetMapping("/get")
    public String get(){
        HashMap<String, Object> map = new HashMap<>();
        int size = poolExecutor.getQueue().size();
        map.put("队列长度",size);
        int activeCount = poolExecutor.getActiveCount();
        map.put("任务总数",activeCount);
        long completedTaskCount = poolExecutor.getCompletedTaskCount();
        map.put("已完成的任务数",completedTaskCount);
        int activeThread = poolExecutor.getActiveCount();
        map.put("正在进行的线程数",activeThread);

        //将map转为json返回给前端
        return JSONUtil.toJsonStr(map);
    }
}
