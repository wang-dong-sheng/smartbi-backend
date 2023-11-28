package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.constant.RedisConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Mr.Wang
 * @create 2023-09-16-14:18
 */
@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserService userService;

    //指定程序监听的消息队列和确认机制
    //channel作用：负责和rabbitmq进行通信
    @SneakyThrows//会将异常处理掉，这里是为了便于测试，实际项目中还是进行异常处理
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name=BiMqConstance.BI_QUEUE_NAME),
            exchange = @Exchange(name=BiMqConstance.BI_EXCHANGE_NAME,type = ExchangeTypes.DIRECT),
            key = BiMqConstance.BI_ROUNTINGKEY_NMAE
    ),
    ackMode ="MANUAL" )//ackMode ="MANUAL":表示ACK是由人工处理
    public void receiveMessage(String[] message, Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long diliveryTag){
       //先判断消息中是否有用，在判断存放在数据库中的数据是否有用
        log.info("收到消息:"+message.toString());
        long chartId=Long.parseLong(message[0]);
        String userId=message[1];
        if (StringUtils.isBlank(message[0])){
            //如果失败消息拒绝
            //requeue是否要重新放回队列里面
            channel.basicNack(diliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        Chart chart = chartService.getById(chartId);
        if (chart==null){
            channel.basicNack(diliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图标为空");
        }
        //判断完之后进行处理
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        //更新如果失败则拒绝消息的签收
        if (!b) {
            channel.basicNack(diliveryTag,false,false);
            handlerUpdateChartError(chart.getId(), "更新图表执行中状态失败");
//                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新图表中的状态失败");
            //查询当前用户,失败ai调用次数+1
            User user = userService.getById(userId);
            user.setNum(user.getNum()+1);
            userService.updateById(user);
            return;
        }

        //检查，更新完chart信息后调用ai进行处理
        //调用ai
        String res = aiManager.doChart(CommonConstant.BI_MODEL_ID, buildUserInput(chart));
        //将回答进行拆分
        String[] split = res.split("【【【【【");
        if (split.length < 3) {
            channel.basicNack(diliveryTag,false,false);
            handlerUpdateChartError(chart.getId(), "AI生成错误");
//                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误");

            return;
        }
        //将ai生成的值划分后得到三个模块
        //第二个模块为ECharts所需要的前端代码
        //第三个模块为生成结论
        String genChart = split[1].trim();
        String genResult = split[2].trim();

        //ai处理结束后再次更新数据库的信息
        Chart updateResultChart = new Chart();
        updateResultChart.setId(chart.getId());
        updateResultChart.setGenChart(genChart);
        updateResultChart.setGenResult(genResult);
        //建议定义状态为枚举类型
        updateResultChart.setStatus("succeed");
        boolean updateResult = chartService.updateById(updateResultChart);
        if (!updateResult) {
            channel.basicNack(diliveryTag,false,false);
            handlerUpdateChartError(chart.getId(), "更新图表成功状态失败");
//                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新图表中的状态失败");

            return;
        }

        //图表更新完成时删除redis中的缓存
        //数据库更新完成后，删除redis中的缓存

        String key= RedisConstant.CHAR_LIST_KEY+userId;
        stringRedisTemplate.delete(key);
        //如果处理成功，消息确认；
        channel.basicAck(diliveryTag,false);
    }

    /**
     * 用户输入
     * @param chart
     * @return
     */
    private String buildUserInput(Chart chart){
        String goal=chart.getGoal();
        String chartType=chart.getChartType();
        String csvData = chart.getChartData();
        //用户输入：
        StringBuffer userInput = new StringBuffer();
        userInput.append("需求分析：").append("\n");
        //拼接分析目标
        String userGoal = goal;
        if (chartType != null) {
            userGoal = "请使用:" + chartType + userGoal;
        }

        userInput.append(userGoal).append("\n");
        userInput.append("原始数据如下：：").append("\n");

        //压缩后的数据

        userInput.append(csvData).append("\n");

        return userInput.toString();

    }
    public void handlerUpdateChartError(Long chartId,String massage){
        Chart updateResultChart = new Chart();
        updateResultChart.setId(chartId);
        updateResultChart.setStatus("failed");
        updateResultChart.setExecMassage("execMassage");
        boolean b = chartService.updateById(updateResultChart);
        if(!b){
            log.error("更新图表状态失败",+chartId+":"+massage);
        }

    }
}
