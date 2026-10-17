package com.english.ai.exercise.dto;

import lombok.Data;

@Data
public class AddVocabularyRequest {
    private Long userId;
    private String word;
    private String translation;
    /** 来源: manual/daily_word/quiz，仅智能添加时使用 */
    private String source;
}
