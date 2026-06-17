package com.english.ai.exercise.service;

import com.english.ai.exercise.dto.QuestionVO;
import com.english.ai.exercise.dto.SubmitRequest;
import com.english.ai.exercise.dto.SubmitResponse;
import com.english.ai.exercise.dto.ReviewTaskVO;
import com.english.ai.exercise.entity.SysKnowledgePoint;

import java.util.List;

/**
 * 练习服务接口
 */
public interface ExerciseService {

    /**
     * 智能选题
     * @param userId     用户ID
     * @param moduleType 1=词汇, 2=语法
     * @param count      题目数量
     */
    List<QuestionVO> selectQuestions(Long userId, Integer moduleType, int count);

    /**
     * 获取定级测试题（20题: 12词汇+8语法，难度均匀分布1~8）
     */
    List<QuestionVO> getAssessQuestions();

    /**
     * 单题详情（不返回答案）
     */
    QuestionVO getQuestionById(Long questionId, boolean showAnswer);

    /**
     * 批量取题（含答案，供learning-engine调用）
     */
    List<QuestionVO> getQuestionsBatch(List<Long> questionIds);

    /**
     * 提交答案并判分
     */
    SubmitResponse submitAnswer(SubmitRequest request);

    /**
     * 错题本
     */
    List<QuestionVO> getErrorBook(Long userId);

    /**
     * 知识点树
     */
    List<SysKnowledgePoint> getKnowledgePoints(String cetLevel);
}
