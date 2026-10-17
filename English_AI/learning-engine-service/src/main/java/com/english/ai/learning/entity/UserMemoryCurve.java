package com.english.ai.learning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 艾宾浩斯遗忘曲线表 — 系统核心表
 */
@Data
@TableName("user_memory_curve")
public class UserMemoryCurve {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 题目ID */
    private Long questionId;

    /** 掌握等级: 1=陌生, 2=模糊, 3=熟悉, 4=熟练, 5=精通 */
    private Integer masteryLevel;

    /** 下次复习时间 */
    private LocalDateTime nextReviewTime;

    /** 已复习次数 */
    private Integer reviewCount;

    /** 最后复习时间 */
    private LocalDateTime lastReviewAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
