package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本表
 */
@Data
@TableName("user_error_book")
public class UserErrorBook {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 题目ID */
    private Long questionId;

    /** 累计错误次数 */
    private Integer wrongCount;

    /** 最后错误时间 */
    private LocalDateTime lastWrongAt;

    /** 是否已掌握: 0=未解决, 1=已解决 */
    private Integer isResolved;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
