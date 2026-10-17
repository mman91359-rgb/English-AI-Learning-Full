package com.english.ai.user.service;

/**
 * ELO隐藏分计算器
 *
 * 算法：
 *   期望得分 E = 1 / (1 + 10^((题目难度ELO分 - 用户当前ELO分) / 400))
 *   实际得分 S = 1（答对）或 0（答错）
 *   新ELO分 = 当前ELO分 + K × (S - E)
 *
 * K系数：定级阶段=40（快速收敛），日常练习=24（稳定调整）
 */
public class EloCalculator {

    /** 定级阶段K系数 — 快速收敛到真实水平 */
    private static final int K_ASSESS = 40;

    /** 日常练习K系数 — 稳定微调 */
    private static final int K_PRACTICE = 24;

    /**
     * 将题目难度等级(1~8)映射为ELO分值
     *  1→250, 2→500, 3→750, 4→1000, 5→1250, 6→1500, 7→1750, 8→2000
     */
    public static int difficultyToElo(int difficultyLevel) {
        return Math.max(1, Math.min(8, difficultyLevel)) * 250;
    }

    /**
     * 计算期望得分
     * @param userElo 用户当前ELO分
     * @param questionDifficulty 题目难度等级(1~8)
     */
    public static double expectedScore(int userElo, int questionDifficulty) {
        int questionElo = difficultyToElo(questionDifficulty);
        return 1.0 / (1.0 + Math.pow(10, (questionElo - userElo) / 400.0));
    }

    /**
     * 计算新ELO分（定级模式，K=40）
     */
    public static int calculateAssess(int currentElo, int questionDifficulty, boolean isCorrect) {
        return calculate(currentElo, questionDifficulty, isCorrect, K_ASSESS);
    }

    /**
     * 计算新ELO分（练习模式，K=24）
     */
    public static int calculatePractice(int currentElo, int questionDifficulty, boolean isCorrect) {
        return calculate(currentElo, questionDifficulty, isCorrect, K_PRACTICE);
    }

    private static int calculate(int currentElo, int questionDifficulty, boolean isCorrect, int k) {
        double e = expectedScore(currentElo, questionDifficulty);
        double s = isCorrect ? 1.0 : 0.0;
        int newElo = (int) Math.round(currentElo + k * (s - e));
        // ELO分边界保护
        return Math.max(100, Math.min(2500, newElo));
    }
}
