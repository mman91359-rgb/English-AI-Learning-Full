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
import com.english.ai.exercise.dto.WeakKnowledgePoint;
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

    /**
     * 智能添加单词：仅传入单词，由 AI 自动补全音标/词性/释义/例句/搭配
     * 并初始化艾宾浩斯复习计划（next_review_at = 明天）
     */
    VocabularyVO addVocabularySmart(Long userId, String word, String source);

    /** 单词本列表 */
    List<VocabularyVO> getVocabulary(Long userId);

    /** 删除单词 */
    void removeVocabulary(Long id, Long userId);

    /** 切换掌握状态 */
    VocabularyVO toggleMastered(Long id, Long userId);

    /**
     * 获取今日待复习单词列表
     * 规则：next_review_at <= 现在 且 isMastered = 0
     */
    List<VocabularyVO> getReviewList(Long userId);

    /**
     * 提交单词复习结果
     * @param id 单词记录ID
     * @param userId 用户ID
     * @param isCorrect 是否记住
     * @return 更新后的单词记录
     */
    VocabularyVO reviewWord(Long id, Long userId, Boolean isCorrect);

    // ========== 学习历史 ==========

    /** 答题历史（分页） */
    List<ExerciseHistoryVO> getExerciseHistory(Long userId, Integer page, Integer size, Integer moduleType, Integer isCorrect, String startTime);

    /**
     * 获取学习历史总数
     */
    long getExerciseHistoryCount(Long userId, Integer moduleType, Integer isCorrect, String startTime);

    // ========== 错题驱动同类题生成 ==========

    /**
     * 获取用户薄弱知识点列表
     * 查询用户所有未解决错题，统计每个知识点出现次数，按次数降序返回前10个
     *
     * @param userId 用户ID
     * @return 薄弱知识点列表（按错误次数降序）
     */
    List<WeakKnowledgePoint> getWeakKnowledgePoints(Long userId);

    /**
     * 根据错题生成同类强化练习题
     * 优先从正式题库中取同知识点、同难度、同题型的题目，
     * 不足部分通过 AI 生成并直接入库（无需审核，标记为PENDING）
     *
     * @param userId     用户ID
     * @param questionId 错题ID
     * @param count      需要生成的题目数量
     * @return 同类题目列表（供前端立即练习）
     */
    List<QuestionVO> generateSimilarQuestions(Long userId, Long questionId, int count);
}
