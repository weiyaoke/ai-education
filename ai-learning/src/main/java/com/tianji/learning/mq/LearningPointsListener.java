package com.tianji.learning.mq;

import com.tianji.common.constants.MqConstants;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.tianji.common.constants.MqConstants.Exchange.LEARNING_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LEARN_SECTION;
import static com.tianji.common.constants.MqConstants.Key.SIGN_IN;

@Component
@RequiredArgsConstructor
@Slf4j
public class LearningPointsListener {
    private final IPointsRecordService pointsRecordService;
    // 监听签到事件
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.point.queue",durable = "true"),
            exchange = @Exchange(name = LEARNING_EXCHANGE ,type = ExchangeTypes.TOPIC),
            key = SIGN_IN
    ))
    public void addSignPoints(SignInMessage signInMessage){
        this.pointsRecordService.addSPointsRecord(signInMessage.getUserId(),signInMessage.getPoints(), PointsRecordType.SIGN);
    }
    // 监听新增互动问答事件
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_REPLY
    ))
    public void listenWriteReplyMessage(Long userId){
        this.pointsRecordService.addSPointsRecord(userId, 5, PointsRecordType.QA);
    }
    // 监听学习记录事件
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.LEARN_SECTION
    ))
    public void listenLearningMessage(Long userId){
        this.pointsRecordService.addSPointsRecord(userId, 10, PointsRecordType.LEARNING);
    }
    // 监听写笔记事件
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_NOTE
    ))
    public void listenWriteNoteMessage(Long userId){
        this.pointsRecordService.addSPointsRecord(userId, 3, PointsRecordType.NOTE);
    }
    // 监听写笔记被采纳事件
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.NOTE_GATHERED
    ))
    public void listenNoteGatheredMessage(Long userId){
        this.pointsRecordService.addSPointsRecord(userId, 2, PointsRecordType.NOTE);
    }
    // 监听课程评价事件
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.COMMENT
    ))
    public void listenCommentMessage(Long userId){
        this.pointsRecordService.addSPointsRecord(userId, 10, PointsRecordType.COMMENT);
    }
}
