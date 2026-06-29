package com.english.ai.exercise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.english.ai.common.Result;
import com.english.ai.exercise.config.RabbitMQConfig;
import com.english.ai.exercise.dto.QuestionVO;
import com.english.ai.exercise.dto.SubmitRequest;
import com.english.ai.exercise.dto.SubmitResponse;
import com.english.ai.exercise.dto.ReviewTaskVO;
import com.english.ai.exercise.dto.UserExerciseStats;
import com.english.ai.exercise.dto.FavoriteVO;
import com.english.ai.exercise.dto.VocabularyVO;
import com.english.ai.exercise.dto.ExerciseHistoryVO;
import com.english.ai.exercise.dto.DailyWordVO;
import com.english.ai.exercise.entity.*;
import com.english.ai.exercise.event.ExerciseCompletedEvent;
import com.english.ai.exercise.mapper.*;
import com.english.ai.exercise.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final UserFavoriteMapper favoriteMapper;
    private final UserVocabularyMapper vocabularyMapper;
    private final WordDictionaryMapper wordDictionaryMapper;
    private final DailyWordMapper dailyWordMapper;
    private final RestTemplate restTemplate;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

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

        // 4. 构建候选等级列表：首选用户CET等级，其次相邻等级，最后全量兜底
        String[] levelCandidates;
        if ("CET4".equalsIgnoreCase(cetLevel)) {
            levelCandidates = new String[]{"CET4", "CET6"};
        } else if ("CET6".equalsIgnoreCase(cetLevel)) {
            levelCandidates = new String[]{"CET6", "CET4"};
        } else {
            levelCandidates = new String[]{cetLevel, "CET4", "CET6"};
        }

        // moduleType=1(词汇)取FILL_BLANK, moduleType=2(语法)取SENTENCE_MAKING
        String questionType = moduleType == 2 ? "SENTENCE_MAKING" : "FILL_BLANK";

        List<QuestionInfo> candidates = new ArrayList<>();

        // 按优先级从每个等级中取题，直到凑够数量或取完
        for (String level : levelCandidates) {
            if (candidates.size() >= count * 3) break;
            int remaining = count * 3 - candidates.size();

            LambdaQueryWrapper<QuestionInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(QuestionInfo::getTargetLevel, level)
                    .eq(QuestionInfo::getModuleType, moduleType)
                    .eq(QuestionInfo::getQuestionType, questionType)
                    .between(QuestionInfo::getDifficultyLevel, loDifficulty, hiDifficulty)
                    .eq(QuestionInfo::getIsDeleted, 0);
            if (!recentIds.isEmpty()) {
                wrapper.notIn(QuestionInfo::getId, recentIds);
            }
            wrapper.orderByAsc(QuestionInfo::getUsageCount)
                    .last("LIMIT " + remaining);
            candidates.addAll(questionInfoMapper.selectList(wrapper));
        }

        // 5. 再次放宽：忽略难度范围
        if (candidates.size() < count) {
            for (String level : levelCandidates) {
                if (candidates.size() >= count) break;
                int remaining = count - candidates.size();
                LambdaQueryWrapper<QuestionInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(QuestionInfo::getTargetLevel, level)
                        .eq(QuestionInfo::getModuleType, moduleType)
                        .eq(QuestionInfo::getQuestionType, questionType)
                        .eq(QuestionInfo::getIsDeleted, 0)
                        .orderByAsc(QuestionInfo::getUsageCount)
                        .last("LIMIT " + remaining);
                candidates.addAll(questionInfoMapper.selectList(wrapper));
            }
        }

        // 6. 最后兜底：不限制等级，只按模块取
        if (candidates.size() < count) {
            LambdaQueryWrapper<QuestionInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(QuestionInfo::getModuleType, moduleType)
                    .eq(QuestionInfo::getQuestionType, questionType)
                    .eq(QuestionInfo::getIsDeleted, 0)
                    .orderByAsc(QuestionInfo::getUsageCount)
                    .last("LIMIT " + count);
            candidates.addAll(questionInfoMapper.selectList(wrapper));
        }

        // 7. 取前count个，更新usage_count，填充题目内容
        List<QuestionInfo> selected = candidates.stream().limit(count).collect(Collectors.toList());
        log.info("智能选题: userId={}, userLevel={}, 实际使用等级{}条, moduleType={}, 需求{}题, 返回{}题",
                userId, cetLevel, selected.size() > 0 ? "混合" : "无", moduleType, count, selected.size());

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
        if (rabbitTemplate != null) {
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
        }

        log.info("答题提交: userId={}, questionId={}, correct={}, traceId={}",
                request.getUserId(), request.getQuestionId(), isCorrect, traceId);

        return SubmitResponse.builder()
                .isCorrect(isCorrect)
                .correctAnswer(content.getCorrectAnswer())
                .correctAnswerKey(extractAnswerKey(content.getCorrectAnswer()))
                .analysis(content.getAnalysis())
                .traceId(traceId)
                .build();
    }

    private boolean judge(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) return false;
        String ua = userAnswer.trim().toLowerCase();
        String ca = correctAnswer.trim().toLowerCase();
        if (ua.equals(ca)) return true;
        // 从完整答案文本（如 "A. established"）中提取选项字母
        String caKey = extractKey(ca);
        if (ua.equals(caKey)) return true;
        // userAnswer 是字母（a-d），correctAnswer 可能以该字母开头
        if (ua.matches("[a-d]")) {
            if (ca.startsWith(ua + ".") || ca.startsWith(ua + "、") || ca.startsWith(ua + ")")) {
                return true;
            }
        }
        return false;
    }

    /** 从任意格式的答案中提取纯选项字母 */
    private String extractKey(String answer) {
        if (answer == null) return "";
        String t = answer.trim().toLowerCase();
        if (t.isEmpty()) return t;
        // 尝试：A. xxx / A.xxx
        if (t.length() >= 2 && t.charAt(1) == '.') return t.substring(0, 1);
        // 尝试：数字. A. xxx
        int dotIdx = t.indexOf('.');
        if (dotIdx >= 0 && dotIdx + 2 < t.length()) {
            String rest = t.substring(dotIdx + 1).trim();
            if (rest.length() >= 1 && rest.charAt(0) >= 'a' && rest.charAt(0) <= 'd') {
                return String.valueOf(rest.charAt(0));
            }
        }
        // 单字母
        if (t.length() == 1 && t.charAt(0) >= 'a' && t.charAt(0) <= 'd') return t;
        return t;
    }

    /** 从 "A. some text" 中提取 "A"，兼容纯字母格式 */
    private String extractAnswerKey(String answer) {
        if (answer == null) return "";
        String t = answer.trim();
        if (t.length() >= 2 && t.charAt(1) == '.') return t.substring(0, 1).toUpperCase();
        if (t.length() == 1 && Character.isLetter(t.charAt(0))) return t.toUpperCase();
        return t;
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

    // ============= 每日单词 =============

    @Override
    public DailyWordVO getDailyWord(String cetLevel) {
        java.time.LocalDate today = java.time.LocalDate.now();

        // 1. 查今天是否已有每日单词
        DailyWord dailyWord = dailyWordMapper.selectOne(
                new LambdaQueryWrapper<DailyWord>()
                        .eq(DailyWord::getDate, today));

        WordDictionary word;
        if (dailyWord != null) {
            word = wordDictionaryMapper.selectById(dailyWord.getWordId());
        } else {
            // 2. 从词库选一个：按等级优先，usage_count 升序（轮转公平）
            List<WordDictionary> candidates = wordDictionaryMapper.selectList(
                    new LambdaQueryWrapper<WordDictionary>()
                            .eq(WordDictionary::getLevel, cetLevel)
                            .orderByAsc(WordDictionary::getUsageCount)
                            .last("LIMIT 1"));

            // 备用：放宽等级限制
            if (candidates.isEmpty()) {
                candidates = wordDictionaryMapper.selectList(
                        new LambdaQueryWrapper<WordDictionary>()
                                .orderByAsc(WordDictionary::getUsageCount)
                                .last("LIMIT 1"));
            }

            if (candidates.isEmpty()) {
                throw new RuntimeException("词库为空，请联系管理员添加单词");
            }

            word = candidates.get(0);

            // 3. 创建每日单词记录
            DailyWord newDailyWord = new DailyWord();
            newDailyWord.setWordId(word.getId());
            newDailyWord.setDate(today);
            try {
                dailyWordMapper.insert(newDailyWord);
            } catch (Exception e) {
                // 并发场景：唯一约束冲突，重新查询
                DailyWord existing = dailyWordMapper.selectOne(
                        new LambdaQueryWrapper<DailyWord>()
                                .eq(DailyWord::getDate, today));
                if (existing != null) {
                    word = wordDictionaryMapper.selectById(existing.getWordId());
                }
            }

            // 4. usage_count +1
            if (word != null) {
                word.setUsageCount(word.getUsageCount() + 1);
                wordDictionaryMapper.updateById(word);
            }
        }

        if (word == null) {
            throw new RuntimeException("每日单词数据异常");
        }

        return DailyWordVO.builder()
                .id(word.getId())
                .word(word.getWord())
                .phonetic(word.getPhonetic())
                .translation(word.getTranslation())
                .pos(word.getPos())
                .example(word.getExample())
                .exampleTranslation(word.getExampleTranslation())
                .level(word.getLevel())
                .date(today)
                .build();
    }

    // ============= 用户答题统计 =============

    @Override
    public UserExerciseStats getUserStats(Long userId) {
        // 1. 获取该用户所有答题记录
        List<UserExerciseLog> allLogs = exerciseLogMapper.selectList(
                new LambdaQueryWrapper<UserExerciseLog>()
                        .eq(UserExerciseLog::getUserId, userId)
                        .orderByAsc(UserExerciseLog::getCreatedAt));

        int totalAnswered = allLogs.size();
        int correctCount = (int) allLogs.stream().filter(l -> l.getIsCorrect() != null && l.getIsCorrect() == 1).count();
        int errorCount = totalAnswered - correctCount;
        double correctRate = totalAnswered > 0 ? (correctCount * 100.0 / totalAnswered) : 0.0;

        // 2. 累计学习时长
        int totalDurationSeconds = allLogs.stream().mapToInt(l -> l.getDurationSeconds() != null ? l.getDurationSeconds() : 0).sum();
        double totalHours = totalDurationSeconds / 3600.0;
        int totalMinutes = (totalDurationSeconds % 3600) / 60;

        java.time.LocalDate today = java.time.LocalDate.now();

        // 2.1 今日学习时长
        List<UserExerciseLog> todayLogs = allLogs.stream()
                .filter(l -> l.getCreatedAt() != null && l.getCreatedAt().toLocalDate().equals(today))
                .collect(Collectors.toList());

        int todayDurationSeconds = todayLogs.stream()
                .mapToInt(l -> l.getDurationSeconds() != null ? l.getDurationSeconds() : 0)
                .sum();

        // 2.2 今日答题数统计
        int todayAnsweredCount = todayLogs.size();
        int todayVocabAnswered = (int) todayLogs.stream()
                .filter(l -> l.getModuleType() != null && l.getModuleType() == 1)
                .count();
        int todayGrammarAnswered = (int) todayLogs.stream()
                .filter(l -> l.getModuleType() != null && l.getModuleType() == 2)
                .count();

        // 3. 按日期分组（只看日期，不看时间）
        Map<java.time.LocalDate, List<UserExerciseLog>> byDate = allLogs.stream()
                .filter(l -> l.getCreatedAt() != null)
                .collect(Collectors.groupingBy(l -> l.getCreatedAt().toLocalDate()));

        // 4. 活跃天数
        int activeDays = byDate.size();

        // 5. 连续学习天数（从今天向前数连续有答题记录的天数）
        int streakDays = 0;
        for (int i = 0; i < 365; i++) {
            java.time.LocalDate d = today.minusDays(i);
            if (byDate.containsKey(d)) {
                streakDays++;
            } else {
                if (i == 0) continue; // 今天还没答题，不打断streak
                break;
            }
        }
        if (streakDays == 0 && byDate.containsKey(today)) {
            streakDays = 1;
        }

        // 6. 已掌握单词数 = 正确答题数（词汇题占70%权重）
        long vocabCorrect = allLogs.stream()
                .filter(l -> l.getIsCorrect() != null && l.getIsCorrect() == 1)
                .filter(l -> l.getModuleType() == null || l.getModuleType() == 1)
                .count();
        int masteredWords = (int) Math.max(vocabCorrect * 1L, correctCount);

        // 7. 全国排名估算（基于正确率和答题量）
        double score = correctRate * 0.6 + Math.min(totalAnswered / 200.0, 1.0) * 40;
        double nationalRank = Math.max(1.0, Math.min(99.0, score));

        // 8. 词汇理解度 = 正确率
        double vocabularyComprehension = correctRate;

        // 9. 生成年度热力图数据 + 每日学习时长（过去365天）
        List<UserExerciseStats.DailyActivity> heatmapData = new ArrayList<>();
        List<Integer> yearlyStudyMinutes = new ArrayList<>();
        java.time.LocalDate startOfYear = today.minusDays(364);
        for (int i = 0; i < 365; i++) {
            java.time.LocalDate d = startOfYear.plusDays(i);
            List<UserExerciseLog> dayLogs = byDate.getOrDefault(d, Collections.emptyList());
            int count = dayLogs.size();
            int activityLevel = 0;
            if (count > 0 && count <= 3) {
                activityLevel = 1;
            } else if (count > 3 && count <= 8) {
                activityLevel = 2;
            } else if (count > 8 && count <= 15) {
                activityLevel = 3;
            } else if (count > 15 && count <= 25) {
                activityLevel = 4;
            } else if (count > 25) {
                activityLevel = 5;
            }

            heatmapData.add(UserExerciseStats.DailyActivity.builder()
                    .date(d.toString())
                    .questionCount(count)
                    .activityLevel(activityLevel)
                    .build());

            // 当日学习时长（分钟）
            int daySeconds = dayLogs.stream()
                    .mapToInt(l -> l.getDurationSeconds() != null ? l.getDurationSeconds() : 0)
                    .sum();
            yearlyStudyMinutes.add(daySeconds / 60);
        }

        // 10. 最近7天学习时长（分钟）+ 每日答题数
        List<Integer> recentWeekMinutes = new ArrayList<>();
        List<Integer> recentWeekQuestionCounts = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate d = today.minusDays(i);
            List<UserExerciseLog> dayLogs = byDate.getOrDefault(d, Collections.emptyList());
            int daySeconds = dayLogs.stream()
                    .mapToInt(l -> l.getDurationSeconds() != null ? l.getDurationSeconds() : 0)
                    .sum();
            recentWeekMinutes.add(daySeconds / 60);
            // 当日答题数
            recentWeekQuestionCounts.add(dayLogs.size());
        }

        // 10.1 本周平均每天答题数（最近7天每日答题数的平均值，四舍五入保留1位小数）
        double weeklyAvgQuestions = recentWeekQuestionCounts.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        double weeklyAverageQuestions = Math.round(weeklyAvgQuestions * 10.0) / 10.0;

        // 10.2 本年 1~12 月每月答题总数（12个元素，下标0对应1月）
        List<Integer> yearlyMonthQuestions = new ArrayList<>();
        int currentYear = today.getYear();
        for (int m = 1; m <= 12; m++) {
            java.time.YearMonth ym = java.time.YearMonth.of(currentYear, m);
            // 统计该月答题总数
            int monthQuestionCount = 0;
            for (Map.Entry<java.time.LocalDate, List<UserExerciseLog>> entry : byDate.entrySet()) {
                if (java.time.YearMonth.from(entry.getKey()).equals(ym)) {
                    monthQuestionCount += entry.getValue().size();
                }
            }
            yearlyMonthQuestions.add(monthQuestionCount);
        }

        // 11. 24小时学习分布（按小时统计累计学习时长/分钟）
        List<Integer> hourlyStudyMinutes = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hourlyStudyMinutes.add(0);
        }
        for (UserExerciseLog log : allLogs) {
            if (log.getCreatedAt() != null) {
                int hour = log.getCreatedAt().getHour();
                int minutes = log.getDurationSeconds() != null ? log.getDurationSeconds() / 60 : 0;
                hourlyStudyMinutes.set(hour, hourlyStudyMinutes.get(hour) + minutes);
            }
        }

        return UserExerciseStats.builder()
                .totalAnswered(totalAnswered)
                .correctCount(correctCount)
                .errorCount(errorCount)
                .correctRate(Math.round(correctRate * 100.0) / 100.0)
                .totalDurationSeconds(totalDurationSeconds)
                .totalHours(Math.round(totalHours * 100.0) / 100.0)
                .totalMinutes(totalMinutes)
                .todayDurationSeconds(todayDurationSeconds)
                .todayAnsweredCount(todayAnsweredCount)
                .todayVocabAnswered(todayVocabAnswered)
                .todayGrammarAnswered(todayGrammarAnswered)
                .streakDays(streakDays)
                .activeDays(activeDays)
                .masteredWords(masteredWords)
                .nationalRankPercent(Math.round(nationalRank * 100.0) / 100.0)
                .vocabularyComprehension(Math.round(vocabularyComprehension * 100.0) / 100.0)
                .heatmapData(heatmapData)
                .recentWeekMinutes(recentWeekMinutes)
                .weeklyAverageQuestions(weeklyAverageQuestions)
                .yearlyMonthQuestions(yearlyMonthQuestions)
                .yearlyStudyMinutes(yearlyStudyMinutes)
                .hourlyStudyMinutes(hourlyStudyMinutes)
                .build();
    }

    // ============= 内部工具方法 =============

    private QuestionVO toQuestionVO(QuestionInfo info, boolean showAnswer) {
        QuestionContent content = questionContentMapper.selectOne(
                new LambdaQueryWrapper<QuestionContent>()
                        .eq(QuestionContent::getQuestionId, info.getId()));

        String correctAnswer = content != null ? content.getCorrectAnswer() : null;
        // hint = 正确答案前2个字母（练习模式可用，答题前提示）
        String hint = (correctAnswer != null && correctAnswer.length() >= 2)
                ? correctAnswer.substring(0, 2)
                : (correctAnswer != null ? correctAnswer : "");

        return QuestionVO.builder()
                .id(info.getId())
                .moduleType(info.getModuleType())
                .difficultyLevel(info.getDifficultyLevel())
                .questionType(info.getQuestionType())
                .stem(content != null ? content.getStem() : null)
                .options(content != null ? content.getOptions() : null)
                .correctAnswer(showAnswer && content != null ? correctAnswer : null)
                .hint(hint)
                .analysis(content != null ? content.getAnalysis() : null)
                .showAnswer(showAnswer)
                .build();
    }

    // ============= 收藏句 =============

    @Override
    public FavoriteVO addFavorite(Long userId, String sentenceText, Long questionId) {
        UserFavorite f = new UserFavorite();
        f.setUserId(userId);
        f.setSentenceText(sentenceText);
        f.setQuestionId(questionId);
        f.setSourceType(questionId != null ? "EXERCISE" : "SENTENCE");
        favoriteMapper.insert(f);
        return toFavoriteVO(f);
    }

    @Override
    public List<FavoriteVO> getFavorites(Long userId) {
        List<UserFavorite> list = favoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getCreatedAt));
        return list.stream().map(this::toFavoriteVO).collect(Collectors.toList());
    }

    @Override
    public void removeFavorite(Long id, Long userId) {
        favoriteMapper.delete(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getId, id)
                        .eq(UserFavorite::getUserId, userId));
    }

    private FavoriteVO toFavoriteVO(UserFavorite f) {
        return FavoriteVO.builder()
                .id(f.getId())
                .userId(f.getUserId())
                .sentenceText(f.getSentenceText())
                .questionId(f.getQuestionId())
                .sourceType(f.getSourceType())
                .createdAt(f.getCreatedAt())
                .build();
    }

    // ============= 单词本 =============

    @Override
    public VocabularyVO addVocabulary(Long userId, String word, String translation) {
        UserVocabulary v = new UserVocabulary();
        v.setUserId(userId);
        v.setWord(word);
        v.setTranslation(translation);
        v.setIsMastered(0);
        vocabularyMapper.insert(v);
        return toVocabularyVO(v);
    }

    @Override
    public List<VocabularyVO> getVocabulary(Long userId) {
        List<UserVocabulary> list = vocabularyMapper.selectList(
                new LambdaQueryWrapper<UserVocabulary>()
                        .eq(UserVocabulary::getUserId, userId)
                        .orderByAsc(UserVocabulary::getIsMastered)
                        .orderByDesc(UserVocabulary::getCreatedAt));
        return list.stream().map(this::toVocabularyVO).collect(Collectors.toList());
    }

    @Override
    public void removeVocabulary(Long id, Long userId) {
        vocabularyMapper.delete(
                new LambdaQueryWrapper<UserVocabulary>()
                        .eq(UserVocabulary::getId, id)
                        .eq(UserVocabulary::getUserId, userId));
    }

    @Override
    public VocabularyVO toggleMastered(Long id, Long userId) {
        UserVocabulary v = vocabularyMapper.selectOne(
                new LambdaQueryWrapper<UserVocabulary>()
                        .eq(UserVocabulary::getId, id)
                        .eq(UserVocabulary::getUserId, userId));
        if (v == null) throw new RuntimeException("单词不存在");
        v.setIsMastered(v.getIsMastered() == 1 ? 0 : 1);
        vocabularyMapper.updateById(v);
        return toVocabularyVO(v);
    }

    private VocabularyVO toVocabularyVO(UserVocabulary v) {
        return VocabularyVO.builder()
                .id(v.getId())
                .userId(v.getUserId())
                .word(v.getWord())
                .translation(v.getTranslation())
                .isMastered(v.getIsMastered())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    // ============= 学习历史 =============

    @Override
    public List<ExerciseHistoryVO> getExerciseHistory(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<UserExerciseLog> logs = exerciseLogMapper.selectList(
                new LambdaQueryWrapper<UserExerciseLog>()
                        .eq(UserExerciseLog::getUserId, userId)
                        .orderByDesc(UserExerciseLog::getCreatedAt)
                        .last("LIMIT " + offset + "," + size));

        List<Long> questionIds = logs.stream()
                .map(UserExerciseLog::getQuestionId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, QuestionInfo> infoMap = Collections.emptyMap();
        Map<Long, QuestionContent> contentMap = Collections.emptyMap();
        if (!questionIds.isEmpty()) {
            infoMap = questionInfoMapper.selectBatchIds(questionIds).stream()
                    .collect(Collectors.toMap(QuestionInfo::getId, qi -> qi));
            contentMap = questionContentMapper.selectList(
                    new LambdaQueryWrapper<QuestionContent>()
                            .in(QuestionContent::getQuestionId, questionIds))
                    .stream()
                    .collect(Collectors.toMap(QuestionContent::getQuestionId, qc -> qc, (a, b) -> a));
        }

        Map<Long, QuestionInfo> finalInfoMap = infoMap;
        Map<Long, QuestionContent> finalContentMap = contentMap;

        return logs.stream().map(log -> {
            QuestionInfo info = finalInfoMap.get(log.getQuestionId());
            QuestionContent content = finalContentMap.get(log.getQuestionId());
            return ExerciseHistoryVO.builder()
                    .logId(log.getId())
                    .questionId(log.getQuestionId())
                    .stem(content != null ? content.getStem() : null)
                    .userAnswer(log.getUserAnswer())
                    .correctAnswer(content != null ? content.getCorrectAnswer() : null)
                    .isCorrect(log.getIsCorrect())
                    .durationSeconds(log.getDurationSeconds())
                    .moduleType(log.getModuleType())
                    .questionType(info != null ? info.getQuestionType() : null)
                    .difficultyLevel(info != null ? info.getDifficultyLevel() : null)
                    .createdAt(log.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public long getExerciseHistoryCount(Long userId) {
        return exerciseLogMapper.selectCount(
                new LambdaQueryWrapper<UserExerciseLog>()
                        .eq(UserExerciseLog::getUserId, userId));
    }
}
