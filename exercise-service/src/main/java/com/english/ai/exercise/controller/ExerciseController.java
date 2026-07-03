package com.english.ai.exercise.controller;

import com.english.ai.common.Result;
import com.english.ai.exercise.dto.*;
import com.english.ai.exercise.entity.SysKnowledgePoint;
import com.english.ai.exercise.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 练习服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/exercise")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    /**
     * 智能选题
     * GET /exercise/questions?userId=1&moduleType=1&count=10
     */
    @GetMapping("/questions")
    public Result<List<QuestionVO>> selectQuestions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer moduleType,
            @RequestParam(defaultValue = "10") Integer count) {
        if (count > 50) count = 50;
        List<QuestionVO> questions = exerciseService.selectQuestions(userId, moduleType, count);
        log.info("智能选题: userId={}, moduleType={}, count={}, 实际返回{}题", userId, moduleType, count, questions.size());
        return Result.success(questions);
    }

    /**
     * 定级测试题
     * GET /exercise/assess-questions
     */
    @GetMapping("/assess-questions")
    public Result<List<QuestionVO>> getAssessQuestions() {
        List<QuestionVO> questions = exerciseService.getAssessQuestions();
        log.info("定级测试: 返回{}题", questions.size());
        return Result.success(questions);
    }

    /**
     * 单题详情
     * GET /exercise/question/{id}?showAnswer=false
     */
    @GetMapping("/question/{id}")
    public Result<QuestionVO> getQuestion(@PathVariable Long id,
                                          @RequestParam(defaultValue = "false") boolean showAnswer) {
        QuestionVO vo = exerciseService.getQuestionById(id, showAnswer);
        if (vo == null) return Result.error(404, "题目不存在");
        return Result.success(vo);
    }

    /**
     * 批量取题（含答案）
     * GET /exercise/questions/batch?ids=1,2,3
     */
    @GetMapping("/questions/batch")
    public Result<List<QuestionVO>> getQuestionsBatch(@RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        List<QuestionVO> questions = exerciseService.getQuestionsBatch(idList);
        return Result.success(questions);
    }

    /**
     * 提交答案
     * POST /exercise/submit
     */
    @PostMapping("/submit")
    public Result<SubmitResponse> submit(@RequestBody SubmitRequest request) {
        if (request.getUserId() == null || request.getQuestionId() == null) {
            return Result.badRequest("userId和questionId不能为空");
        }
        try {
            SubmitResponse response = exerciseService.submitAnswer(request);
            return Result.success(response);
        } catch (RuntimeException e) {
            log.warn("提交失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 错题本
     * GET /exercise/error-book/{userId}
     */
    @GetMapping("/error-book/{userId}")
    public Result<List<QuestionVO>> errorBook(@PathVariable Long userId) {
        List<QuestionVO> errors = exerciseService.getErrorBook(userId);
        return Result.success(errors);
    }

    /**
     * 知识点树
     * GET /exercise/knowledge-points?cetLevel=CET4
     */
    @GetMapping("/knowledge-points")
    public Result<List<SysKnowledgePoint>> knowledgePoints(@RequestParam(defaultValue = "CET4") String cetLevel) {
        List<SysKnowledgePoint> points = exerciseService.getKnowledgePoints(cetLevel);
        return Result.success(points);
    }

    /**
     * 每日单词
     * GET /exercise/daily-word?level=CET4
     */
    @GetMapping("/daily-word")
    public Result<DailyWordVO> getDailyWord(@RequestParam(defaultValue = "CET4") String level) {
        DailyWordVO vo = exerciseService.getDailyWord(level);
        log.info("每日单词: level={}, word={}", level, vo.getWord());
        return Result.success(vo);
    }

    /**
     * 用户答题统计（热力图、学习时间、词汇量等）
     * GET /exercise/stats/{userId}
     */
    @GetMapping("/stats/{userId}")
    public Result<UserExerciseStats> getUserStats(@PathVariable Long userId) {
        UserExerciseStats stats = exerciseService.getUserStats(userId);
        log.info("用户答题统计: userId={}, 总答题={}, 正确={}, 活跃天数={}",
                userId, stats.getTotalAnswered(), stats.getCorrectCount(), stats.getActiveDays());
        return Result.success(stats);
    }

    // ========== 收藏句 ==========

    /** POST /exercise/favorites */
    @PostMapping("/favorites")
    public Result<FavoriteVO> addFavorite(@RequestBody AddFavoriteRequest request) {
        if (request.getUserId() == null || request.getSentenceText() == null || request.getSentenceText().isBlank()) {
            return Result.badRequest("userId和sentenceText不能为空");
        }
        FavoriteVO vo = exerciseService.addFavorite(request.getUserId(), request.getSentenceText(), request.getQuestionId());
        return Result.success(vo);
    }

    /** GET /exercise/favorites/{userId} */
    @GetMapping("/favorites/{userId}")
    public Result<List<FavoriteVO>> getFavorites(@PathVariable Long userId) {
        return Result.success(exerciseService.getFavorites(userId));
    }

    /** DELETE /exercise/favorites/{id}?userId=1 */
    @DeleteMapping("/favorites/{id}")
    public Result<String> removeFavorite(@PathVariable Long id, @RequestParam Long userId) {
        exerciseService.removeFavorite(id, userId);
        return Result.success("已删除");
    }

    // ========== 单词本 ==========

    /** POST /exercise/vocabulary */
    @PostMapping("/vocabulary")
    public Result<VocabularyVO> addVocabulary(@RequestBody AddVocabularyRequest request) {
        if (request.getUserId() == null || request.getWord() == null || request.getWord().isBlank()) {
            return Result.badRequest("userId和word不能为空");
        }
        VocabularyVO vo = exerciseService.addVocabulary(request.getUserId(), request.getWord(), request.getTranslation());
        return Result.success(vo);
    }

    /**
     * 智能添加单词：只传 userId 和 word，AI 自动补全音标/词性/释义/例句/搭配
     * 并初始化艾宾浩斯复习计划
     * POST /exercise/vocabulary/smart
     */
    @PostMapping("/vocabulary/smart")
    public Result<VocabularyVO> addVocabularySmart(@RequestBody AddVocabularyRequest request) {
        if (request.getUserId() == null || request.getWord() == null || request.getWord().isBlank()) {
            return Result.badRequest("userId和word不能为空");
        }
        try {
            VocabularyVO vo = exerciseService.addVocabularySmart(
                    request.getUserId(), request.getWord(), request.getSource());
            return Result.success(vo);
        } catch (RuntimeException e) {
            log.warn("智能添加单词失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /** GET /exercise/vocabulary/{userId} */
    @GetMapping("/vocabulary/{userId}")
    public Result<List<VocabularyVO>> getVocabulary(@PathVariable Long userId) {
        return Result.success(exerciseService.getVocabulary(userId));
    }

    /** DELETE /exercise/vocabulary/{id}?userId=1 */
    @DeleteMapping("/vocabulary/{id}")
    public Result<String> removeVocabulary(@PathVariable Long id, @RequestParam Long userId) {
        exerciseService.removeVocabulary(id, userId);
        return Result.success("已删除");
    }

    /** PUT /exercise/vocabulary/{id}/toggle?userId=1 */
    @PutMapping("/vocabulary/{id}/toggle")
    public Result<VocabularyVO> toggleMastered(@PathVariable Long id, @RequestParam Long userId) {
        VocabularyVO vo = exerciseService.toggleMastered(id, userId);
        return Result.success(vo);
    }

    /**
     * 获取今日待复习单词列表
     * GET /exercise/vocabulary/review/{userId}
     */
    @GetMapping("/vocabulary/review/{userId}")
    public Result<List<VocabularyVO>> getReviewList(@PathVariable Long userId) {
        List<VocabularyVO> list = exerciseService.getReviewList(userId);
        log.info("今日待复习: userId={}, 数量={}", userId, list.size());
        return Result.success(list);
    }

    /**
     * 提交单词复习结果（记住/没记住）
     * PUT /exercise/vocabulary/{id}/review
     */
    @PutMapping("/vocabulary/{id}/review")
    public Result<VocabularyVO> reviewWord(@PathVariable Long id, @RequestBody ReviewResultRequest request) {
        if (request.getUserId() == null || request.getIsCorrect() == null) {
            return Result.badRequest("userId和isCorrect不能为空");
        }
        try {
            VocabularyVO vo = exerciseService.reviewWord(id, request.getUserId(), request.getIsCorrect());
            return Result.success(vo);
        } catch (RuntimeException e) {
            log.warn("提交复习结果失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    // ========== 学习历史 ==========

    /** GET /exercise/history/{userId}?page=1&size=20&moduleType=1&isCorrect=1&startTime=2024-01-01T00:00:00 */
    @GetMapping("/history/{userId}")
    public Result<Map<String, Object>> getExerciseHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer moduleType,
            @RequestParam(required = false) Integer isCorrect,
            @RequestParam(required = false) String startTime) {
        List<ExerciseHistoryVO> list = exerciseService.getExerciseHistory(userId, page, size, moduleType, isCorrect, startTime);
        long total = exerciseService.getExerciseHistoryCount(userId, moduleType, isCorrect, startTime);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    // ========== 错题驱动同类题生成 ==========

    /**
     * 获取用户薄弱知识点列表
     * GET /exercise/weak-points/{userId}
     * 统计用户所有未解决错题中每个知识点的出现次数，按次数降序返回前10个
     */
    @GetMapping("/weak-points/{userId}")
    public Result<List<WeakKnowledgePoint>> getWeakPoints(@PathVariable Long userId) {
        log.info("获取薄弱知识点: userId={}", userId);
        List<WeakKnowledgePoint> points = exerciseService.getWeakKnowledgePoints(userId);
        return Result.success(points);
    }

    /**
     * 生成同类强化练习题
     * POST /exercise/generate-similar
     * 请求体: { "userId": 1, "questionId": 100, "count": 5 }
     * 优先取正式题库中同知识点题目，不足部分由 AI 生成
     */
    @PostMapping("/generate-similar")
    public Result<List<QuestionVO>> generateSimilar(@RequestBody Map<String, Object> request) {
        Long userId = request.get("userId") != null
                ? ((Number) request.get("userId")).longValue()
                : null;
        Long questionId = request.get("questionId") != null
                ? ((Number) request.get("questionId")).longValue()
                : null;
        int count = request.get("count") != null
                ? ((Number) request.get("count")).intValue()
                : 5;

        if (userId == null || questionId == null) {
            return Result.badRequest("userId和questionId不能为空");
        }
        if (count > 20) count = 20;

        log.info("生成同类题: userId={}, questionId={}, count={}", userId, questionId, count);
        try {
            List<QuestionVO> questions = exerciseService.generateSimilarQuestions(userId, questionId, count);
            return Result.success(questions);
        } catch (RuntimeException e) {
            log.warn("生成同类题失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
