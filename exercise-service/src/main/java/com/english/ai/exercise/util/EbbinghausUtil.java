package com.english.ai.exercise.util;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 艾宾浩斯遗忘曲线复习间隔工具类
 *
 * 复习间隔（天）按掌握度等级递增：
 *  等级0 → 1天    （刚加入，次日复习）
 *  等级1 → 2天
 *  等级2 → 4天
 *  等级3 → 7天
 *  等级4 → 15天
 *  等级5 → 30天   （达到最高掌握度，长期复习）
 *
 * 复习逻辑：
 *  - 答对（记住）：掌握度等级 +1，最高到 5
 *  - 答错（没记住）：掌握度等级 重置为 0
 *  - 等级达到 5 且再次答对：视为已掌握，isMastered 置 1
 */
public final class EbbinghausUtil {

    /** 各等级对应的下次复习间隔（天） */
    private static final List<Integer> INTERVALS_DAYS = Arrays.asList(1, 2, 4, 7, 15, 30);

    /** 最高掌握度等级 */
    public static final int MAX_MASTERY = 5;

    private EbbinghausUtil() {}

    /**
     * 计算下次复习时间
     * @param masteryLevel 当前掌握度等级 0-5
     * @return 下次复习时间
     */
    public static LocalDateTime nextReviewAt(int masteryLevel) {
        int idx = Math.max(0, Math.min(masteryLevel, MAX_MASTERY));
        int days = INTERVALS_DAYS.get(idx);
        return LocalDateTime.now().plusDays(days);
    }

    /**
     * 复习答对时的新掌握度等级
     * 等级 +1，上限为 5
     */
    public static int nextMasteryOnCorrect(int currentMastery) {
        return Math.min(MAX_MASTERY, currentMastery + 1);
    }

    /**
     * 复习答错时的新掌握度等级
     * 重置为 0，从头复习
     */
    public static int nextMasteryOnWrong() {
        return 0;
    }

    /**
     * 判断是否已达到"完全掌握"
     * 等级达到 5 且再次答对即视为掌握
     */
    public static boolean isMastered(int masteryLevel, boolean isCorrect) {
        return masteryLevel >= MAX_MASTERY && isCorrect;
    }
}
