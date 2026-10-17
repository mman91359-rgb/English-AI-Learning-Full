package com.english.ai.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.english.ai.user.entity.UserAbilityRadar;
import com.english.ai.user.mapper.UserAbilityRadarMapper;
import com.english.ai.user.service.EloCalculator;
import com.english.ai.user.service.UserAbilityRadarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAbilityRadarServiceImpl implements UserAbilityRadarService {

    private final UserAbilityRadarMapper radarMapper;

    @Override
    public void initRadar(Long userId, String targetLevel) {
        UserAbilityRadar exist = getByUserId(userId);
        if (exist != null) {
            log.info("雷达记录已存在: userId={}", userId);
            return;
        }
        UserAbilityRadar radar = new UserAbilityRadar();
        radar.setUserId(userId);
        radar.setVocabularyScore(BigDecimal.ZERO);
        radar.setGrammarScore(BigDecimal.ZERO);
        radar.setListeningScore(BigDecimal.ZERO);
        radar.setReadingScore(BigDecimal.ZERO);
        radar.setEloScore(1000);
        radar.setTargetLevel(targetLevel);
        radarMapper.insert(radar);
        log.info("初始化雷达记录: userId={}, targetLevel={}, elo=1000", userId, targetLevel);
    }

    @Override
    public UserAbilityRadar getByUserId(Long userId) {
        return radarMapper.selectOne(
                new LambdaQueryWrapper<UserAbilityRadar>()
                        .eq(UserAbilityRadar::getUserId, userId));
    }

    @Override
    public int updateElo(Long userId, int questionDifficulty, boolean isCorrect, boolean isAssess) {
        UserAbilityRadar radar = getOrCreateRadar(userId);
        int oldElo = radar.getEloScore();
        int newElo = isAssess
                ? EloCalculator.calculateAssess(oldElo, questionDifficulty, isCorrect)
                : EloCalculator.calculatePractice(oldElo, questionDifficulty, isCorrect);

        radar.setEloScore(newElo);
        radarMapper.updateById(radar);

        log.info("ELO更新: userId={}, difficulty={}, correct={}, isAssess={}, {}→{}",
                userId, questionDifficulty, isCorrect, isAssess, oldElo, newElo);
        return newElo;
    }

    @Override
    public void updateModuleScore(Long userId, int moduleType, boolean isCorrect) {
        UserAbilityRadar radar = getOrCreateRadar(userId);
        BigDecimal current = getModuleScore(radar, moduleType);
        // 移动加权平均: 正确+10%剩余距离, 错误-10%当前值
        double rate = 0.1;
        double newVal = isCorrect
                ? current.doubleValue() + (100.0 - current.doubleValue()) * rate
                : current.doubleValue() * (1.0 - rate);
        newVal = Math.max(0, Math.min(100, newVal));
        BigDecimal newScore = BigDecimal.valueOf(Math.round(newVal * 10) / 10.0);
        setModuleScore(radar, moduleType, newScore);
        radarMapper.updateById(radar);
        log.info("模块分更新: userId={}, moduleType={}, correct={}, {}→{}",
                userId, moduleType, isCorrect, current, newScore);
    }

    private UserAbilityRadar getOrCreateRadar(Long userId) {
        UserAbilityRadar radar = getByUserId(userId);
        if (radar == null) {
            log.warn("雷达记录不存在, 自动创建: userId={}", userId);
            initRadar(userId, "CET4");
            radar = getByUserId(userId);
        }
        return radar;
    }

    private BigDecimal getModuleScore(UserAbilityRadar radar, int moduleType) {
        return switch (moduleType) {
            case 1 -> radar.getVocabularyScore();
            case 2 -> radar.getGrammarScore();
            case 3 -> radar.getListeningScore();
            case 4 -> radar.getReadingScore();
            default -> BigDecimal.ZERO;
        };
    }

    private void setModuleScore(UserAbilityRadar radar, int moduleType, BigDecimal score) {
        switch (moduleType) {
            case 1 -> radar.setVocabularyScore(score);
            case 2 -> radar.setGrammarScore(score);
            case 3 -> radar.setListeningScore(score);
            case 4 -> radar.setReadingScore(score);
        }
    }

    @Override
    public Map<String, Object> getRadarData(Long userId) {
        UserAbilityRadar radar = getByUserId(userId);
        if (radar == null) {
            return defaultRadarData();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("eloScore", radar.getEloScore());
        data.put("eloLevel", eloToLevel(radar.getEloScore()));
        data.put("targetLevel", radar.getTargetLevel());

        data.put("vocabularyScore", radar.getVocabularyScore() != null
                ? radar.getVocabularyScore().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        data.put("grammarScore", radar.getGrammarScore() != null
                ? radar.getGrammarScore().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        data.put("listeningScore", radar.getListeningScore() != null
                ? radar.getListeningScore().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        data.put("readingScore", radar.getReadingScore() != null
                ? radar.getReadingScore().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        // 雷达图最大值100，计算归一化比例
        double maxRadar = 100.0;
        data.put("vocabularyPercent", calcPercent(radar.getVocabularyScore(), maxRadar));
        data.put("grammarPercent", calcPercent(radar.getGrammarScore(), maxRadar));
        data.put("listeningPercent", calcPercent(radar.getListeningScore(), maxRadar));
        data.put("readingPercent", calcPercent(radar.getReadingScore(), maxRadar));

        // ELO等级进度环数据
        int[] range = eloRange(radar.getEloScore());
        data.put("eloFloor", range[0]);
        data.put("eloCeil", range[1]);
        data.put("eloProgress", Math.round((radar.getEloScore() - range[0]) * 100.0 / (range[1] - range[0])));

        return data;
    }

    private Map<String, Object> defaultRadarData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("eloScore", 1000);
        data.put("eloLevel", "初心者");
        data.put("vocabularyScore", 0);
        data.put("grammarScore", 0);
        data.put("listeningScore", 0);
        data.put("readingScore", 0);
        data.put("vocabularyPercent", 0);
        data.put("grammarPercent", 0);
        data.put("listeningPercent", 0);
        data.put("readingPercent", 0);
        data.put("eloFloor", 0);
        data.put("eloCeil", 1250);
        data.put("eloProgress", 80);
        return data;
    }

    private double calcPercent(BigDecimal score, double max) {
        if (score == null) return 0;
        return Math.min(100, Math.round(score.doubleValue() / max * 1000.0) / 10.0);
    }

    /** ELO分→等级名称 */
    private String eloToLevel(int elo) {
        if (elo >= 2000) return "宗师";
        if (elo >= 1750) return "大师";
        if (elo >= 1500) return "高手";
        if (elo >= 1250) return "进阶";
        if (elo >= 1000) return "入门";
        return "初心者";
    }

    /** ELO分所在等级区间 */
    private int[] eloRange(int elo) {
        if (elo >= 2000) return new int[]{2000, 2500};
        if (elo >= 1750) return new int[]{1750, 2000};
        if (elo >= 1500) return new int[]{1500, 1750};
        if (elo >= 1250) return new int[]{1250, 1500};
        if (elo >= 1000) return new int[]{1000, 1250};
        return new int[]{0, 1000};
    }
}
