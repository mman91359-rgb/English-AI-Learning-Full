package com.english.ai.exercise.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — exercise-service（生产者端）
 * 当 spring.rabbitmq.enabled=false 时自动禁用RabbitMQ相关Bean
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "exercise.exchange";
    public static final String QUEUE_NAME = "exercise.complete.queue";
    public static final String ROUTING_KEY = "exercise.complete";

    @Bean
    public TopicExchange exerciseExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue completeQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding completeBinding() {
        return BindingBuilder.bind(completeQueue()).to(exerciseExchange()).with(ROUTING_KEY);
    }

    /** 使用JSON序列化消息，替代默认的Java序列化 */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
