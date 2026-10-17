package com.english.ai.ai.dto;

import lombok.Data;

/**
 * AI 语义可接受性判对请求DTO
 */
@Data
public class JudgeAcceptableRequest {
    /** 用户答案 */
    private String userAnswer;
    /** 标准答案 */
    private String correctAnswer;
    /** 题干上下文（含占位符，如 "The ____ of the accident is still unknown."） */
    private String context;
    /** 题目ID */
    private Long questionId;
}
