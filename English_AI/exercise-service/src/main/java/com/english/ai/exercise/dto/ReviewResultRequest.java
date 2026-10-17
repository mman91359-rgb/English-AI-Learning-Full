package com.english.ai.exercise.dto;

import lombok.Data;

/**
 * 单词复习结果提交请求
 * 用户在复习模式中标记"记住/没记住"后提交
 */
@Data
public class ReviewResultRequest {

    /** 用户ID */
    private Long userId;

    /** 是否记住：true=记住（提升掌握度，延长下次复习间隔），false=没记住（重置掌握度） */
    private Boolean isCorrect;
}
