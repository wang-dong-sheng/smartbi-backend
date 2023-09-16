package com.yupi.springbootinit.mq;

import com.rabbitmq.client.*;

public class DirectConsumer {

    private static final String EXCHANGE_NAME = "direct_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "direct");
        //自己创建消息队列
        String queueName = "xiaoyu_queue";
        channel.queueDeclare(queueName, true, false, false, null);
        //指定交换机和路由键的绑定规则,指定路由键名称为xiaoyu,当
        channel.queueBind(queueName, EXCHANGE_NAME, "xiaoyu");

        //自己创建消息队列
        String queueName1 = "xiaowang_queue";
        channel.queueDeclare(queueName1, true, false, false, null);
        channel.queueBind(queueName1, EXCHANGE_NAME, "xiaowang");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        //DeliverCallback定义怎么去处理这个队列
        DeliverCallback xiaoyuDeliverCallback1 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [xiaoyu] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        //DeliverCallback定义怎么去处理这个队列
        DeliverCallback xiaowangDeliverCallback2 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [xiaowang] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        //启动消息队列的客户端对该消息进行监听
        channel.basicConsume(queueName, true, xiaoyuDeliverCallback1, consumerTag -> {
        });

        //监听任务队列2
        channel.basicConsume(queueName1, true, xiaowangDeliverCallback2, consumerTag -> {
        });
    }
}