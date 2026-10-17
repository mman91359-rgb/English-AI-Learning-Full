package com.english.ai.exercise.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 复习任务视图
 */
@Data
@Builder
public class ReviewTaskVO {

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

    /** 题干 */
    private String stem;
}
