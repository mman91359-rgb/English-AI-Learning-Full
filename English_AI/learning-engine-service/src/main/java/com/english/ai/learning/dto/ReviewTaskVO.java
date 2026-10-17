package com.english.ai.learning.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 复习任务视图
 */
@Data
@Builder
public class ReviewTaskVO {

    /** 记忆曲线记录ID */
    private Long curveId;

    /** 题目ID */
    private Long questionId;

    /** 模块类型 */
    private Integer moduleType;

    /** 掌握等级 */
    private Integer masteryLevel;

    /** 下次复习时间 */
    private LocalDateTime nextReviewTime;

    /** 复习次数 */
    private Integer reviewCount;

    /** 题干（从exercise-service获取） */
    private String stem;

    /** 正确答案（复习模式展示） */
    private String correctAnswer;

    /** 题目解析 */
    private String analysis;
}
