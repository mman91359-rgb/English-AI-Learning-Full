package com.english.ai.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.english.ai.common.Result;
import com.english.ai.learning.dto.ReviewTaskVO;
import com.english.ai.learning.dto.UpdateCurveRequest;
import com.english.ai.learning.entity.UserMemoryCurve;
import com.english.ai.learning.mapper.UserMemoryCurveMapper;
import com.english.ai.learning.service.LearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习引擎服务实现 — 基于艾宾浩斯遗忘曲线
 *
 * 复习时间梯度:
 *   mastery=1(陌生) → T+1天
 *   mastery=2(模糊) → T+3天
 *   mastery=3(熟悉) → T+7天
 *   mastery=4(熟练) → T+15天
 *   mastery=5(精通) → T+30天
 *
 * 答对: mastery+1, 按新等级计算next_review_time
 * 答错: mastery=max(1, mastery-1), 按降级后等级计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private final UserMemoryCurveMapper memoryCurveMapper;
    private final RestTemplate restTemplate;

    @Value("${service.exercise.url}")
    private String exerciseServiceUrl;

    /** 复习间隔梯度（天数） */
    private static final int[] REVIEW_INTERVALS = {0, 1, 3, 7, 15, 30};

    @Override
    public void updateCurve(UpdateCurveRequest request) {
        // 1. 查询现有记录
        UserMemoryCurve curve = memoryCurveMapper.selectOne(
                new LambdaQueryWrapper<UserMemoryCurve>()
                        .eq(UserMemoryCurve::getUserId, request.getUserId())
                        .eq(UserMemoryCurve::getQuestionId, request.getQuestionId()));

        if (curve == null) {
            // 2. 新增记录
            curve = new UserMemoryCurve();
            curve.setUserId(request.getUserId());
            curve.setQuestionId(request.getQuestionId());
            curve.setMasteryLevel(1);
            curve.setReviewCount(0);
            curve.setNextReviewTime(LocalDateTime.now().plusDays(1)); // T+1
            curve.setLastReviewAt(LocalDateTime.now());
            memoryCurveMapper.insert(curve);
            log.info("新增遗忘曲线记录: userId={}, questionId={}, nextReview=T+1",
                    request.getUserId(), request.getQuestionId());
        } else {
            // 3. 更新记录
            int newMastery;
            if (Boolean.TRUE.equals(request.getIsCorrect())) {
                // 答对：升级（上限5）
                newMastery = Math.min(5, curve.getMasteryLevel() + 1);
            } else {
                // 答错：降级（下限1）
                newMastery = Math.max(1, curve.getMasteryLevel() - 1);
            }

            int intervalDays = REVIEW_INTERVALS[newMastery];
            curve.setMasteryLevel(newMastery);
            curve.setReviewCount(curve.getReviewCount() + 1);
            curve.setNextReviewTime(LocalDateTime.now().plusDays(intervalDays));
            curve.setLastReviewAt(LocalDateTime.now());
            memoryCurveMapper.updateById(curve);

            log.info("更新遗忘曲线: userId={}, questionId={}, mastery={}→{}, nextReview=T+{}天, reviewCount={}",
                    request.getUserId(), request.getQuestionId(),
                    curve.getMasteryLevel(), newMastery, intervalDays, curve.getReviewCount());
        }
    }

    @Override
    public List<ReviewTaskVO> getReviewTasks(Long userId) {
        // 查询今日到期的复习任务
        LocalDateTime now = LocalDateTime.now();
        List<UserMemoryCurve> curves = memoryCurveMapper.selectList(
                new LambdaQueryWrapper<UserMemoryCurve>()
                        .eq(UserMemoryCurve::getUserId, userId)
                        .le(UserMemoryCurve::getNextReviewTime, now)
                        .orderByAsc(UserMemoryCurve::getNextReviewTime)
                        .last("LIMIT 20"));

        if (curves.isEmpty()) {
            return Collections.emptyList();
        }

        // 从 exercise-service 批量获取题目内容
        List<Long> questionIds = curves.stream()
                .map(UserMemoryCurve::getQuestionId)
                .collect(Collectors.toList());

        Map<Long, Map<String, Object>> questionMap = fetchQuestions(questionIds);

        return curves.stream().map(curve -> {
            ReviewTaskVO.ReviewTaskVOBuilder builder = ReviewTaskVO.builder()
                    .curveId(curve.getId())
                    .questionId(curve.getQuestionId())
                    .masteryLevel(curve.getMasteryLevel())
                    .nextReviewTime(curve.getNextReviewTime())
                    .reviewCount(curve.getReviewCount());

            Map<String, Object> q = questionMap.get(curve.getQuestionId());
            if (q != null) {
                builder.stem((String) q.get("stem"))
                        .correctAnswer((String) q.get("correctAnswer"))
                        .analysis((String) q.get("analysis"))
                        .moduleType(q.get("moduleType") != null ? ((Number) q.get("moduleType")).intValue() : 0);
            }

            return builder.build();
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getCurveStats(Long userId) {
        List<UserMemoryCurve> all = memoryCurveMapper.selectList(
                new LambdaQueryWrapper<UserMemoryCurve>()
                        .eq(UserMemoryCurve::getUserId, userId));

        long total = all.size();
        long todayReview = all.stream()
                .filter(c -> c.getNextReviewTime() != null
                        && c.getNextReviewTime().isBefore(LocalDateTime.now()))
                .count();

        // 掌握等级分布
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            final int level = i;
            distribution.put(i, all.stream().filter(c -> c.getMasteryLevel() == level).count());
        }

        // 平均掌握等级
        double avgMastery = all.stream().mapToInt(UserMemoryCurve::getMasteryLevel).average().orElse(0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalQuestions", total);
        stats.put("todayReviewCount", todayReview);
        stats.put("averageMastery", Math.round(avgMastery * 10.0) / 10.0);
        stats.put("masteryDistribution", distribution);
        return stats;
    }

    /**
     * 从 exercise-service 批量获取题目
     */
    private Map<Long, Map<String, Object>> fetchQuestions(List<Long> questionIds) {
        try {
            String ids = questionIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            String url = exerciseServiceUrl + "/exercise/questions/batch?ids=" + ids;

            ResponseEntity<Result> resp = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Result>() {});

            if (resp.getBody() != null && resp.getBody().getCode() == 200) {
                List<Map<String, Object>> questions = (List<Map<String, Object>>) resp.getBody().getData();
                if (questions != null) {
                    return questions.stream()
                            .collect(Collectors.toMap(
                                    q -> ((Number) q.get("id")).longValue(),
                                    q -> q
                            ));
                }
            }
        } catch (Exception e) {
            log.warn("获取题目内容失败: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }
}
