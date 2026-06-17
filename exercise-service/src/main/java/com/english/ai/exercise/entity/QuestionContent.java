package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目内容表 — 前后端分离，只在答题时加载
 */
@Data
@TableName("question_content")
public class QuestionContent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 question_info.id */
    private Long questionId;

    /** 题干 */
    private String stem;

    /** 选项(JSON) */
    private String options;

    /** 正确答案 */
    private String correctAnswer;

    /** 题目解析 */
    private String analysis;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
