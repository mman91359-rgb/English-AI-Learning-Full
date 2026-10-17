package com.english.ai.exercise.controller;

import com.english.ai.common.Result;
import com.english.ai.exercise.entity.QuestionPending;
import com.english.ai.exercise.service.QuestionGenerateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 题目生成与审核管理控制器
 * 提供题目的 AI 批量生成、待审核列表查询、审核通过/驳回等功能
 */
@Slf4j
@RestController
@RequestMapping("/exercise")
@RequiredArgsConstructor
public class QuestionGenerateController {

    private final QuestionGenerateService questionGenerateService;

    /**
     * AI 批量生成题目
     * POST /exercise/generate-questions
     * 请求体参数：
     *   - knowledgePoint: 知识点名称（必填）
     *   - cetLevel: 目标等级，默认 CET4
     *   - difficulty: 难度等级 1~8，默认 3
     *   - questionType: 题型，默认 FILL_BLANK
     *   - count: 生成数量，上限 20，默认 5
     * 返回保存到待审核表的题目数量
     */
    @PostMapping("/generate-questions")
    public Result<Integer> generateQuestions(@RequestBody Map<String, Object> request) {
        String knowledgePoint = (String) request.get("knowledgePoint");
        if (knowledgePoint == null || knowledgePoint.isBlank()) {
            return Result.badRequest("知识点名称不能为空");
        }

        String cetLevel = (String) request.getOrDefault("cetLevel", "CET4");
        int difficulty = ((Number) request.getOrDefault("difficulty", 3)).intValue();
        String questionType = (String) request.getOrDefault("questionType", "FILL_BLANK");
        int count = ((Number) request.getOrDefault("count", 5)).intValue();
        if (count > 20) count = 20;

        log.info("触发AI出题: knowledgePoint={}, cetLevel={}, difficulty={}, questionType={}, count={}",
                knowledgePoint, cetLevel, difficulty, questionType, count);

        int savedCount = questionGenerateService.generateAndSave(
                knowledgePoint, cetLevel, difficulty, questionType, count);
        return Result.success(savedCount);
    }

    /**
     * 查询待审核题目列表
     * GET /exercise/pending-questions?status=PENDING
     *
     * @param status 状态筛选：PENDING/APPROVED/REJECTED，不传则返回全部
     * @return 待审核题目列表
     */
    @GetMapping("/pending-questions")
    public Result<List<QuestionPending>> getPendingQuestions(
            @RequestParam(required = false) String status) {
        log.info("查询待审核题目列表: status={}", status);
        List<QuestionPending> list = questionGenerateService.getPendingQuestions(status);
        return Result.success(list);
    }

    /**
     * 审核通过某道待审核题目
     * POST /exercise/pending-questions/{id}/approve
     * 请求体: { "reviewerId": 1 }
     *
     * @param id        待审核题目ID
     * @param request   请求体，包含审核人ID
     * @return 操作结果
     */
    @PostMapping("/pending-questions/{id}/approve")
    public Result<String> approveQuestion(@PathVariable Long id,
                                          @RequestBody Map<String, Object> request) {
        Long reviewerId = request.get("reviewerId") != null
                ? ((Number) request.get("reviewerId")).longValue()
                : 1L;

        log.info("审核通过题目: pendingId={}, reviewerId={}", id, reviewerId);
        boolean success = questionGenerateService.approveQuestion(id, reviewerId);
        if (success) {
            return Result.success("审核通过，题目已正式入库");
        } else {
            return Result.badRequest("审核失败，题目不存在");
        }
    }

    /**
     * 驳回某道待审核题目
     * POST /exercise/pending-questions/{id}/reject
     * 请求体: { "reviewerId": 1, "comment": "题干有误" }
     *
     * @param id        待审核题目ID
     * @param request   请求体，包含审核人ID和驳回意见
     * @return 操作结果
     */
    @PostMapping("/pending-questions/{id}/reject")
    public Result<String> rejectQuestion(@PathVariable Long id,
                                         @RequestBody Map<String, Object> request) {
        Long reviewerId = request.get("reviewerId") != null
                ? ((Number) request.get("reviewerId")).longValue()
                : 1L;
        String comment = (String) request.getOrDefault("comment", "");

        log.info("驳回题目: pendingId={}, reviewerId={}, comment={}", id, reviewerId, comment);
        boolean success = questionGenerateService.rejectQuestion(id, reviewerId, comment);
        if (success) {
            return Result.success("已驳回");
        } else {
            return Result.badRequest("驳回失败，题目不存在");
        }
    }
}