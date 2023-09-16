package com.yupi.springbootinit.bizmq;

import org.apache.ibatis.annotations.Result;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Mr.Wang
 * @create 2023-09-16-14:11
 */
@Component
public class MyMessageProducer {
    //spring在启动的时候会将RabbitTemplate对象放入IOC容器中提供使用，就如同使用Redis的时候引入RedisTemplate一样
    @Resource
    private RabbitTemplate rabbitTemplate;

    public  void sendMessage(String exchange,String routingKey,String message){
        rabbitTemplate.convertAndSend(exchange,routingKey,message);
    }
}
