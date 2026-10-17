package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("word_dictionary")
public class WordDictionary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;

    private String phonetic;

    private String translation;

    private String pos;

    private String example;

    private String exampleTranslation;

    private String level;

    private Integer usageCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
