package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_buggy_path")
public class QuestionBuggyPath {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private String wrongAnswer;

    private String heuristicHint;

    private String errorType;

    private String knowledgePointId;

    private Integer occurrenceCount;

    private Integer isAiGenerated;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}