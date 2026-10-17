package com.english.ai.exercise.service;

import com.english.ai.exercise.dto.JudgmentResult;
import com.english.ai.exercise.dto.SubmitRequest;

/**
 * 三级分层路由判题引擎接口
 * 
 * 层级 1：规则算法层（优先执行，零 AI 开销）
 *   - 拼写错误：莱文斯坦编辑距离
 *   - 固定语法硬规则：正则 + 词性标注 + 名词可数/不可数、冠词搭配、时态固定约束等规则库
 * 
 * 层级 2：结构化题库知识图谱层（系统核心，教研沉淀）
 *   - Buggy Paths 常见错误路径表
 *   - 知识点标签映射表
 * 
 * 层级 3：LLM / 预训练语义模型兜底层（处理未知、未收录作答）
 *   - 语义相似度计算
 *   - LLM 动态生成启发反馈
 */
public interface JudgmentEngine {

    /**
     * 执行三级分层路由判题
     * @param request 提交请求
     * @param correctAnswer 正确答案
     * @param context 题干上下文
     * @return 判题结果
     */
    JudgmentResult judge(SubmitRequest request, String correctAnswer, String context);
}