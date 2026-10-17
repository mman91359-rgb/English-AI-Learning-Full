package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_word")
public class DailyWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long wordId;

    private LocalDate date;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
