package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目元数据表 — 高频查询，不含正文
 */
@Data
@TableName("question_info")
public class QuestionInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块类型: 1=词汇, 2=语法 */
    private Integer moduleType;

    /** 目标等级: CET4/CET6 */
    private String targetLevel;

    /** 难度等级: 1~8（ELO算法中的"题目难度分"） */
    private Integer difficultyLevel;

    /** 题型: CHOICE/FILL_BLANK/TRANSLATION/WRITING */
    private String questionType;

    /** 关联主知识点ID */
    private Long knowledgePointId;

    /** 来源: MANUAL/AI/IMPORT */
    private String source;

    /** AI学习到的可接受答案列表（JSON数组），如 ["circumstance","condition"] */
    private String acceptedAnswers;

    /** 是否AI生成：0=原始题目，1=AI生成的可接受答案待审核 */
    private Integer aiGenerated;

    /** 被使用次数 */
    private Integer usageCount;

    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
