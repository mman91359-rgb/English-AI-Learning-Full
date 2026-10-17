package com.english.ai.learning.dto;

import lombok.Data;

/**
 * 更新遗忘曲线请求
 */
@Data
public class UpdateCurveRequest {

    /** 用户ID */
    private Long userId;

    /** 题目ID */
    private Long questionId;

    /** 是否正确 */
    private Boolean isCorrect;

    /** 模块类型 */
    private Integer moduleType;
}
