package com.english.ai.exercise.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 练习完成事件 — 通过RabbitMQ发送给下游消费者（learning-engine等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 题目ID */
    private Long questionId;

    /** 模块类型: 1=词汇, 2=语法 */
    private Integer moduleType;

    /** 是否答对 */
    private Boolean isCorrect;

    /** 历史尝试次数 */
    private Integer attemptCount;

    /** 事件发生时间戳 */
    private Long timestamp;

    /** 全局追踪ID */
    private String traceId;
}
