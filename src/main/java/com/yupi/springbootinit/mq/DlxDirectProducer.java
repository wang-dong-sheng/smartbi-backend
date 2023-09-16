package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.Scanner;

public class DlxDirectProducer {
    private static final String WORK_EXCHANGE_NAME = "direct2_exchange";

  private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        //声明死信交换机
        channel.exchangeDeclare(DEAD_EXCHANGE_NAME, "direct");

        //创建死信队列
        String queueName = "laoban_dlx_queue";
        channel.queueDeclare(queueName, true, false, false, null);
        //指定交换机和路由键的绑定规则,指定路由键名称为xiaoyu,当
        channel.queueBind(queueName, DEAD_EXCHANGE_NAME, "laoban");

        String queueName1 = "waibao_dlx_queue";
        channel.queueDeclare(queueName1, true, false, false, null);
        //指定交换机和路由键的绑定规则,指定路由键名称为xiaoyu,当
        channel.queueBind(queueName1, DEAD_EXCHANGE_NAME, "waibao");

        //定义老板和外包处理程序

        //DeliverCallback定义怎么去处理这个队列
        DeliverCallback laobanDeliverCallback = (consumerTag, delivery) -> {
            //拒绝消息
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [laoban] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "    ");
        };

        //DeliverCallback定义怎么去处理这个队列
        DeliverCallback waibaoDeliverCallback = (consumerTag, delivery) -> {
            //拒绝消息
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);

            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [xiaocat] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        //启动消息队列的客户端对该消息进行监听
        channel.basicConsume(queueName1, false, laobanDeliverCallback, consumerTag -> {
        });
        //监听任务队列2
        channel.basicConsume(queueName, false, waibaoDeliverCallback, consumerTag -> {
        });

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()){
            String userInput = scanner.nextLine();
            String[] split = userInput.split(" ");
            if (split.length<1){
                continue;
            }
            String message=split[0];
            String routingKey=split[1];

            //给工作队列发消息
            channel.basicPublish(WORK_EXCHANGE_NAME, routingKey,
                    null,
                    message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "with routing"+routingKey);
        }





    }

  }
  //..
}