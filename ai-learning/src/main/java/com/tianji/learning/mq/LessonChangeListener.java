package com.tianji.learning.mq;

import cn.hutool.core.util.ObjectUtil;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.Constant;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LessonChangeListener {
    private final ILearningLessonService iLearningLessonService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void listenLessonPay(OrderBasicDTO order){
        //1、先进行一些参数校验
        if (ObjectUtil.isEmpty(order) || ObjectUtil.isEmpty(order.getUserId()) || ObjectUtil.isEmpty(order.getCourseIds())){
            return;
        }
        //2、将参数传到业务层进行相关封装操作
        this.iLearningLessonService.addUserLessons(order.getUserId(),order.getCourseIds());
    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.refund.queue",durable = "true"),
            exchange = @Exchange(name =  MqConstants.Exchange.ORDER_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_REFUND_KEY
    ))
    public void listenLessonRefund(OrderBasicDTO order){
        //先进行参数校验
        if (ObjectUtil.isEmpty(order) || ObjectUtil.hasEmpty(order.getOrderId(),order.getCourseIds())){
            return;
        }
        //业务层进行处理
        this.iLearningLessonService.deleteCourseFromLesson(order.getUserId(),order.getCourseIds());
    }

}
