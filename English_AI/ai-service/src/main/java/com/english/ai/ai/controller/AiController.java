package com.english.ai.ai.controller;

import com.english.ai.ai.dto.ChatRequest;
import com.english.ai.ai.dto.JudgeAcceptableRequest;
import com.english.ai.ai.dto.JudgeFeedbackRequest;
import com.english.ai.ai.dto.WordDetailVO;
import com.english.ai.ai.service.AiService;
import com.english.ai.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * AI对话（SSE流式）
     * POST /ai/chat
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody ChatRequest request) {
        log.info("AI聊天请求: contextType={}, message={}", request.getContextType(),
                request.getMessage() != null ? request.getMessage().substring(0, Math.min(50, request.getMessage().length())) : "");
        return aiService.chat(request);
    }

    /**
     * 口语/作文评分（SSE流式）
     * POST /ai/score
     */
    @PostMapping("/score")
    public SseEmitter score(@RequestBody ChatRequest request) {
        log.info("AI评分请求: scoreType={}", request.getScoreType());
        if ("speaking".equals(request.getScoreType())) {
            request.setContextType("speaking");
        } else if ("writing".equals(request.getScoreType())) {
            request.setContextType("writing");
        }
        return aiService.chat(request);
    }

    /**
     * AI 生成单词详情（同步，非流式）
     * GET /ai/word-detail?word=hello
     * 返回音标、词性、释义、例句、搭配等结构化信息，供单词本智能化使用
     */
    @GetMapping("/word-detail")
    public Result<WordDetailVO> wordDetail(@RequestParam String word) {
        log.info("生成单词详情: word={}", word);
        WordDetailVO vo = aiService.generateWordDetail(word);
        if (vo == null) {
            return Result.badRequest("单词不能为空");
        }
        return Result.success(vo);
    }

    /**
     * AI 判题反馈（同步，非流式）
     * POST /ai/judge-feedback
     * 由 exercise-service 三级判题引擎 Level 3 调用，
     * 根据用户错误答案生成启发式提示
     */
    @PostMapping("/judge-feedback")
    public Map<String, Object> judgeFeedback(@RequestBody JudgeFeedbackRequest request) {
        log.info("判题反馈请求: questionId={}, userAnswer={}",
                request.getQuestionId(),
                request.getUserAnswer() != null ? request.getUserAnswer().substring(0, Math.min(30, request.getUserAnswer().length())) : "");
        return aiService.judgeFeedback(
                request.getUserAnswer(),
                request.getCorrectAnswer(),
                request.getContext());
    }

    /**
     * AI 语义可接受性判对（同步，非流式）
     * POST /ai/judge-acceptable
     * 由 exercise-service 判题引擎 Level 2.5 调用，
     * 判断用户答案是否语义可接受（近义词、同义表达等）
     * 返回 {"acceptable":true/false, "reason":"...", "altAnswer":"..."}
     */
    @PostMapping("/judge-acceptable")
    public Map<String, Object> judgeAcceptable(@RequestBody JudgeAcceptableRequest request) {
        log.info("语义判对请求: questionId={}, userAnswer={}, correctAnswer={}",
                request.getQuestionId(),
                request.getUserAnswer() != null ? request.getUserAnswer().substring(0, Math.min(30, request.getUserAnswer().length())) : "",
                request.getCorrectAnswer());
        return aiService.judgeAcceptable(
                request.getUserAnswer(),
                request.getCorrectAnswer(),
                request.getContext());
    }

    /**
     * AI 批量生成题目（同步，非流式）
     * POST /ai/generate-questions
     * 请求体参数：
     *   - knowledgePoint: 知识点名称（必填）
     *   - cetLevel: 目标等级，默认 CET4
     *   - difficulty: 难度等级 1~8，默认 3
     *   - questionType: 题型 FILL_BLANK/SENTENCE_MAKING，默认 FILL_BLANK
     *   - count: 生成数量，上限 20，默认 5
     * 返回 AI 生成的题目 JSON 数组字符串
     */
    @PostMapping("/generate-questions")
    public Result<String> generateQuestions(@RequestBody Map<String, Object> request) {
        String knowledgePoint = (String) request.get("knowledgePoint");
        String cetLevel = (String) request.getOrDefault("cetLevel", "CET4");
        int difficulty = ((Number) request.getOrDefault("difficulty", 3)).intValue();
        String questionType = (String) request.getOrDefault("questionType", "FILL_BLANK");
        int count = ((Number) request.getOrDefault("count", 5)).intValue();
        // 数量上限保护
        if (count > 20) count = 20;

        log.info("AI批量生成题目: knowledgePoint={}, cetLevel={}, difficulty={}, questionType={}, count={}",
                knowledgePoint, cetLevel, difficulty, questionType, count);
        String json = aiService.generateQuestions(knowledgePoint, cetLevel, difficulty, questionType, count);
        return Result.success(json);
    }
}
