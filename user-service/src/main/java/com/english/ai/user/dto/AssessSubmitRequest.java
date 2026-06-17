package com.english.ai.user.dto;

import lombok.Data;

/**
 * 定级评估提交请求
 */
@Data
public class AssessSubmitRequest {

    /** 用户ID */
    private Long userId;

    /** 题目难度等级(1~8) */
    private Integer difficultyLevel;

    /** 模块类型: 1=词汇, 2=语法 */
    private Integer moduleType;

    /** 是否答对 */
    private Boolean isCorrect;
}
