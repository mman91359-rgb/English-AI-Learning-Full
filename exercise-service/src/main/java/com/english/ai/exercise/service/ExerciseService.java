package com.english.ai.exercise.service;

import com.english.ai.exercise.dto.QuestionVO;
import com.english.ai.exercise.dto.SubmitRequest;
import com.english.ai.exercise.dto.SubmitResponse;
import com.english.ai.exercise.dto.ReviewTaskVO;
import com.english.ai.exercise.dto.UserExerciseStats;
import com.english.ai.exercise.dto.FavoriteVO;
import com.english.ai.exercise.dto.VocabularyVO;
import com.english.ai.exercise.dto.ExerciseHistoryVO;
import com.english.ai.exercise.dto.DailyWordVO;
import com.english.ai.exercise.entity.SysKnowledgePoint;

import java.util.List;

/**
 * 练习服务接口
 */
public interface ExerciseService {

    /**
     * 智能选题
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

    /**
     * 每日单词（全局统一，按日期固定）
     */
    DailyWordVO getDailyWord(String cetLevel);

    /**
     * 用户答题统计（用于热力图、学习时间等）
     */
    UserExerciseStats getUserStats(Long userId);

    // ========== 收藏句 ==========

    /** 添加收藏 */
    FavoriteVO addFavorite(Long userId, String sentenceText, Long questionId);

    /** 收藏列表 */
    List<FavoriteVO> getFavorites(Long userId);

    /** 删除收藏 */
    void removeFavorite(Long id, Long userId);

    // ========== 单词本 ==========

    /** 添加单词 */
    VocabularyVO addVocabulary(Long userId, String word, String translation);

    /** 单词本列表 */
    List<VocabularyVO> getVocabulary(Long userId);

    /** 删除单词 */
    void removeVocabulary(Long id, Long userId);

    /** 切换掌握状态 */
    VocabularyVO toggleMastered(Long id, Long userId);

    // ========== 学习历史 ==========

    /** 答题历史（分页） */
    List<ExerciseHistoryVO> getExerciseHistory(Long userId, Integer page, Integer size);

    /** 历史总数 */
    long getExerciseHistoryCount(Long userId);
}
