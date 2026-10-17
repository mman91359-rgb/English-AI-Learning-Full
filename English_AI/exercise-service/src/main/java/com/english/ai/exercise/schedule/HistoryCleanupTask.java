package com.english.ai.exercise.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.english.ai.exercise.entity.UserExerciseLog;
import com.english.ai.exercise.mapper.UserExerciseLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 学习记录定时清理任务
 * 每天凌晨2点执行，删除30天前的学习记录，保持学习历史数量无上限但有时效
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryCleanupTask {

    private final UserExerciseLogMapper exerciseLogMapper;

    /** 学习历史保留天数 */
    private static final int RETENTION_DAYS = 30;

    /**
     * 每天凌晨2点清理30天前的学习记录
     * cron 表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        try {
            int deleted = exerciseLogMapper.delete(
                    new LambdaQueryWrapper<UserExerciseLog>()
                            .lt(UserExerciseLog::getCreatedAt, cutoff));
            log.info("学习记录定时清理完成，删除 {} 条{}天前的记录", deleted, RETENTION_DAYS);
        } catch (Exception e) {
            log.error("学习记录定时清理失败", e);
        }
    }
}
