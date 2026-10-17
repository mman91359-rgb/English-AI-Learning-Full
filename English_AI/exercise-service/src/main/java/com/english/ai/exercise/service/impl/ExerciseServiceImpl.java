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
import com.english.ai.exercise.dto.WordDetailVO;
import com.english.ai.exercise.dto.JudgmentResult;
import com.english.ai.exercise.dto.WeakKnowledgePoint;
import com.english.ai.exercise.entity.*;
import com.english.ai.exercise.event.ExerciseCompletedEvent;
import com.english.ai.exercise.mapper.*;
import com.english.ai.exercise.service.ExerciseService;
import com.english.ai.exercise.service.JudgmentEngine;
import com.english.ai.exercise.service.QuestionGenerateService;
import com.english.ai.exercise.util.EbbinghausUtil;
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
    private final JudgmentEngine judgmentEngine;
    private final QuestionGenerateService questionGenerateService;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Value("${service.user.url}")
    private String userServiceUrl;

    @Value("${service.learning-engine.url}")
    private String learningEngineUrl;

    @Value("${service.ai.url}")
    private String aiServiceUrl;

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

        String traceId = UUID.randomUUID().toString().substring(0, 8);

        boolean isCorrect;
        String heuristicHint = null;
        String errorType = null;
        Integer matchedLevel = null;
        List<String> suggestions = new ArrayList<>();
        Boolean isSemanticMatch = null;

        String judgeCorrectAnswer = content.getCorrectAnswer();
        String judgeContext = content.getStem();

        JudgmentResult judgmentResult;
        if (request.getModuleType() != null && request.getModuleType() == 2) {
            String[] display = buildGrammarDisplay(content, request.getUserAnswer());
            String fullSentence = display[0] != null ? display[0] : "";
            String correctText = display[1] != null ? display[1] : "";
            judgeCorrectAnswer = correctText;

            // 用 normalizeSentence 对比：消除标点、空格差异
            // 用户提交的可能是完整句子也可能是填空部分
            String normUser = normalizeSentence(request.getUserAnswer());
            String normFull = normalizeSentence(fullSentence);
            String normText = normalizeSentence(correctText);

            if (!normUser.isEmpty() && (normUser.equals(normFull) || normUser.equals(normText))) {
                judgmentResult = JudgmentResult.builder()
                        .isCorrect(true)
                        .heuristicHint("回答正确！")
                        .matchedLevel(0)
                        .suggestions(List.of())
                        .build();
            } else {
                judgmentResult = judgmentEngine.judge(request, judgeCorrectAnswer, judgeContext);
            }
        } else {
            judgmentResult = judgmentEngine.judge(request, judgeCorrectAnswer, judgeContext);
        }

        isCorrect = judgmentResult.getIsCorrect();
        heuristicHint = judgmentResult.getHeuristicHint();
        errorType = judgmentResult.getErrorType();
        matchedLevel = judgmentResult.getMatchedLevel();
        suggestions = judgmentResult.getSuggestions();
        isSemanticMatch = judgmentResult.getIsSemanticMatch();

        if (!isCorrect && heuristicHint == null) {
            heuristicHint = "这个答案不太对哦，再想一想这个词的正确用法是什么？";
        }

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
        } else {
            resolveErrorBook(request.getUserId(), request.getQuestionId());
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

        log.info("答题提交: userId={}, questionId={}, correct={}, matchedLevel={}, errorType={}, traceId={}",
                request.getUserId(), request.getQuestionId(), isCorrect, matchedLevel, errorType, traceId);

        return SubmitResponse.builder()
                .isCorrect(isCorrect)
                .correctAnswer(content.getCorrectAnswer())
                .correctAnswerKey(extractAnswerKey(content.getCorrectAnswer()))
                .analysis(content.getAnalysis())
                .traceId(traceId)
                .heuristicHint(heuristicHint)
                .errorType(errorType)
                .matchedLevel(matchedLevel)
                .suggestions(suggestions)
                .isSemanticMatch(isSemanticMatch)
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
        // 从 "A. established" 中提取选项文本 "established" 与 ua 对比
        // 解决前端提交选项文本（而非字母）时的判分问题
        String caText = extractOptionText(ca);
        if (!caText.isEmpty() && ua.equals(caText)) return true;
        // userAnswer 是字母（a-d），correctAnswer 可能以该字母开头
        if (ua.matches("[a-d]")) {
            if (ca.startsWith(ua + ".") || ca.startsWith(ua + "、") || ca.startsWith(ua + ")")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 规范化句子用于对比判分：
     * 去掉所有标点符号（句号、逗号、问号、感叹号、分号、冒号、引号、括号等），
     * 去掉多余空格，统一小写。
     * 用于语法题（词语排序题）提交完整句子时的对比，避免标点和空格差异导致误判。
     */
    private String normalizeSentence(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[\\p{Punct}。？！，；：、\u201c\u201d\u2018\u2019（）【】《》…—]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 从 "A. established" 格式中提取选项纯文本 "established"
     * 用于前端提交选项文本时与 correctAnswer（含字母前缀）做对比
     * 输入已统一转小写
     */
    private String extractOptionText(String answer) {
        if (answer == null) return "";
        String t = answer.trim();
        if (t.length() < 2) return "";
        char first = t.charAt(0);
        // 首字符为 a-d，跳过分隔符（. 、 ) 空格），返回剩余文本
        if (first >= 'a' && first <= 'd') {
            int i = 1;
            while (i < t.length() && (t.charAt(i) == '.' || t.charAt(i) == '、'
                    || t.charAt(i) == ')' || t.charAt(i) == ' ')) {
                i++;
            }
            if (i < t.length()) {
                return t.substring(i).trim();
            }
        }
        return "";
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

    /**
     * 用户答对某题后，从错题本中移除该题（标记为已解决）
     * 错题集只保留未解决的错题，解决一个少一个
     */
    private void resolveErrorBook(Long userId, Long questionId) {
        UserErrorBook exist = errorBookMapper.selectOne(
                new LambdaQueryWrapper<UserErrorBook>()
                        .eq(UserErrorBook::getUserId, userId)
                        .eq(UserErrorBook::getQuestionId, questionId)
                        .eq(UserErrorBook::getIsResolved, 0));
        if (exist != null) {
            exist.setIsResolved(1);
            exist.setUpdatedAt(LocalDateTime.now());
            errorBookMapper.updateById(exist);
            log.info("错题已解决: userId={}, questionId={}", userId, questionId);
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
        // 错题本需要显示正确答案（用户重做后需对照），showAnswer=true
        return infos.stream()
                .map(qi -> toQuestionVO(qi, true))
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

        // 9. 生成年度热力图数据 + 每日学习时长（当前年 1月1日 ~ 12月31日，按自然年排版）
        List<UserExerciseStats.DailyActivity> heatmapData = new ArrayList<>();
        List<Integer> yearlyStudyMinutes = new ArrayList<>();
        int currentYear = today.getYear();
        java.time.LocalDate startOfYear = java.time.LocalDate.of(currentYear, 1, 1);
        java.time.LocalDate endOfYear = java.time.LocalDate.of(currentYear, 12, 31);
        // 计算当前年总天数（兼容闰年 366 天与平年 365 天）
        long totalDaysOfYear = java.time.temporal.ChronoUnit.DAYS.between(startOfYear, endOfYear) + 1;
        for (int i = 0; i < totalDaysOfYear; i++) {
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

        // 10. 本周学习时长（分钟）+ 每日答题数
        // 按周一到周日顺序返回，确保与前端标签对齐
        List<Integer> recentWeekMinutes = new ArrayList<>();
        List<Integer> recentWeekQuestionCounts = new ArrayList<>();
        // 获取本周一的日期（Java的DayOfWeek: 1=周一, 7=周日）
        int dayOfWeek = today.getDayOfWeek().getValue();
        java.time.LocalDate monday = today.minusDays(dayOfWeek - 1);
        // 遍历周一到周日（7天）
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate d = monday.plusDays(i);
            List<UserExerciseLog> dayLogs = byDate.getOrDefault(d, Collections.emptyList());
            int daySeconds = dayLogs.stream()
                    .mapToInt(l -> l.getDurationSeconds() != null ? l.getDurationSeconds() : 0)
                    .sum();
            recentWeekMinutes.add(daySeconds / 60);
            recentWeekQuestionCounts.add(dayLogs.size());
        }

        // 10.1 本周平均每天答题数（最近7天每日答题数的平均值，四舍五入取整）
        double weeklyAvgQuestions = recentWeekQuestionCounts.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        int weeklyAverageQuestions = (int) Math.round(weeklyAvgQuestions);

        // 10.2 本年 1~12 月每月答题总数（12个元素，下标0对应1月）
        List<Integer> yearlyMonthQuestions = new ArrayList<>();
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
                .phonetic(v.getPhonetic())
                .pos(v.getPos())
                .example(v.getExample())
                .exampleTranslation(v.getExampleTranslation())
                .collocation(v.getCollocation())
                .source(v.getSource())
                .reviewCount(v.getReviewCount())
                .masteryLevel(v.getMasteryLevel())
                .lastReviewedAt(v.getLastReviewedAt())
                .nextReviewAt(v.getNextReviewAt())
                .isMastered(v.getIsMastered())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    /**
     * 智能添加单词：调用 ai-service 的 /ai/word-detail 接口自动补全音标/词性/释义/例句/搭配
     * 然后写入 user_vocabulary 表，并初始化艾宾浩斯复习计划（next_review_at = 明天）
     */
    @Override
    public VocabularyVO addVocabularySmart(Long userId, String word, String source) {
        String w = word == null ? "" : word.trim();
        if (w.isEmpty()) throw new RuntimeException("单词不能为空");

        // 调用 ai-service 获取单词详情
        WordDetailVO detail = fetchWordDetailFromAi(w);

        UserVocabulary v = new UserVocabulary();
        v.setUserId(userId);
        v.setWord(w);
        v.setIsMastered(0);
        v.setSource(source == null || source.isBlank() ? "manual" : source);
        v.setReviewCount(0);
        v.setMasteryLevel(0);
        v.setNextReviewAt(EbbinghausUtil.nextReviewAt(0));

        if (detail != null) {
            v.setPhonetic(detail.getPhonetic());
            v.setPos(detail.getPos());
            v.setTranslation(detail.getTranslation());
            v.setExample(detail.getExample());
            v.setExampleTranslation(detail.getExampleTranslation());
            v.setCollocation(detail.getCollocation());
        } else {
            // AI 不可用兜底：translation 留空，前端引导用户手动补
            v.setTranslation("");
        }

        vocabularyMapper.insert(v);
        log.info("智能添加单词: userId={}, word={}, source={}", userId, w, v.getSource());
        return toVocabularyVO(v);
    }

    /**
     * 通过 RestTemplate 调用 ai-service 的 /ai/word-detail 接口
     */
    @SuppressWarnings("unchecked")
    private WordDetailVO fetchWordDetailFromAi(String word) {
        try {
            String url = aiServiceUrl + "/ai/word-detail?word=" + java.net.URLEncoder.encode(word, java.nio.charset.StandardCharsets.UTF_8);
            ResponseEntity<Result> resp = restTemplate.getForEntity(url, Result.class);
            if (resp.getBody() != null && resp.getBody().getCode() == 200 && resp.getBody().getData() != null) {
                Map<String, Object> map = (Map<String, Object>) resp.getBody().getData();
                WordDetailVO vo = new WordDetailVO();
                vo.setWord((String) map.get("word"));
                vo.setPhonetic((String) map.get("phonetic"));
                vo.setPos((String) map.get("pos"));
                vo.setTranslation((String) map.get("translation"));
                vo.setExample((String) map.get("example"));
                vo.setExampleTranslation((String) map.get("exampleTranslation"));
                vo.setCollocation((String) map.get("collocation"));
                return vo;
            }
        } catch (Exception e) {
            log.warn("调用 ai-service 生成单词详情失败，word={}: {}", word, e.getMessage());
        }
        return null;
    }

    /**
     * 获取今日待复习单词列表
     * 规则：next_review_at <= 现在 且 isMastered = 0
     */
    @Override
    public List<VocabularyVO> getReviewList(Long userId) {
        List<UserVocabulary> list = vocabularyMapper.selectList(
                new LambdaQueryWrapper<UserVocabulary>()
                        .eq(UserVocabulary::getUserId, userId)
                        .eq(UserVocabulary::getIsMastered, 0)
                        .le(UserVocabulary::getNextReviewAt, LocalDateTime.now())
                        .orderByAsc(UserVocabulary::getNextReviewAt));
        return list.stream().map(this::toVocabularyVO).collect(Collectors.toList());
    }

    /**
     * 提交单词复习结果：
     *  - 答对：掌握度+1，下次复习时间按新等级延后；若达到最高掌握度且再次答对则标记为已掌握
     *  - 答错：掌握度重置为0，下次复习时间为明天
     */
    @Override
    public VocabularyVO reviewWord(Long id, Long userId, Boolean isCorrect) {
        if (isCorrect == null) throw new RuntimeException("复习结果不能为空");
        UserVocabulary v = vocabularyMapper.selectOne(
                new LambdaQueryWrapper<UserVocabulary>()
                        .eq(UserVocabulary::getId, id)
                        .eq(UserVocabulary::getUserId, userId));
        if (v == null) throw new RuntimeException("单词不存在");

        int currentMastery = v.getMasteryLevel() == null ? 0 : v.getMasteryLevel();
        int newMastery;
        if (isCorrect) {
            newMastery = EbbinghausUtil.nextMasteryOnCorrect(currentMastery);
        } else {
            newMastery = EbbinghausUtil.nextMasteryOnWrong();
        }

        v.setMasteryLevel(newMastery);
        v.setReviewCount((v.getReviewCount() == null ? 0 : v.getReviewCount()) + 1);
        v.setLastReviewedAt(LocalDateTime.now());
        v.setNextReviewAt(EbbinghausUtil.nextReviewAt(newMastery));

        // 达到最高掌握度且本次答对 → 标记为已掌握
        if (EbbinghausUtil.isMastered(newMastery, isCorrect)) {
            v.setIsMastered(1);
        } else {
            v.setIsMastered(0);
        }

        vocabularyMapper.updateById(v);
        log.info("单词复习: id={}, userId={}, isCorrect={}, newMastery={}, nextReviewAt={}",
                id, userId, isCorrect, newMastery, v.getNextReviewAt());
        return toVocabularyVO(v);
    }

    // ============= 学习历史 =============

    @Override
    public List<ExerciseHistoryVO> getExerciseHistory(Long userId, Integer page, Integer size, Integer moduleType, Integer isCorrect, String startTime) {
        int offset = (page - 1) * size;
        LambdaQueryWrapper<UserExerciseLog> wrapper = new LambdaQueryWrapper<UserExerciseLog>()
                .eq(UserExerciseLog::getUserId, userId);

        if (moduleType != null && moduleType != 0) {
            wrapper.eq(UserExerciseLog::getModuleType, moduleType);
        }
        if (isCorrect != null && isCorrect >= 0) {
            wrapper.eq(UserExerciseLog::getIsCorrect, isCorrect);
        }
        if (startTime != null && !startTime.isBlank()) {
            try {
                LocalDateTime start = LocalDateTime.parse(startTime);
                wrapper.ge(UserExerciseLog::getCreatedAt, start);
            } catch (Exception e) {
                log.warn("解析 startTime 失败: {}", startTime);
            }
        }

        List<UserExerciseLog> logs = exerciseLogMapper.selectList(
                wrapper.orderByDesc(UserExerciseLog::getCreatedAt)
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

            // 默认取数据库原始值
            String stem = content != null ? content.getStem() : null;
            String correctAnswer = content != null ? content.getCorrectAnswer() : null;
            String userAnswer = log.getUserAnswer();

            // 语法题（moduleType=2）特殊处理：还原完整句子，correctAnswer 和 userAnswer 转为纯文本
            // 数据库中语法题存的是选择题格式（stem 带 ___ 占位符，correctAnswer 为 "A. xxx"），
            // 历史记录需要展示为词语排序题形式（完整句子）
            if (log.getModuleType() != null && log.getModuleType() == 2 && content != null) {
                String[] display = buildGrammarDisplay(content, userAnswer);
                stem = display[0];
                correctAnswer = display[1];
                userAnswer = display[2];
            }

            return ExerciseHistoryVO.builder()
                    .logId(log.getId())
                    .questionId(log.getQuestionId())
                    .stem(stem)
                    .userAnswer(userAnswer)
                    .correctAnswer(correctAnswer)
                    .isCorrect(log.getIsCorrect())
                    .durationSeconds(log.getDurationSeconds())
                    .moduleType(log.getModuleType())
                    .questionType(info != null ? info.getQuestionType() : null)
                    .analysis(content != null ? content.getAnalysis() : null)
                    .difficultyLevel(info != null ? info.getDifficultyLevel() : null)
                    .createdAt(log.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public long getExerciseHistoryCount(Long userId, Integer moduleType, Integer isCorrect, String startTime) {
        LambdaQueryWrapper<UserExerciseLog> wrapper = new LambdaQueryWrapper<UserExerciseLog>()
                .eq(UserExerciseLog::getUserId, userId);

        if (moduleType != null && moduleType != 0) {
            wrapper.eq(UserExerciseLog::getModuleType, moduleType);
        }
        if (isCorrect != null && isCorrect >= 0) {
            wrapper.eq(UserExerciseLog::getIsCorrect, isCorrect);
        }
        if (startTime != null && !startTime.isBlank()) {
            try {
                LocalDateTime start = LocalDateTime.parse(startTime);
                wrapper.ge(UserExerciseLog::getCreatedAt, start);
            } catch (Exception e) {
                log.warn("解析 startTime 失败: {}", startTime);
            }
        }

        return exerciseLogMapper.selectCount(wrapper);
    }

    /**
     * 对语法题（moduleType=2）做展示转换：
     * 数据库中语法题以选择题格式存储（stem 带 ___ 占位符，correctAnswer 为 "A. xxx"），
     * 此方法将其还原为词语排序题的展示形式：
     * 1. 从 correctAnswer 中去掉选项前缀（如 "A. "），提取纯文本
     * 2. 用纯文本替换 stem 中的占位符（___、[BLANK]、...），生成完整句子
     * 3. 如果 userAnswer 是选项字母（如 "C"），从 options 中提取对应选项文本
     *
     * @param content    题目内容
     * @param userAnswer 用户原始答案
     * @return String[3]，[0]=完整句子 stem，[1]=纯文本 correctAnswer，[2]=转换后的 userAnswer
     */
    private String[] buildGrammarDisplay(QuestionContent content, String userAnswer) {
        String stem = content.getStem();
        String correctAnswer = content.getCorrectAnswer();

        if (stem == null || correctAnswer == null) {
            return new String[]{stem, correctAnswer, userAnswer};
        }

        // 从 correctAnswer 中去掉选项前缀（如 "A. "、"B、 "、"C) "）
        String correctText = stripOptionPrefix(correctAnswer);

        // 如果 correctText 仍然是单个字母（A-D），说明数据库中只存储了选项字母，
        // 需要从 options 中提取对应选项的文本
        if (correctText != null && correctText.length() == 1 
                && Character.toUpperCase(correctText.charAt(0)) >= 'A' 
                && Character.toUpperCase(correctText.charAt(0)) <= 'D') {
            String optionText = getOptionTextByKey(content.getOptions(), correctText.toUpperCase());
            if (optionText != null && !optionText.isEmpty()) {
                correctText = optionText;
            }
        }

        // 用选项文本替换 stem 中的占位符（___两个以上下划线、[BLANK]、...三个以上点）
        String replaced = stem.replaceAll("_{2,}|\\[BLANK\\]|\\.{3,}", correctText);

        // 清理内联选项字母：无论是否替换了占位符，都需要移除 stem 中直接包含的大写选项字母 A-D
        // 避免选项字母残留（如 "Only C C solve" → "Only can we solve"）
        String cleaned = replaced.replaceAll("\\b[A-D]\\b", correctText);

        String fullStem = cleaned.replaceAll("\\s+", " ")
                .trim();

        // 处理 userAnswer：如果是字母（A/B/C/D）或 "A. xxx" 格式，转为纯文本
        // 优先从 options 中提取对应选项的原文单词，避免用选项字母代替原文
        String displayUserAnswer = userAnswer;
        if (userAnswer != null && !userAnswer.isEmpty()) {
            String userKey = extractAnswerKey(userAnswer);
            // 如果提取出的 key 是单字母 A-D，说明是选择题格式，从 options 中提取对应选项文本
            if (userKey.length() == 1 && userKey.charAt(0) >= 'A' && userKey.charAt(0) <= 'D') {
                String optionText = getOptionTextByKey(content.getOptions(), userKey);
                if (optionText != null) {
                    displayUserAnswer = optionText;
                } else {
                    // options 解析失败兜底：保留用户原始答案（字母），不做"选项X"的伪装替换
                    displayUserAnswer = userAnswer;
                }
            }
        }

        return new String[]{fullStem, correctText, displayUserAnswer};
    }

    /**
     * 去掉答案文本的选项前缀（如 "A. "、"B、 "、"C) "、"D " → 纯文本）
     * 如果不以选项字母开头，说明已经是纯文本，不做处理
     */
    private String stripOptionPrefix(String answer) {
        if (answer == null || answer.isEmpty()) {
            return answer;
        }
        char first = Character.toUpperCase(answer.charAt(0));
        if (first >= 'A' && first <= 'D') {
            String removed = answer.replaceFirst("^[A-Da-d][.、)\\s]+\\s*", "").trim();
            if (!removed.isEmpty()) {
                return removed;
            }
        }
        return answer;
    }

    /**
     * 从 options JSON 字符串中，按选项字母（A/B/C/D）提取对应选项的纯文本
     * options 格式示例：["A. established", "B. establish", "C. establishes", "D. establishing"]
     * 也兼容无前缀格式：["established", "establish", "establishes", "establishing"]
     *
     * @param optionsJson options JSON 字符串
     * @param key         选项字母（A/B/C/D）
     * @return 选项纯文本，解析失败返回 null
     */
    private String getOptionTextByKey(String optionsJson, String key) {
        if (optionsJson == null || optionsJson.isEmpty() || key == null || key.isEmpty()) {
            return null;
        }
        try {
            // 解析 options JSON 数组
            List<String> options = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(optionsJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            int idx = key.toUpperCase().charAt(0) - 'A';
            if (idx >= 0 && idx < options.size()) {
                return stripOptionPrefix(options.get(idx));
            }
        } catch (Exception e) {
            // JSON 解析失败，尝试按换行符分割
            String[] lines = optionsJson.split("\\n|\\\\n");
            int idx = key.toUpperCase().charAt(0) - 'A';
            if (idx >= 0 && idx < lines.length) {
                return stripOptionPrefix(lines[idx].replaceFirst("^[A-Da-d][.、)\\s]+\\s*", "").trim());
            }
        }
        return null;
    }

    // ============= 错题驱动同类题生成 =============

    /**
     * 获取用户薄弱知识点列表
     * 查询用户所有未解决错题，统计每个知识点出现次数，按次数降序返回前10个
     */
    @Override
    public List<WeakKnowledgePoint> getWeakKnowledgePoints(Long userId) {
        // 1. 查询用户所有未解决错题（isResolved=0）
        List<UserErrorBook> errors = errorBookMapper.selectList(
                new LambdaQueryWrapper<UserErrorBook>()
                        .eq(UserErrorBook::getUserId, userId)
                        .eq(UserErrorBook::getIsResolved, 0));
        if (errors.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取所有错题ID
        List<Long> questionIds = errors.stream()
                .map(UserErrorBook::getQuestionId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 查询题目-知识点映射关系（多对多）
        List<KnowledgeMapping> mappings = knowledgeMappingMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMapping>()
                        .in(KnowledgeMapping::getQuestionId, questionIds));
        if (mappings.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 统计每个知识点的错题出现次数
        Map<Long, Integer> countMap = new HashMap<>();
        for (KnowledgeMapping mapping : mappings) {
            countMap.merge(mapping.getKnowledgePointId(), 1, Integer::sum);
        }

        // 5. 批量查询知识点名称
        List<Long> kpIds = new ArrayList<>(countMap.keySet());
        Map<Long, String> nameMap = new HashMap<>();
        if (!kpIds.isEmpty()) {
            List<SysKnowledgePoint> kps = knowledgePointMapper.selectBatchIds(kpIds);
            for (SysKnowledgePoint kp : kps) {
                nameMap.put(kp.getId(), kp.getName());
            }
        }

        // 6. 按错误次数降序排序，取前10个
        Map<Long, String> finalNameMap = nameMap;
        return countMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> WeakKnowledgePoint.builder()
                        .knowledgePointId(entry.getKey())
                        .knowledgePointName(finalNameMap.getOrDefault(entry.getKey(), "未知知识点"))
                        .errorCount(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 根据错题生成同类强化练习题
     * 优先从正式题库中取同知识点、同难度、同题型的题目，
     * 不足部分通过 AI 生成并直接入库（无需审核，标记为PENDING）
     */
    @Override
    public List<QuestionVO> generateSimilarQuestions(Long userId, Long questionId, int count) {
        if (count <= 0 || count > 20) {
            count = 5;
        }

        // 1. 获取原题信息
        QuestionInfo questionInfo = questionInfoMapper.selectById(questionId);
        if (questionInfo == null) {
            throw new RuntimeException("题目不存在");
        }

        // 2. 查询该题关联的知识点
        List<KnowledgeMapping> mappings = knowledgeMappingMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMapping>()
                        .eq(KnowledgeMapping::getQuestionId, questionId));
        if (mappings.isEmpty()) {
            // 无知识点映射，退回使用智能选题（同模块、同题型）
            return selectQuestions(userId, questionInfo.getModuleType(), count);
        }

        Long knowledgePointId = mappings.get(0).getKnowledgePointId();
        SysKnowledgePoint kp = knowledgePointMapper.selectById(knowledgePointId);
        String kpName = kp != null ? kp.getName() : "未知知识点";

        // 3. 从正式题库中查询同类题目（同知识点、同模块、同题型、同难度，排除自身）
        List<QuestionInfo> existing = questionInfoMapper.selectList(
                new LambdaQueryWrapper<QuestionInfo>()
                        .eq(QuestionInfo::getKnowledgePointId, knowledgePointId)
                        .eq(QuestionInfo::getModuleType, questionInfo.getModuleType())
                        .eq(QuestionInfo::getQuestionType, questionInfo.getQuestionType())
                        .eq(QuestionInfo::getIsDeleted, 0)
                        .ne(QuestionInfo::getId, questionId)
                        .orderByAsc(QuestionInfo::getUsageCount)
                        .last("LIMIT " + count));

        List<QuestionVO> result = new ArrayList<>();

        // 4. 已有正式题库题目 → 优先使用
        for (QuestionInfo qi : existing) {
            result.add(toQuestionVO(qi, false));
            // 更新使用次数
            qi.setUsageCount(qi.getUsageCount() + 1);
            questionInfoMapper.updateById(qi);
        }

        int remaining = count - result.size();
        if (remaining <= 0) {
            log.info("同类题生成: 正式题库已有足够题目，共{}题", result.size());
            return result;
        }

        // 5. 正式题库不足 → 调用 AI 生成剩余题目
        String questionType = questionInfo.getQuestionType();
        if (questionType == null || questionType.isBlank()) {
            questionType = "FILL_BLANK";
        }

        log.info("同类题生成: 正式题库{}题，需AI生成{}题, knowledgePoint={}, difficulty={}, type={}",
                result.size(), remaining, kpName, questionInfo.getDifficultyLevel(), questionType);

        List<QuestionPending> generated = questionGenerateService.generateAndSaveDirectly(
                kpName,
                questionInfo.getTargetLevel(),
                questionInfo.getDifficultyLevel(),
                questionType,
                questionInfo.getModuleType(),
                remaining);

        // 6. 将生成的待审核题目转换为 QuestionVO 返回给前端（含答案，供练习用）
        for (QuestionPending pending : generated) {
            QuestionVO vo = QuestionVO.builder()
                    .id(pending.getId()) // 使用 pending 的 ID 作为临时ID
                    .moduleType(pending.getModuleType())
                    .difficultyLevel(pending.getDifficultyLevel())
                    .questionType(pending.getQuestionType())
                    .stem(pending.getStem())
                    .options(pending.getOptions())
                    .correctAnswer(pending.getCorrectAnswer()) // 练习模式下展示答案
                    .analysis(pending.getAnalysis())
                    .showAnswer(true)
                    .build();
            result.add(vo);
        }

        log.info("同类题生成完成: 共返回{}题（正式题库{}题 + AI生成{}题）",
                result.size(), result.size() - generated.size(), generated.size());
        return result;
    }
}
