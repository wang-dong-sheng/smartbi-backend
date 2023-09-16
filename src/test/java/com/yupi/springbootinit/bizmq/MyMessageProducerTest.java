package com.yupi.springbootinit.bizmq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Priority;
import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mr.Wang
 * @create 2023-09-16-14:47
 */
@SpringBootTest
class MyMessageProducerTest {

    @Resource
    private MyMessageProducer myMessageProducer;

    @Test
    public void sendMessage(){
        String exchange="code_exchange";
        String routingKey="my_routingKey";
        String message="你好呀";
        myMessageProducer.sendMessage(exchange,routingKey,message);
    }

}