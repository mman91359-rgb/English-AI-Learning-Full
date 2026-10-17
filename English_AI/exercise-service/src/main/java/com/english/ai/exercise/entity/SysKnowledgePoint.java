package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识点树表
 */
@Data
@TableName("sys_knowledge_point")
public class SysKnowledgePoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 知识点名称 */
    private String name;

    /** 父知识点ID(0=根节点) */
    private Long parentId;

    /** 考试级别 */
    private String cetLevel;

    /** 排序号 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
