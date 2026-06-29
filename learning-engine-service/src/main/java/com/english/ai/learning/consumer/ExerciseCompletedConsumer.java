package com.english.ai.learning.consumer;

import com.english.ai.learning.dto.UpdateCurveRequest;
import com.english.ai.learning.service.LearningService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 练习完成事件消费者
 *
 * 监听 exercise.complete.queue，收到消息后更新艾宾浩斯遗忘曲线。
 * 手动ACK模式，消费成功才确认，失败重新入队。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExerciseCompletedConsumer {

    private final LearningService learningService;

    @RabbitListener(queues = "exercise.complete.queue", ackMode = "MANUAL")
    public void onExerciseCompleted(Map<String, Object> message,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            Long userId = toLong(message.get("userId"));
            Long questionId = toLong(message.get("questionId"));
            Boolean isCorrect = (Boolean) message.get("isCorrect");
            Integer moduleType = message.get("moduleType") != null
                    ? ((Number) message.get("moduleType")).intValue() : null;

            log.info("收到练习完成事件: userId={}, questionId={}, correct={}", userId, questionId, isCorrect);

            UpdateCurveRequest request = new UpdateCurveRequest();
            request.setUserId(userId);
            request.setQuestionId(questionId);
            request.setIsCorrect(isCorrect);
            request.setModuleType(moduleType);
            learningService.updateCurve(request);

            // 手动ACK确认
            channel.basicAck(deliveryTag, false);
            log.debug("消息确认: deliveryTag={}", deliveryTag);

        } catch (Exception e) {
            log.error("消费练习完成事件失败: {}", e.getMessage(), e);
            try {
                // 消费失败，拒绝并重新入队
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("消息拒绝失败: {}", ioException.getMessage());
            }
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
