package com.english.ai.learning.service;

import com.english.ai.learning.dto.ReviewTaskVO;
import com.english.ai.learning.dto.UpdateCurveRequest;

import java.util.List;
import java.util.Map;

/**
 * 学习引擎服务接口
 */
public interface LearningService {

    /**
     * 更新遗忘曲线（答题后调用）
     */
    void updateCurve(UpdateCurveRequest request);

    /**
     * 获取每日复习任务
     */
    List<ReviewTaskVO> getReviewTasks(Long userId);

    /**
     * 遗忘曲线统计
     */
    Map<String, Object> getCurveStats(Long userId);
}
