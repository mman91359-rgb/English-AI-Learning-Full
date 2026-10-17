package com.english.ai.ai.dto;

import lombok.Data;

/**
 * 单词详情视图对象
 * 用于 AI 自动补全单词本时返回结构化的单词信息
 */
@Data
public class WordDetailVO {

    /** 单词本身 */
    private String word;

    /** 音标，如 /ˈsɜːkəmstəns/ */
    private String phonetic;

    /** 词性，如 n./v./adj. */
    private String pos;

    /** 中文释义 */
    private String translation;

    /** 英文例句 */
    private String example;

    /** 例句中文翻译 */
    private String exampleTranslation;

    /** 常见搭配，分号分隔 */
    private String collocation;
}
