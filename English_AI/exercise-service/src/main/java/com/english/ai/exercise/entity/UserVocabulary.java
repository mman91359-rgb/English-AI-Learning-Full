package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_vocabulary")
public class UserVocabulary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String word;

    private String translation;

    /** 音标，如 /ˈsɜːkəmstəns/ */
    private String phonetic;

    /** 词性，如 n./v./adj. */
    private String pos;

    /** 英文例句 */
    private String example;

    /** 例句中文翻译 */
    private String exampleTranslation;

    /** 常见搭配，分号分隔 */
    private String collocation;

    /** 来源: manual/daily_word/quiz */
    private String source;

    /** 累计复习次数 */
    private Integer reviewCount;

    /** 掌握度等级 0-5，5 为已掌握 */
    private Integer masteryLevel;

    /** 上次复习时间 */
    private LocalDateTime lastReviewedAt;

    /** 下次复习时间（按艾宾浩斯曲线计算） */
    private LocalDateTime nextReviewAt;

    private Integer isMastered;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
