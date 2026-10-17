package com.english.ai.exercise.service.impl;

import com.english.ai.exercise.dto.JudgmentResult;
import com.english.ai.exercise.dto.SubmitRequest;
import com.english.ai.exercise.entity.QuestionBuggyPath;
import com.english.ai.exercise.entity.QuestionInfo;
import com.english.ai.exercise.mapper.QuestionBuggyPathMapper;
import com.english.ai.exercise.mapper.QuestionInfoMapper;
import com.english.ai.exercise.service.JudgmentEngine;
import com.english.ai.exercise.util.GrammarRuleChecker;
import com.english.ai.exercise.util.LevenshteinUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgmentEngineImpl implements JudgmentEngine {

    private final QuestionBuggyPathMapper buggyPathMapper;
    private final QuestionInfoMapper questionInfoMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${service.ai.url}")
    private String aiServiceUrl;

    @Override
    public JudgmentResult judge(SubmitRequest request, String correctAnswer, String context) {
        String userAnswer = request.getUserAnswer() != null ? request.getUserAnswer().trim().toLowerCase() : "";
        String normCorrect = correctAnswer != null ? correctAnswer.trim().toLowerCase() : "";

        if (userAnswer.isEmpty()) {
            return JudgmentResult.builder()
                    .isCorrect(false)
                    .heuristicHint("请输入答案")
                    .errorType("EMPTY")
                    .matchedLevel(1)
                    .suggestions(List.of())
                    .build();
        }

        if (userAnswer.equals(normCorrect)) {
            return JudgmentResult.builder()
                    .isCorrect(true)
                    .heuristicHint("回答正确！")
                    .matchedLevel(1)
                    .suggestions(List.of())
                    .build();
        }

        JudgmentResult level1Result = level1RuleEngine(userAnswer, normCorrect, context);
        if (level1Result != null) {
            log.debug("判题引擎：层级1规则算法层匹配成功, traceId={}", 
                    request.getUserId() + "-" + request.getQuestionId());
            return level1Result;
        }

        JudgmentResult level2Result = level2KnowledgeBase(request.getQuestionId(), userAnswer, normCorrect);
        if (level2Result != null) {
            log.debug("判题引擎：层级2结构化题库层匹配成功, traceId={}", 
                    request.getUserId() + "-" + request.getQuestionId());
            return level2Result;
        }

        // ====== Level 2.5：语义可接受性判对（仅词汇题生效） ======
        // 语法连词成句题（moduleType=2）不经过语义判对，避免 AI 放水
        if (request.getModuleType() == null || request.getModuleType() != 2) {
            JudgmentResult semanticResult = level2_5SemanticAccept(
                    request.getQuestionId(), userAnswer, normCorrect, context);
            if (semanticResult != null) {
                log.info("判题引擎：层级2.5语义判对层匹配成功（语义可接受）, traceId={}, altAnswer={}",
                        request.getUserId() + "-" + request.getQuestionId(), userAnswer);
                return semanticResult;
            }
        }

        JudgmentResult level3Result = level3AiFallback(request.getQuestionId(), userAnswer, normCorrect, context);
        log.debug("判题引擎：层级3AI兜底层执行完成, traceId={}", 
                request.getUserId() + "-" + request.getQuestionId());
        return level3Result;
    }

    private JudgmentResult level1RuleEngine(String userAnswer, String correctAnswer, String context) {
        List<String> suggestions = new ArrayList<>();

        String spellingHint = LevenshteinUtil.generateSpellingHint(userAnswer, correctAnswer);
        if (spellingHint != null) {
            suggestions.add(spellingHint);
            return JudgmentResult.builder()
                    .isCorrect(false)
                    .heuristicHint(spellingHint)
                    .errorType("SPELLING")
                    .matchedLevel(1)
                    .suggestions(suggestions)
                    .build();
        }

        String grammarHint = GrammarRuleChecker.checkGrammarRules(userAnswer, context);
        if (grammarHint != null) {
            suggestions.add(grammarHint);
        }

        String prepositionHint = GrammarRuleChecker.checkPreposition(userAnswer);
        if (prepositionHint != null) {
            suggestions.add(prepositionHint);
            return JudgmentResult.builder()
                    .isCorrect(false)
                    .heuristicHint(prepositionHint)
                    .errorType("PREPOSITION")
                    .matchedLevel(1)
                    .suggestions(suggestions)
                    .build();
        }

        String tenseHint = GrammarRuleChecker.checkTense(userAnswer, context);
        if (tenseHint != null) {
            suggestions.add(tenseHint);
            return JudgmentResult.builder()
                    .isCorrect(false)
                    .heuristicHint(tenseHint)
                    .errorType("TENSE")
                    .matchedLevel(1)
                    .suggestions(suggestions)
                    .build();
        }

        if (!suggestions.isEmpty()) {
            return JudgmentResult.builder()
                    .isCorrect(false)
                    .heuristicHint(suggestions.get(0))
                    .errorType("GRAMMAR")
                    .matchedLevel(1)
                    .suggestions(suggestions)
                    .build();
        }

        return null;
    }

    private JudgmentResult level2KnowledgeBase(Long questionId, String userAnswer, String correctAnswer) {
        QuestionBuggyPath buggyPath = buggyPathMapper.findByQuestionAndAnswer(questionId, userAnswer);
        
        if (buggyPath != null) {
            buggyPath.setOccurrenceCount(buggyPath.getOccurrenceCount() + 1);
            buggyPathMapper.updateById(buggyPath);
            
            List<String> suggestions = new ArrayList<>();
            suggestions.add(buggyPath.getHeuristicHint());
            
            return JudgmentResult.builder()
                    .isCorrect(false)
                    .heuristicHint(buggyPath.getHeuristicHint())
                    .errorType(buggyPath.getErrorType())
                    .matchedLevel(2)
                    .suggestions(suggestions)
                    .build();
        }

        double similarity = LevenshteinUtil.calculateSimilarity(userAnswer, correctAnswer);
        if (similarity >= 0.8) {
            String hint = "你的答案与正确答案非常接近！再仔细检查一下拼写或语法细节。";
            return JudgmentResult.builder()
                    .isCorrect(false)
                    .heuristicHint(hint)
                    .errorType("SIMILAR")
                    .matchedLevel(2)
                    .suggestions(List.of(hint))
                    .build();
        }

        return null;
    }

    // ====== Level 2.5：语义可接受性判对 ======

    /**
     * 语义可接受性判对层：
     * 1. 先查 question_info.accepted_answers 是否有缓存命中
     * 2. 缓存未命中则调用 AI 服务 /ai/judge-acceptable 判断
     * 3. AI 判定可接受后，写回 accepted_answers 实现自学习闭环
     * 4. 返回 isCorrect=true 表示语义可接受，给分
     *
     * @return null 表示语义不可接受（交给 Level 3 继续处理），非 null 表示判对结果
     */
    private JudgmentResult level2_5SemanticAccept(Long questionId, String userAnswer, 
                                                   String correctAnswer, String context) {
        // 1. 查题目的 accepted_answers 缓存
        QuestionInfo info = questionInfoMapper.selectById(questionId);
        if (info == null) return null;

        String acceptedAnswersJson = info.getAcceptedAnswers();
        if (acceptedAnswersJson != null && !acceptedAnswersJson.isBlank()) {
            try {
                List<String> acceptedList = objectMapper.readValue(
                        acceptedAnswersJson, new TypeReference<List<String>>() {});
                if (acceptedList.contains(userAnswer)) {
                    log.info("语义判对缓存命中: questionId={}, answer={}", questionId, userAnswer);
                    return JudgmentResult.builder()
                            .isCorrect(true)
                            .heuristicHint("你的答案 \"" + userAnswer + "\" 也可以用在这里，但最适合这道题的词是 \"" + correctAnswer + "\"。")
                            .errorType(null)
                            .matchedLevel(2)
                            .suggestions(List.of("近义词替换，语义可接受"))
                            .isSemanticMatch(true)
                            .build();
                }
            } catch (Exception e) {
                log.warn("解析 accepted_answers JSON 失败: questionId={}, json={}", questionId, acceptedAnswersJson);
            }
        }

        // 2. 缓存未命中，调 AI 服务进行语义判对
        boolean aiAcceptable = false;
        String aiReason = "";
        String aiAltAnswer = "";
        try {
            String url = aiServiceUrl + "/ai/judge-acceptable";
            Map<String, Object> body = new HashMap<>();
            body.put("userAnswer", userAnswer);
            body.put("correctAnswer", correctAnswer);
            body.put("context", context);
            body.put("questionId", questionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            if (resp.getBody() != null) {
                Object acceptableObj = resp.getBody().get("acceptable");
                Object reasonObj = resp.getBody().get("reason");
                Object altObj = resp.getBody().get("altAnswer");

                if (acceptableObj instanceof Boolean) {
                    aiAcceptable = (Boolean) acceptableObj;
                }
                if (reasonObj != null) aiReason = reasonObj.toString();
                if (altObj != null) aiAltAnswer = altObj.toString();
            }
        } catch (Exception e) {
            log.warn("调用 AI 语义判对服务失败: {}", e.getMessage());
            return null; // AI 不可用，不判对，交给 Level 3
        }

        // 3. AI 判定可接受 → 写回 accepted_answers 实现自学习
        if (aiAcceptable) {
            saveAcceptedAnswer(questionId, userAnswer, aiAltAnswer);
            return JudgmentResult.builder()
                    .isCorrect(true)
                    .heuristicHint("你的答案 \"" + userAnswer + "\" 也可以用在这里，但最适合这道题的词是 \"" + correctAnswer + "\"。"
                            + (aiReason.isEmpty() ? "" : "（" + aiReason + "）"))
                    .errorType(null)
                    .matchedLevel(2)
                    .suggestions(List.of("近义词/同义表达，语义可接受"))
                    .isSemanticMatch(true)
                    .build();
        }

        return null; // 语义不可接受，交给 Level 3 继续处理
    }

    /**
     * 将 AI 判定可接受的答案写入 question_info.accepted_answers
     * 实现自学习闭环：下次同一答案直接缓存命中，不再消耗 AI 调用
     */
    private void saveAcceptedAnswer(Long questionId, String userAnswer, String altAnswer) {
        try {
            QuestionInfo info = questionInfoMapper.selectById(questionId);
            if (info == null) return;

            List<String> acceptedList = new ArrayList<>();
            String existing = info.getAcceptedAnswers();
            if (existing != null && !existing.isBlank()) {
                try {
                    acceptedList = objectMapper.readValue(existing, new TypeReference<List<String>>() {});
                } catch (Exception e) {
                    log.warn("解析已有 accepted_answers 失败，重新初始化: {}", e.getMessage());
                    acceptedList = new ArrayList<>();
                }
            }

            // 使用 AI 返回的 altAnswer（优先）或用户原始答案
            String answerToSave = (altAnswer != null && !altAnswer.isBlank()) ? altAnswer.trim().toLowerCase() : userAnswer;
            if (!acceptedList.contains(answerToSave)) {
                acceptedList.add(answerToSave);
                info.setAcceptedAnswers(objectMapper.writeValueAsString(acceptedList));
                info.setAiGenerated(1);
                questionInfoMapper.updateById(info);
                log.info("自学习：已保存语义可接受答案 questionId={}, answer={}, total={}", 
                        questionId, answerToSave, acceptedList.size());
            }
        } catch (Exception e) {
            log.warn("保存 accepted_answers 失败: questionId={}, error={}", questionId, e.getMessage());
        }
    }

    private JudgmentResult level3AiFallback(Long questionId, String userAnswer, String correctAnswer, String context) {
        String aiHint = null;
        String errorType = "UNKNOWN";
        List<String> suggestions = new ArrayList<>();

        try {
            String url = aiServiceUrl + "/ai/judge-feedback";
            Map<String, Object> body = new HashMap<>();
            body.put("userAnswer", userAnswer);
            body.put("correctAnswer", correctAnswer);
            body.put("context", context);
            body.put("questionId", questionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            if (resp.getBody() != null) {
                Object hintObj = resp.getBody().get("hint");
                Object typeObj = resp.getBody().get("errorType");
                Object suggestionsObj = resp.getBody().get("suggestions");

                if (hintObj != null) {
                    aiHint = hintObj.toString();
                }
                if (typeObj != null) {
                    errorType = typeObj.toString();
                }
                if (suggestionsObj instanceof List) {
                    ((List<?>) suggestionsObj).forEach(item -> suggestions.add(item.toString()));
                }
            }
        } catch (Exception e) {
            log.warn("调用AI服务生成启发反馈失败: {}", e.getMessage());
        }

        if (aiHint == null || aiHint.isEmpty()) {
            aiHint = "这个答案不太对哦，再想一想这个词的正确用法是什么？";
        }

        if (suggestions.isEmpty()) {
            suggestions.add(aiHint);
        }

        saveBuggyPath(questionId, userAnswer, aiHint, errorType);

        return JudgmentResult.builder()
                .isCorrect(false)
                .heuristicHint(aiHint)
                .errorType(errorType)
                .matchedLevel(3)
                .suggestions(suggestions)
                .build();
    }

    private void saveBuggyPath(Long questionId, String wrongAnswer, String hint, String errorType) {
        try {
            QuestionBuggyPath exist = buggyPathMapper.findByQuestionAndAnswer(questionId, wrongAnswer);
            if (exist == null) {
                QuestionBuggyPath newPath = new QuestionBuggyPath();
                newPath.setQuestionId(questionId);
                newPath.setWrongAnswer(wrongAnswer);
                newPath.setHeuristicHint(hint);
                newPath.setErrorType(errorType);
                newPath.setOccurrenceCount(1);
                newPath.setIsAiGenerated(1);
                buggyPathMapper.insert(newPath);
                log.debug("新错误路径已保存: questionId={}, wrongAnswer={}", questionId, wrongAnswer);
            }
        } catch (Exception e) {
            log.warn("保存BuggyPath失败: {}", e.getMessage());
        }
    }
}