package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 答题记录表
 */
@Data
@TableName("user_exercise_log")
public class UserExerciseLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 题目ID */
    private Long questionId;

    /** 用户答案 */
    private String userAnswer;

    /** 是否正确: 1=正确, 0=错误 */
    private Integer isCorrect;

    /** 答题用时(秒) */
    private Integer durationSeconds;

    /** 模块类型 */
    private Integer moduleType;

    /** 全链路追踪ID */
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
