package com.english.ai.exercise.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VocabularyVO {
    private Long id;
    private Long userId;
    private String word;
    private String translation;
    /** 音标 */
    private String phonetic;
    /** 词性 */
    private String pos;
    /** 英文例句 */
    private String example;
    /** 例句中文翻译 */
    private String exampleTranslation;
    /** 常见搭配 */
    private String collocation;
    /** 来源: manual/daily_word/quiz */
    private String source;
    /** 累计复习次数 */
    private Integer reviewCount;
    /** 掌握度等级 0-5 */
    private Integer masteryLevel;
    /** 上次复习时间 */
    private LocalDateTime lastReviewedAt;
    /** 下次复习时间 */
    private LocalDateTime nextReviewAt;
    private Integer isMastered;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
