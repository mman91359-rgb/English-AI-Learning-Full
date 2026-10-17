package com.english.ai.exercise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.english.ai.common.Result;
import com.english.ai.exercise.entity.KnowledgeMapping;
import com.english.ai.exercise.entity.QuestionContent;
import com.english.ai.exercise.entity.QuestionInfo;
import com.english.ai.exercise.entity.QuestionPending;
import com.english.ai.exercise.entity.SysKnowledgePoint;
import com.english.ai.exercise.mapper.KnowledgeMappingMapper;
import com.english.ai.exercise.mapper.QuestionContentMapper;
import com.english.ai.exercise.mapper.QuestionInfoMapper;
import com.english.ai.exercise.mapper.QuestionPendingMapper;
import com.english.ai.exercise.mapper.SysKnowledgePointMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI 题目生成服务
 * 负责：
 *  1. 调用 ai-service 批量生成题目，保存到待审核表
 *  2. 审核通过 → 将题目正式入库到 question_info + question_content + knowledge_mapping
 *  3. 审核驳回 → 更新状态和审核意见
 *  4. 查询待审核题目列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerateService {

    private final QuestionPendingMapper pendingMapper;
    private final QuestionInfoMapper questionInfoMapper;
    private final QuestionContentMapper questionContentMapper;
    private final KnowledgeMappingMapper knowledgeMappingMapper;
    private final SysKnowledgePointMapper knowledgePointMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** ai-service 服务地址，从配置文件注入 */
    @Value("${service.ai.url}")
    private String aiServiceUrl;

    /**
     * AI 生成并保存题目到待审核表
     *
     * @param knowledgePoint 知识点名称
     * @param cetLevel       目标等级 CET4/CET6
     * @param difficulty     难度 1~8
     * @param questionType   题型 FILL_BLANK/SENTENCE_MAKING
     * @param count          生成数量（上限20）
     * @return 成功保存的题目数量
     */
    public int generateAndSave(String knowledgePoint, String cetLevel, int difficulty,
                               String questionType, int count) {
        // 1. 调用 ai-service 生成题目
        String aiResultJson = callAiGenerate(knowledgePoint, cetLevel, difficulty, questionType, count);
        if (aiResultJson == null || aiResultJson.isBlank() || "[]".equals(aiResultJson)) {
            log.warn("AI 返回空题目列表，生成失败: knowledgePoint={}", knowledgePoint);
            return 0;
        }

        // 2. 判断模块类型：词汇填空是词汇模块（1），连词成句是语法模块（2）
        int moduleType = "FILL_BLANK".equals(questionType) ? 1 : 2;

        // 3. 解析 AI 返回的 JSON 数组
        List<Map<String, Object>> questions;
        try {
            questions = objectMapper.readValue(aiResultJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析 AI 返回 JSON 失败: {}", aiResultJson, e);
            return 0;
        }

        if (questions == null || questions.isEmpty()) {
            log.warn("解析后题目列表为空");
            return 0;
        }

        // 4. 逐题保存到 question_pending
        int savedCount = 0;
        for (Map<String, Object> q : questions) {
            try {
                QuestionPending pending = new QuestionPending();
                pending.setModuleType(moduleType);
                pending.setTargetLevel(cetLevel);
                pending.setDifficultyLevel(difficulty);
                pending.setQuestionType(questionType);
                pending.setKnowledgePointName(knowledgePoint);
                pending.setStatus("PENDING");

                // 提取字段
                Object stemObj = q.get("stem");
                Object optionsObj = q.get("options");
                Object correctAnswerObj = q.get("correctAnswer");
                Object analysisObj = q.get("analysis");

                if (stemObj != null) pending.setStem(stemObj.toString());
                if (correctAnswerObj != null) pending.setCorrectAnswer(correctAnswerObj.toString());
                if (analysisObj != null) pending.setAnalysis(analysisObj.toString());

                // options 需要序列化为 JSON 字符串存储
                if (optionsObj != null) {
                    pending.setOptions(objectMapper.writeValueAsString(optionsObj));
                }

                pendingMapper.insert(pending);
                savedCount++;
                log.debug("保存待审核题目成功: id={}", pending.getId());
            } catch (Exception e) {
                log.error("单题保存失败，跳过: {}", e.getMessage());
            }
        }

        log.info("AI生成题目完成，共{}题，成功保存{}题到待审核表", count, savedCount);
        return savedCount;
    }

    /**
     * 调用 ai-service 的 /ai/generate-questions 接口获取题目
     */
    private String callAiGenerate(String knowledgePoint, String cetLevel, int difficulty,
                                   String questionType, int count) {
        String url = aiServiceUrl + "/ai/generate-questions";
        Map<String, Object> request = Map.of(
            "knowledgePoint", knowledgePoint,
            "cetLevel", cetLevel,
            "difficulty", difficulty,
            "questionType", questionType,
            "count", count
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            Result<String> response = restTemplate.postForObject(url, entity, Result.class);
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                return response.getData();
            }
            log.warn("AI 服务返回失败: {}", response != null ? response.getMessage() : "null response");
            return null;
        } catch (Exception e) {
            log.error("调用 AI 服务异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 审核通过，将待审核题目正式入库到正式题目表
     *
     * @param pendingId  待审核题目ID
     * @param reviewerId 审核人ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean approveQuestion(Long pendingId, Long reviewerId) {
        // 1. 查询待审核题目
        QuestionPending pending = pendingMapper.selectById(pendingId);
        if (pending == null) {
            log.warn("待审核题目不存在: pendingId={}", pendingId);
            return false;
        }

        // 2. 更新待审核状态为已通过
        pending.setStatus("APPROVED");
        pending.setReviewerId(reviewerId);
        pendingMapper.updateById(pending);

        // 3. 根据知识点名称查找或创建知识点
        Long knowledgePointId = findOrCreateKnowledgePoint(pending.getKnowledgePointName(), pending.getTargetLevel());

        // 4. 插入 question_info（题目元数据）
        QuestionInfo info = new QuestionInfo();
        info.setModuleType(pending.getModuleType());
        info.setTargetLevel(pending.getTargetLevel());
        info.setDifficultyLevel(pending.getDifficultyLevel());
        info.setQuestionType(pending.getQuestionType());
        info.setKnowledgePointId(knowledgePointId);
        info.setSource("AI");              // AI生成来源
        info.setAiGenerated(1);            // AI生成标记
        info.setUsageCount(0);             // 初始使用次数为0
        questionInfoMapper.insert(info);
        Long questionId = info.getId();

        // 5. 插入 question_content（题目内容）
        QuestionContent content = new QuestionContent();
        content.setQuestionId(questionId);
        content.setStem(pending.getStem());
        content.setOptions(pending.getOptions());
        content.setCorrectAnswer(pending.getCorrectAnswer());
        content.setAnalysis(pending.getAnalysis());
        questionContentMapper.insert(content);

        // 6. 插入题目-知识点映射
        KnowledgeMapping mapping = new KnowledgeMapping();
        mapping.setQuestionId(questionId);
        mapping.setKnowledgePointId(knowledgePointId);
        knowledgeMappingMapper.insert(mapping);

        log.info("审核通过，题目已正式入库: pendingId={}, questionId={}", pendingId, questionId);
        return true;
    }

    /**
     * 根据知识点名称查找已存在的知识点，如果不存在则创建新知识点
     *
     * @param knowledgePointName 知识点名称
     * @param cetLevel           考试级别
     * @return 知识点ID
     */
    private Long findOrCreateKnowledgePoint(String knowledgePointName, String cetLevel) {
        // 先按名称和级别查找
        LambdaQueryWrapper<SysKnowledgePoint> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysKnowledgePoint::getName, knowledgePointName)
                    .eq(SysKnowledgePoint::getCetLevel, cetLevel);
        List<SysKnowledgePoint> points = knowledgePointMapper.selectList(queryWrapper);
        if (!points.isEmpty()) {
            return points.get(0).getId();
        }

        // 不存在，创建新知识点
        SysKnowledgePoint newPoint = new SysKnowledgePoint();
        newPoint.setName(knowledgePointName);
        newPoint.setParentId(0L); // 先作为一级节点，后续可手动调整树形结构
        newPoint.setCetLevel(cetLevel);
        newPoint.setSortOrder(0);
        knowledgePointMapper.insert(newPoint);

        log.info("自动创建新知识点: id={}, name={}, cetLevel={}",
                newPoint.getId(), knowledgePointName, cetLevel);
        return newPoint.getId();
    }

    /**
     * 审核驳回题目
     *
     * @param pendingId  待审核题目ID
     * @param reviewerId 审核人ID
     * @param comment    审核意见（驳回原因）
     * @return 是否成功
     */
    public boolean rejectQuestion(Long pendingId, Long reviewerId, String comment) {
        QuestionPending pending = pendingMapper.selectById(pendingId);
        if (pending == null) {
            log.warn("待审核题目不存在: pendingId={}", pendingId);
            return false;
        }

        pending.setStatus("REJECTED");
        pending.setReviewerId(reviewerId);
        pending.setReviewComment(comment);
        pendingMapper.updateById(pending);

        log.info("审核驳回: pendingId={}, reviewerId={}, comment={}", pendingId, reviewerId, comment);
        return true;
    }

    /**
     * 查询待审核题目列表，按状态筛选
     *
     * @param status 状态：PENDING/APPROVED/REJECTED，null 表示不筛选
     * @return 待审核题目列表
     */
    public List<QuestionPending> getPendingQuestions(String status) {
        LambdaQueryWrapper<QuestionPending> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            queryWrapper.eq(QuestionPending::getStatus, status);
        }
        queryWrapper.orderByDesc(QuestionPending::getCreatedAt);
        return pendingMapper.selectList(queryWrapper);
    }

    /**
     * AI 生成题目并直接保存到 question_pending，返回保存后的题目列表
     * 用于错题驱动同类题生成功能，生成后直接返回给前端练习（无需审核）
     *
     * @param knowledgePointName 知识点名称
     * @param cetLevel           目标等级 CET4/CET6
     * @param difficulty         难度 1~8
     * @param questionType       题型 FILL_BLANK/SENTENCE_MAKING
     * @param moduleType         模块类型 1=词汇, 2=语法
     * @param count              生成数量（上限20）
     * @return 成功保存的题目列表
     */
    public List<QuestionPending> generateAndSaveDirectly(String knowledgePointName, String cetLevel,
                                                          int difficulty, String questionType, int moduleType, int count) {
        // 1. 调用 ai-service 生成题目
        String aiResultJson = callAiGenerate(knowledgePointName, cetLevel, difficulty, questionType, count);
        if (aiResultJson == null || aiResultJson.isBlank() || "[]".equals(aiResultJson)) {
            log.warn("AI 返回空题目列表，生成失败: knowledgePoint={}", knowledgePointName);
            return Collections.emptyList();
        }

        // 2. 解析 AI 返回的 JSON 数组
        List<Map<String, Object>> questions;
        try {
            questions = objectMapper.readValue(aiResultJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析 AI 返回 JSON 失败: {}", aiResultJson, e);
            return Collections.emptyList();
        }

        if (questions == null || questions.isEmpty()) {
            log.warn("解析后题目列表为空");
            return Collections.emptyList();
        }

        // 3. 逐题保存到 question_pending，状态为 PENDING（待后续审核，但可立即使用）
        List<QuestionPending> savedList = new ArrayList<>();
        for (Map<String, Object> q : questions) {
            try {
                QuestionPending pending = new QuestionPending();
                pending.setModuleType(moduleType);
                pending.setTargetLevel(cetLevel);
                pending.setDifficultyLevel(difficulty);
                pending.setQuestionType(questionType);
                pending.setKnowledgePointName(knowledgePointName);
                pending.setStatus("PENDING");

                // 提取字段
                Object stemObj = q.get("stem");
                Object optionsObj = q.get("options");
                Object correctAnswerObj = q.get("correctAnswer");
                Object analysisObj = q.get("analysis");

                if (stemObj != null) pending.setStem(stemObj.toString());
                if (correctAnswerObj != null) pending.setCorrectAnswer(correctAnswerObj.toString());
                if (analysisObj != null) pending.setAnalysis(analysisObj.toString());

                // options 需要序列化为 JSON 字符串存储
                if (optionsObj != null) {
                    pending.setOptions(objectMapper.writeValueAsString(optionsObj));
                }

                pendingMapper.insert(pending);
                savedList.add(pending);
                log.debug("AI生成题目直接保存成功: id={}", pending.getId());
            } catch (Exception e) {
                log.error("单题保存失败，跳过: {}", e.getMessage());
            }
        }

        log.info("AI生成同类题完成，共{}题，成功保存{}题到待审核表", count, savedList.size());
        return savedList;
    }
}