package com.english.ai.exercise.dto;

import lombok.Data;

/**
 * 提交答案请求
 */
@Data
public class SubmitRequest {

    /** 用户ID */
    private Long userId;

    /** 题目ID */
    private Long questionId;

    /** 用户答案 */
    private String userAnswer;

    /** 答题用时(秒) */
    private Integer durationSeconds;

    /** 模块类型 */
    private Integer moduleType;
}
