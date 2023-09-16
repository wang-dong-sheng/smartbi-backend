package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MultiConsumer {

  private static final String TASK_QUEUE_NAME = "multi_queue";

  public static void main(String[] argv) throws Exception {
      //建立连接
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    final Connection connection = factory.newConnection();

      for (int i = 0; i < 2; i++) {
          final Channel channel = connection.createChannel();

          channel.basicQos(1);//表示的意思每个消费者最多可以处理的任务数

          channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
          System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
          //定义如何处理消息
          int finalI = i;
          DeliverCallback deliverCallback = (consumerTag, delivery) -> {
              String message = new String(delivery.getBody(), "UTF-8");
              try {
                  //处理工作
                  System.out.println(" [x] Received '" + "处理人："+ finalI +message + "'");
                  //停20s，来模拟机器处理能力有限
                  Thread.sleep(20000);

                  channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
              } catch (InterruptedException e) {
                  System.out.println();
                  channel.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
                  throw new RuntimeException(e);
              } finally {
                  System.out.println(" [x] Done");
                  channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
              }
          };
          //开启消费监听
          channel.basicConsume(TASK_QUEUE_NAME, true, deliverCallback, consumerTag -> { });
      }

  }



}