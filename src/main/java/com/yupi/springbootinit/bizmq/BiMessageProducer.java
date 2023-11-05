package com.yupi.springbootinit.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Mr.Wang
 * @create 2023-09-16-14:11
 */
@Component
public class BiMessageProducer {
    //spring在启动的时候会将RabbitTemplate对象放入IOC容器中提供使用，就如同使用Redis的时候引入RedisTemplate一样
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发消息
     * @param message
     */
    public  void sendMessage(String[] message){
        rabbitTemplate.convertAndSend(BiMqConstance.BI_EXCHANGE_NAME,BiMqConstance.BI_ROUNTINGKEY_NMAE,message);
    }
}
