package com.english.ai.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户能力雷达表 — 映射 user_ability_radar 表
 */
@Data
@TableName("user_ability_radar")
public class UserAbilityRadar {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（唯一约束） */
    private Long userId;

    /** 词汇能力分 */
    private BigDecimal vocabularyScore;

    /** 语法能力分 */
    private BigDecimal grammarScore;

    /** 听力能力分 */
    private BigDecimal listeningScore;

    /** 阅读能力分 */
    private BigDecimal readingScore;

    /** ELO隐藏分 */
    private Integer eloScore;

    /** 目标等级 */
    private String targetLevel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
