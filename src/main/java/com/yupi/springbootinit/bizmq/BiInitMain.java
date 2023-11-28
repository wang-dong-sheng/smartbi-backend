package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 用于测试用到的交换机队列（只用在程序启动前执行一次）
 * @author Mr.Wang
 * @create 2023-09-16-14:33
 */
public class BiInitMain {
    public static void main(String[] args) {
        //如果修改了默认的用户名密码等需要重新设置
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("champion");
        factory.setHost("47.109.60.15");
        factory.setPassword("@KunKun2023");

        try {

            //创建连接、创建频道
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String EXCHANGE_NAME=BiMqConstance.BI_EXCHANGE_NAME;
            channel.exchangeDeclare(EXCHANGE_NAME,"direct");

            //创建消息队列
            String queueNmae=BiMqConstance.BI_QUEUE_NAME;
            channel.queueDeclare(queueNmae, true, false, false, null);
            channel.queueBind(queueNmae,EXCHANGE_NAME,BiMqConstance.BI_ROUNTINGKEY_NMAE);

        }catch (Exception e){

        }


    }
}
