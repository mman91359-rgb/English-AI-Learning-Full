package com.english.ai.exercise.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 判题引擎结果
 */
@Data
@Builder
public class JudgmentResult {

    /** 是否正确 */
    private Boolean isCorrect;

    /** 启发式反馈提示 */
    private String heuristicHint;

    /** 错误类型 */
    private String errorType;

    /** 匹配层级：1（规则算法层）、2（结构化题库层）、3（AI兜底层） */
    private Integer matchedLevel;

    /** 额外建议列表 */
    private List<String> suggestions;

    /** 是否为语义匹配（近义词/同义表达被判对），由 Level 2.5 设置 */
    private Boolean isSemanticMatch;
}