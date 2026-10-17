package com.english.ai.learning.controller;

import com.english.ai.common.Result;
import com.english.ai.learning.dto.ReviewTaskVO;
import com.english.ai.learning.dto.UpdateCurveRequest;
import com.english.ai.learning.service.LearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学习引擎控制器
 */
@Slf4j
@RestController
@RequestMapping("/learning")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;

    /**
     * 获取每日复习任务
     * GET /learning/review-tasks/{userId}
     */
    @GetMapping("/review-tasks/{userId}")
    public Result<List<ReviewTaskVO>> reviewTasks(@PathVariable Long userId) {
        List<ReviewTaskVO> tasks = learningService.getReviewTasks(userId);
        log.info("复习任务查询: userId={}, 到期{}题", userId, tasks.size());
        return Result.success(tasks);
    }

    /**
     * 更新遗忘曲线（由exercise-service调用）
     * POST /learning/update-curve
     */
    @PostMapping("/update-curve")
    public Result<String> updateCurve(@RequestBody UpdateCurveRequest request) {
        if (request.getUserId() == null || request.getQuestionId() == null) {
            return Result.badRequest("userId和questionId不能为空");
        }
        learningService.updateCurve(request);
        return Result.success("ok");
    }

    /**
     * 遗忘曲线统计
     * GET /learning/curve-stats/{userId}
     */
    @GetMapping("/curve-stats/{userId}")
    public Result<Map<String, Object>> curveStats(@PathVariable Long userId) {
        Map<String, Object> stats = learningService.getCurveStats(userId);
        return Result.success(stats);
    }
}
