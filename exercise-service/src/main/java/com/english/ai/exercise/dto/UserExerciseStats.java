package com.english.ai.exercise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户答题统计视图对象
 * 用于前端统计页展示热力图、学习时间、词汇量等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExerciseStats {

    /** 总答题数 */
    private Integer totalAnswered;

    /** 正确答题数 */
    private Integer correctCount;

    /** 错误答题数 */
    private Integer errorCount;

    /** 正确率（百分比，如 78.5） */
    private Double correctRate;

    /** 累计学习时长（秒） */
    private Integer totalDurationSeconds;

    /** 累计学习时长（小时） */
    private Double totalHours;

    /** 累计学习时长（分钟，去除小时后的余数） */
    private Integer totalMinutes;

    /** 连续学习天数 */
    private Integer streakDays;

    /** 累计活跃天数（有答题记录的天数） */
    private Integer activeDays;

    /** 已掌握单词数（基于正确答题数估算） */
    private Integer masteredWords;

    /** 全国排名（基于正确率估算的百分位排名，如 85.0 代表超过85%用户） */
    private Double nationalRankPercent;

    /** 词汇理解度（正确率*100） */
    private Double vocabularyComprehension;

    /** 年度热力图数据（一年365天，每天一个活跃度值0~5） */
    private List<DailyActivity> heatmapData;

    /** 最近7天学习时长（分钟） */
    private List<Integer> recentWeekMinutes;

    /** 本周平均每天答题数（最近7天每日答题数的平均值，四舍五入取整） */
    private Integer weeklyAverageQuestions;

    /** 本年 1~12 月每月答题总数（12个元素，下标0对应1月） */
    private List<Integer> yearlyMonthQuestions;

    /** 年度每日学习时长（分钟，过去365天，按日期升序） */
    private List<Integer> yearlyStudyMinutes;

    /** 今日学习时长（秒） */
    private Integer todayDurationSeconds;

    /** 24小时学习分布（0~23时，每小时的累计学习时长/分钟） */
    private List<Integer> hourlyStudyMinutes;

    /** 今日答题总数 */
    private Integer todayAnsweredCount;

    /** 今日词汇答题数（模块类型=1） */
    private Integer todayVocabAnswered;

    /** 今日语法答题数（模块类型=2） */
    private Integer todayGrammarAnswered;

    /**
     * 每日活跃度
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyActivity {
        /** 日期（ISO格式：2026-01-01） */
        private String date;
        /** 答题数量 */
        private Integer questionCount;
        /** 活跃等级：0=未学习，1=少，2=中，3=较多，4=多，5=很多 */
        private Integer activityLevel;
    }
}
