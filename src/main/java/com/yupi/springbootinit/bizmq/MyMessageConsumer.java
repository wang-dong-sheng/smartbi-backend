package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Mr.Wang
 * @create 2023-09-16-14:18
 */
@Component
@Slf4j
public class MyMessageConsumer {

    //指定程序监听的消息队列和确认机制
    //channel作用：负责和rabbitmq进行通信
    @SneakyThrows//会将异常处理掉，这里是为了便于测试，实际项目中还是进行异常处理
    @RabbitListener(queues = {"code_queue"},ackMode ="MANUAL" )//ackMode ="MANUAL":表示ACK是由人工处理
    public void receiveMessage(String message, Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long diliveryTag){
        channel.basicAck(diliveryTag,false);
    }
}
