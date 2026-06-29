package com.english.ai.user.service;

import com.english.ai.user.entity.UserAbilityRadar;

import java.util.Map;

public interface UserAbilityRadarService {

    /** 初始化雷达记录（注册时调用，ELO=1000） */
    void initRadar(Long userId, String targetLevel);

    /** 根据用户ID查询雷达数据 */
    UserAbilityRadar getByUserId(Long userId);

    /** 更新ELO分（答题后调） */
    int updateElo(Long userId, int questionDifficulty, boolean isCorrect, boolean isAssess);

    /** 更新模块维度分（答题后调用，移动加权平均） */
    void updateModuleScore(Long userId, int moduleType, boolean isCorrect);

    /** 获取能力雷达图数据（供前端） */
    Map<String, Object> getRadarData(Long userId);
}
