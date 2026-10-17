package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 生成待审核题目实体
 * 映射 question_pending 表，用于存储 AI 离线批量生成的题目，
 * 需经人工审核通过后才能入库到正式题目表
 */
@Data
@TableName("question_pending")
public class QuestionPending {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块类型: 1=词汇, 2=语法 */
    private Integer moduleType;

    /** 目标等级: CET4/CET6 */
    private String targetLevel;

    /** 难度等级: 1~8 */
    private Integer difficultyLevel;

    /** 题型: FILL_BLANK/SENTENCE_MAKING */
    private String questionType;

    /** 题干 */
    private String stem;

    /** 选项(JSON数组字符串) */
    private String options;

    /** 正确答案 */
    private String correctAnswer;

    /** 题目解析（中文） */
    private String analysis;

    /** 关联知识点名称 */
    private String knowledgePointName;

    /** 审核状态: PENDING=待审核, APPROVED=已通过, REJECTED=已驳回 */
    private String status;

    /** 审核人ID */
    private Long reviewerId;

    /** 审核意见 */
    private String reviewComment;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}