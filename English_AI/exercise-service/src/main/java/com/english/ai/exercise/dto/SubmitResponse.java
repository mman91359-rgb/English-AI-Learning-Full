package com.english.ai.exercise.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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

    /** 启发式反馈提示（引导思考，不直接给答案） */
    private String heuristicHint;

    /** 错误类型：SPELLING（拼写错误）、GRAMMAR（语法错误）、PREPOSITION（介词错误）、
     * ARTICLE（冠词错误）、TENSE（时态错误）、VOCABULARY（词汇错误）、UNKNOWN（未知） */
    private String errorType;

    /** 匹配层级：1（规则算法层）、2（结构化题库层）、3（AI兜底层） */
    private Integer matchedLevel;

    /** 额外建议列表 */
    private List<String> suggestions;

    /** 是否为语义匹配（近义词/同义表达被判对），前端据此展示标准答案供参考 */
    private Boolean isSemanticMatch;
}
