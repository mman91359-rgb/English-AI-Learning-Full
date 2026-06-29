package com.english.ai.exercise.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 提交答案响应
 */
@Data
@Builder
public class SubmitResponse {

    /** 是否正确 */
    private Boolean isCorrect;

    /** 正确答案（完整文本，如 "A. established"） */
    private String correctAnswer;

    /** 正确答案选项（仅字母，如 "A"），方便前端高亮对比 */
    private String correctAnswerKey;

    /** 题目解析 */
    private String analysis;

    /** 追踪ID */
    private String traceId;
}
