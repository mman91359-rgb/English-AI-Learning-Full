package com.english.ai.exercise.dto;

import lombok.Data;

/**
 * AI 生成的单词详情（exercise-service 内部使用）
 * 通过 RestTemplate 调用 ai-service 的 /ai/word-detail 接口获取
 */
@Data
public class WordDetailVO {

    /** 单词 */
    private String word;

    /** 音标 */
    private String phonetic;

    /** 词性 */
    private String pos;

    /** 中文释义 */
    private String translation;

    /** 英文例句 */
    private String example;

    /** 例句中文翻译 */
    private String exampleTranslation;

    /** 常见搭配 */
    private String collocation;
}
