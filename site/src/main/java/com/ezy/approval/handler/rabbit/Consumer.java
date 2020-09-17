package com.ezy.approval.handler.rabbit;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.ezy.approval.config.RabbitConfig;
import com.ezy.approval.entity.RabbitMessage;
import com.ezy.approval.handler.ApprovalHandler;
import com.ezy.approval.model.callback.approval.ApprovalStatuChangeEvent;
import com.ezy.approval.service.IRabbitMessageService;
import com.ezy.approval.service.RedisService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Caixiaowei
 * @ClassName Consumer.java
 * @Description rabbit MQ 消费者
 * @createTime 2020年07月30日 17:31:00
 */
@Component
@Slf4j
public class Consumer {

    @Autowired
    private ApprovalHandler approvalHandler;
    @Autowired
    private IRabbitMessageService rabbitMessageService;
    @Autowired
    private RedisService redisService;

    @RabbitListener(queues = RabbitConfig.QUEUE_APPROVAL)
    public void handleMessage(Message message, Channel channel) throws IOException {
        String messageId = message.getMessageProperties().getMessageId();
        /**
         * 防止重复消费，可以根据传过来的唯一ID先判断缓存数据中是否有数据
         * 1、有数据则不消费，直接应答处理
         * 2、缓存没有数据，则进行消费处理数据，处理完后手动应答
         * 3、如果消息 处理异常则，可以存入数据库中，手动处理（可以增加短信和邮件提醒功能）
         */
        if (StrUtil.isEmpty(messageId)) {
            return;
        }
        try {
            RabbitMessage rabbitMessage = rabbitMessageService.getById(Long.valueOf(messageId));
            Boolean isConsumed = rabbitMessage.getIsConsumed();
            String json = new String(message.getBody(), "UTF-8");
            log.info("handleMessage 消费消息: {}", json);
            if (!isConsumed) {
                //业务处理
                ApprovalStatuChangeEvent approvalStatuChangeEvent = JSONObject.parseObject(json, ApprovalStatuChangeEvent.class);
                approvalHandler.handle(approvalStatuChangeEvent);

                // 消费成功, 更新消息记录状态为: 已消费
                rabbitMessage.setIsConsumed(true);
                rabbitMessageService.updateById(rabbitMessage);

                //手动应答
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            } else {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            }

        } catch (Exception e) {
            log.error("handleMessage 消费失败,message: {}, error: {}", new String(message.getBody(), "UTF-8"), e);
            // 处理消息失败，将消息重新放回队列
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,true);
        }

    }
}
