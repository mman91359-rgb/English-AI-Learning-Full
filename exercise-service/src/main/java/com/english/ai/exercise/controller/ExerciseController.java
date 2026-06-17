package com.english.ai.exercise.controller;

import com.english.ai.common.Result;
import com.english.ai.exercise.dto.QuestionVO;
import com.english.ai.exercise.dto.SubmitRequest;
import com.english.ai.exercise.dto.SubmitResponse;
import com.english.ai.exercise.entity.SysKnowledgePoint;
import com.english.ai.exercise.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
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
}
