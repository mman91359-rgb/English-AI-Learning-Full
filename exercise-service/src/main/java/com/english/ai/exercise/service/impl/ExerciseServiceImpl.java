package com.english.ai.exercise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.english.ai.common.Result;
import com.english.ai.exercise.config.RabbitMQConfig;
import com.english.ai.exercise.dto.QuestionVO;
import com.english.ai.exercise.dto.SubmitRequest;
import com.english.ai.exercise.dto.SubmitResponse;
import com.english.ai.exercise.dto.ReviewTaskVO;
import com.english.ai.exercise.entity.*;
import com.english.ai.exercise.event.ExerciseCompletedEvent;
import com.english.ai.exercise.mapper.*;
import com.english.ai.exercise.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final QuestionInfoMapper questionInfoMapper;
    private final QuestionContentMapper questionContentMapper;
    private final SysKnowledgePointMapper knowledgePointMapper;
    private final KnowledgeMappingMapper knowledgeMappingMapper;
    private final UserExerciseLogMapper exerciseLogMapper;
    private final UserErrorBookMapper errorBookMapper;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${service.user.url}")
    private String userServiceUrl;

    @Value("${service.learning-engine.url}")
    private String learningEngineUrl;

    // ============= 智能选题 =============

    @Override
    public List<QuestionVO> selectQuestions(Long userId, Integer moduleType, int count) {
        // 1. 调 user-service 获取用户 ELO分 和 cetLevel
        int eloScore = 1000;
        String cetLevel = "CET4";
        try {
            String url = userServiceUrl + "/user/info/" + userId;
            ResponseEntity<Result> resp = restTemplate.getForEntity(url, Result.class);
            if (resp.getBody() != null && resp.getBody().getCode() == 200) {
                Map userMap = (Map) resp.getBody().getData();
                if (userMap != null && userMap.get("cetLevel") != null) {
                    cetLevel = userMap.get("cetLevel").toString();
                }
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败(userId={}), 使用默认ELO=1000", userId, e);
        }

        // 试取 user_ability_radar 的 ELO分（如果user-service有对应接口）
        try {
            String url = userServiceUrl + "/user/radar/" + userId;
            ResponseEntity<Result> resp = restTemplate.getForEntity(url, Result.class);
            if (resp.getBody() != null && resp.getBody().getCode() == 200) {
                Map radarMap = (Map) resp.getBody().getData();
                if (radarMap != null && radarMap.get("eloScore") != null) {
                    eloScore = ((Number) radarMap.get("eloScore")).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("获取用户雷达信息失败, 使用默认ELO=1000");
        }

        // 2. 计算难度区间 Δ=200
        int lo = Math.max(1, eloScore - 200);
        int hi = Math.min(2000, eloScore + 200);
        // 映射到 1~8
        int loDifficulty = Math.max(1, lo / 250);
        int hiDifficulty = Math.min(8, hi / 250);

        // 3. 查最近7天已做题目ID
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Long> recentIds = exerciseLogMapper.selectList(
                new LambdaQueryWrapper<UserExerciseLog>()
                        .eq(UserExerciseLog::getUserId, userId)
                        .ge(UserExerciseLog::getCreatedAt, sevenDaysAgo)
        ).stream().map(UserExerciseLog::getQuestionId).distinct().collect(Collectors.toList());

        // 4. 选题：target_level + module_type + difficulty_range，排除最近题目
        LambdaQueryWrapper<QuestionInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionInfo::getTargetLevel, cetLevel)
                .eq(QuestionInfo::getModuleType, moduleType)
                .between(QuestionInfo::getDifficultyLevel, loDifficulty, hiDifficulty)
                .eq(QuestionInfo::getIsDeleted, 0);
        if (!recentIds.isEmpty()) {
            wrapper.notIn(QuestionInfo::getId, recentIds);
        }
        wrapper.orderByAsc(QuestionInfo::getUsageCount)
                .last("LIMIT " + (count * 3)); // 多取一些用于混合

        List<QuestionInfo> candidates = questionInfoMapper.selectList(wrapper);

        if (candidates.isEmpty()) {
            // 放宽条件：忽略7天限制
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(QuestionInfo::getTargetLevel, cetLevel)
                    .eq(QuestionInfo::getModuleType, moduleType)
                    .between(QuestionInfo::getDifficultyLevel, loDifficulty, hiDifficulty)
                    .eq(QuestionInfo::getIsDeleted, 0)
                    .orderByAsc(QuestionInfo::getUsageCount)
                    .last("LIMIT " + count);
            candidates = questionInfoMapper.selectList(wrapper);
        }

        // 5. 取前count个，填充题目内容
        List<QuestionInfo> selected = candidates.stream().limit(count).collect(Collectors.toList());

        // 更新usage_count
        for (QuestionInfo qi : selected) {
            qi.setUsageCount(qi.getUsageCount() + 1);
            questionInfoMapper.updateById(qi);
        }

        return selected.stream()
                .map(qi -> toQuestionVO(qi, false))
                .collect(Collectors.toList());
    }

    // ============= 定级测试题 =============

    @Override
    public List<QuestionVO> getAssessQuestions() {
        List<QuestionVO> result = new ArrayList<>();

        // 12道词汇题，难度均匀分布 1~8
        List<QuestionInfo> vocab = getQuestionsByModule(1, 12);
        result.addAll(vocab.stream().map(qi -> toQuestionVO(qi, false)).collect(Collectors.toList()));

        // 8道语法题，难度均匀分布 1~8
        List<QuestionInfo> grammar = getQuestionsByModule(2, 8);
        result.addAll(grammar.stream().map(qi -> toQuestionVO(qi, false)).collect(Collectors.toList()));

        return result;
    }

    private List<QuestionInfo> getQuestionsByModule(int moduleType, int count) {
        LambdaQueryWrapper<QuestionInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionInfo::getModuleType, moduleType)
                .eq(QuestionInfo::getIsDeleted, 0)
                .orderByAsc(QuestionInfo::getDifficultyLevel)
                .last("LIMIT " + count * 2);
        List<QuestionInfo> all = questionInfoMapper.selectList(wrapper);

        // 均匀采样
        if (all.size() <= count) return all;
        List<QuestionInfo> sampled = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sampled.add(all.get(i * all.size() / count));
        }
        return sampled;
    }

    // ============= 单题详情 =============

    @Override
    public QuestionVO getQuestionById(Long questionId, boolean showAnswer) {
        QuestionInfo info = questionInfoMapper.selectById(questionId);
        if (info == null) return null;
        return toQuestionVO(info, showAnswer);
    }

    // ============= 批量取题（含答案，供learning-engine调用） =============

    @Override
    public List<QuestionVO> getQuestionsBatch(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return Collections.emptyList();
        List<QuestionInfo> infos = questionInfoMapper.selectBatchIds(questionIds);
        return infos.stream()
                .map(qi -> toQuestionVO(qi, true))
                .collect(Collectors.toList());
    }

    // ============= 提交答案 =============

    @Override
    public SubmitResponse submitAnswer(SubmitRequest request) {
        QuestionInfo info = questionInfoMapper.selectById(request.getQuestionId());
        QuestionContent content = questionContentMapper.selectOne(
                new LambdaQueryWrapper<QuestionContent>()
                        .eq(QuestionContent::getQuestionId, request.getQuestionId()));

        if (info == null || content == null) {
            throw new RuntimeException("题目不存在");
        }

        // 判分
        String userAnswer = request.getUserAnswer() != null ? request.getUserAnswer().trim().toLowerCase() : "";
        String correctAnswer = content.getCorrectAnswer().trim().toLowerCase();
        boolean isCorrect = judge(userAnswer, correctAnswer);

        String traceId = UUID.randomUUID().toString().substring(0, 8);

        // 写入答题记录
        UserExerciseLog logEntry = new UserExerciseLog();
        logEntry.setUserId(request.getUserId());
        logEntry.setQuestionId(request.getQuestionId());
        logEntry.setUserAnswer(request.getUserAnswer());
        logEntry.setIsCorrect(isCorrect ? 1 : 0);
        logEntry.setDurationSeconds(request.getDurationSeconds());
        logEntry.setModuleType(request.getModuleType());
        logEntry.setTraceId(traceId);
        exerciseLogMapper.insert(logEntry);

        // 错题本
        if (!isCorrect) {
            upsertErrorBook(request.getUserId(), request.getQuestionId());
        }

        // 调用 learning-engine 更新遗忘曲线
        try {
            String url = learningEngineUrl + "/learning/update-curve";
            Map<String, Object> body = new HashMap<>();
            body.put("userId", request.getUserId());
            body.put("questionId", request.getQuestionId());
            body.put("isCorrect", isCorrect);
            body.put("moduleType", request.getModuleType());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.warn("调用learning-engine更新遗忘曲线失败: {}", e.getMessage());
        }

        // 调用 user-service 更新ELO分
        try {
            String url = userServiceUrl + "/user/radar/update-elo";
            Map<String, Object> body = new HashMap<>();
            body.put("userId", request.getUserId());
            body.put("difficultyLevel", info.getDifficultyLevel());
            body.put("moduleType", request.getModuleType());
            body.put("isCorrect", isCorrect);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.warn("调用user-service更新ELO失败: {}", e.getMessage());
        }

        // 发布RabbitMQ事件（异步解耦，fire-and-forget）
        try {
            ExerciseCompletedEvent event = ExerciseCompletedEvent.builder()
                    .userId(request.getUserId())
                    .questionId(request.getQuestionId())
                    .moduleType(request.getModuleType())
                    .isCorrect(isCorrect)
                    .attemptCount(1)
                    .timestamp(System.currentTimeMillis())
                    .traceId(traceId)
                    .build();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY, event);
            log.debug("MQ事件已发送: traceId={}", traceId);
        } catch (Exception e) {
            log.warn("RabbitMQ事件发送失败（不影响主流程）: {}", e.getMessage());
        }

        log.info("答题提交: userId={}, questionId={}, correct={}, traceId={}",
                request.getUserId(), request.getQuestionId(), isCorrect, traceId);

        return SubmitResponse.builder()
                .isCorrect(isCorrect)
                .correctAnswer(content.getCorrectAnswer())
                .analysis(content.getAnalysis())
                .traceId(traceId)
                .build();
    }

    private boolean judge(String userAnswer, String correctAnswer) {
        if (userAnswer.equals(correctAnswer)) return true;
        // 忽略大小写和空格已处理，再处理常见变体
        return userAnswer.replaceAll("\\s+", "").equals(correctAnswer.replaceAll("\\s+", ""));
    }

    private void upsertErrorBook(Long userId, Long questionId) {
        UserErrorBook exist = errorBookMapper.selectOne(
                new LambdaQueryWrapper<UserErrorBook>()
                        .eq(UserErrorBook::getUserId, userId)
                        .eq(UserErrorBook::getQuestionId, questionId));
        if (exist != null) {
            exist.setWrongCount(exist.getWrongCount() + 1);
            exist.setLastWrongAt(LocalDateTime.now());
            exist.setIsResolved(0);
            errorBookMapper.updateById(exist);
        } else {
            UserErrorBook eb = new UserErrorBook();
            eb.setUserId(userId);
            eb.setQuestionId(questionId);
            eb.setWrongCount(1);
            eb.setLastWrongAt(LocalDateTime.now());
            eb.setIsResolved(0);
            errorBookMapper.insert(eb);
        }
    }

    // ============= 错题本 =============

    @Override
    public List<QuestionVO> getErrorBook(Long userId) {
        List<UserErrorBook> errors = errorBookMapper.selectList(
                new LambdaQueryWrapper<UserErrorBook>()
                        .eq(UserErrorBook::getUserId, userId)
                        .eq(UserErrorBook::getIsResolved, 0)
                        .orderByDesc(UserErrorBook::getWrongCount));
        if (errors.isEmpty()) return Collections.emptyList();

        List<Long> questionIds = errors.stream()
                .map(UserErrorBook::getQuestionId)
                .collect(Collectors.toList());

        List<QuestionInfo> infos = questionInfoMapper.selectBatchIds(questionIds);
        return infos.stream()
                .map(qi -> toQuestionVO(qi, false))
                .collect(Collectors.toList());
    }

    // ============= 知识点树 =============

    @Override
    public List<SysKnowledgePoint> getKnowledgePoints(String cetLevel) {
        return knowledgePointMapper.selectList(
                new LambdaQueryWrapper<SysKnowledgePoint>()
                        .eq(SysKnowledgePoint::getCetLevel, cetLevel)
                        .orderByAsc(SysKnowledgePoint::getSortOrder));
    }

    // ============= 内部工具方法 =============

    private QuestionVO toQuestionVO(QuestionInfo info, boolean showAnswer) {
        QuestionContent content = questionContentMapper.selectOne(
                new LambdaQueryWrapper<QuestionContent>()
                        .eq(QuestionContent::getQuestionId, info.getId()));

        return QuestionVO.builder()
                .id(info.getId())
                .moduleType(info.getModuleType())
                .difficultyLevel(info.getDifficultyLevel())
                .questionType(info.getQuestionType())
                .stem(content != null ? content.getStem() : null)
                .options(content != null ? content.getOptions() : null)
                .correctAnswer(showAnswer && content != null ? content.getCorrectAnswer() : null)
                .analysis(showAnswer && content != null ? content.getAnalysis() : null)
                .showAnswer(showAnswer)
                .build();
    }
}
