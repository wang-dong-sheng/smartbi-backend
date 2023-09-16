package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DlxDirectConsumer {

    private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";
    private static final String WORK_EXCHANGE_NAME = "direct2_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(WORK_EXCHANGE_NAME, "direct");


        //指定死信队列参数
        Map<String, Object> args = new HashMap<>();
        //要绑定到那个交换机
        args.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        //指定死信要转发到哪个死信队列
        args.put("x-dead-letter-routing-key", "waibao");

        //自己创建消息队列
        String queueName = "xiaodog_queue";
        //将 "xiaodog_queue"队列和死信队列进行绑定,即将参数args放入
        channel.queueDeclare(queueName, true, false, false, args);
        //指定交换机和路由键的绑定规则,指定路由键名称为xiaodog,当
        channel.queueBind(queueName, WORK_EXCHANGE_NAME, "xiaodog");


        Map<String, Object> args2 = new HashMap<>();
        args2.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        args2.put("x-dead-letter-routing-key", "laoban");

        //自己创建消息队列
        String queueName1 = "xiaocat_queue";
        channel.queueDeclare(queueName1, true, false, false, args2);
        channel.queueBind(queueName1, WORK_EXCHANGE_NAME, "xiaocat");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        //DeliverCallback定义怎么去处理这个队列
        DeliverCallback xiaodogDeliverCallback1 = (consumerTag, delivery) -> {
            //拒绝消息
//            channel.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [xiaodog] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        //DeliverCallback定义怎么去处理这个队列
        DeliverCallback xiaocatDeliverCallback2 = (consumerTag, delivery) -> {
            //拒绝消息
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);

            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [xiaocat] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        //启动消息队列的客户端对该消息进行监听
        channel.basicConsume(queueName, false, xiaodogDeliverCallback1, consumerTag -> {
        });
        //监听任务队列2
        channel.basicConsume(queueName1, false, xiaocatDeliverCallback2, consumerTag -> {
        });

    }
}