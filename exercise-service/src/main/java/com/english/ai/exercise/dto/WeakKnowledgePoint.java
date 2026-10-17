package com.english.ai.exercise.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 薄弱知识点DTO
 * 用于返回用户错误最多的知识点统计数据
 */
@Data
@Builder
public class WeakKnowledgePoint {

    /** 知识点ID */
    private Long knowledgePointId;

    /** 知识点名称 */
    private String knowledgePointName;

    /** 该知识点错题数量 */
    private Integer errorCount;
}
