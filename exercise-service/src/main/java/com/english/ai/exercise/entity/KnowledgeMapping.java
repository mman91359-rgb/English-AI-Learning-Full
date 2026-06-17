package com.english.ai.exercise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 题目-知识点映射表（多对多）
 */
@Data
@TableName("knowledge_mapping")
public class KnowledgeMapping {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目ID */
    private Long questionId;

    /** 知识点ID */
    private Long knowledgePointId;
}
