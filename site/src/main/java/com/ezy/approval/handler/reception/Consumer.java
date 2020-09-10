package com.ezy.approval.handler.reception;

import cn.hutool.core.util.StrUtil;
import com.ezy.approval.config.RabbitConfig;
import com.ezy.approval.handler.ApprovalHandler;
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

    AtomicInteger i = new AtomicInteger(1);

    @Autowired
    private ApprovalHandler approvalHandler;

    private int getNO() {
        return i.getAndIncrement();
    }

    @RabbitHandler
    public void process(String hello) {
        System.out.println("Receiver : " + hello);
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_APPROVAL)
    public void handleMessage(Message message, Channel channel) throws IOException {
        String messageId = message.getMessageProperties().getMessageId();
        if (StrUtil.isEmpty(messageId)) {
            return;
        }
        String json = new String(message.getBody(), "UTF-8");
        log.info("message: {}", json);
        /**
         * 防止重复消费，可以根据传过来的唯一ID先判断缓存数据中是否有数据
         * 1、有数据则不消费，直接应答处理
         * 2、缓存没有数据，则进行消费处理数据，处理完后手动应答
         * 3、如果消息 处理异常则，可以存入数据库中，手动处理（可以增加短信和邮件提醒功能）
         */
        // TODO: 2020/9/9 查找messageId 是否已消费
//        boolean consumed = false;
//        if (!consumed) {
//            try {
//                String json = new String(message.getBody(), "UTF-8");
//                log.info("handleMessage 消费消息: {}", json);
//                //业务处理
//                ApprovalStatuChangeEvent approvalStatuChangeEvent = JSONObject.parseObject(json, ApprovalStatuChangeEvent.class);
//                approvalHandler.handle(approvalStatuChangeEvent);
//
//                //手动应答
//                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//            }catch (Exception e){
//                log.error("handleMessage 消费失败,message: {}, error: {}"+ message.getBody(), e);
//                // 处理消息失败，将消息重新放回队列
//                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,true);
//            }
//
//        } else {
//            // TODO: 2020/9/9 缓存已消费的 mq
//
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//        }

        try {
//            int no = getNO();
//            log.info("no: {}", no);
//            no ++;
            int a = 1/0;

        } catch (Exception e) {
            int no = getNO();
            log.info(" 第 N 次重试no : {}, messageId:{}", no, messageId);
            log.info(" 第 N 次重试i: {}, messageId:{}", i, messageId);
            if (i.get() < 5) {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,true);
            } else {
                i.set(1);
            }

        }

    }
}
