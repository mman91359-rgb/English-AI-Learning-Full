package com.english.ai.learning.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — learning-engine-service（消费者端）
 */
@Configuration
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

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
