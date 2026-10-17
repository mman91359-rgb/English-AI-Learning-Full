-- ==========================================
-- 单词本智能化升级：扩展 user_vocabulary 表字段
-- 在 exercise_db 库中执行
-- 新增字段：音标、词性、例句、例句翻译、搭配、来源、
--          复习次数、掌握度、上次复习时间、下次复习时间
-- ==========================================
USE exercise_db;

ALTER TABLE user_vocabulary
    ADD COLUMN phonetic           VARCHAR(100) DEFAULT NULL COMMENT '音标，如 /ˈsɜːkəmstəns/' AFTER translation,
    ADD COLUMN pos                VARCHAR(32)  DEFAULT NULL COMMENT '词性，如 n./v./adj.'        AFTER phonetic,
    ADD COLUMN example            TEXT         DEFAULT NULL COMMENT '英文例句'                  AFTER pos,
    ADD COLUMN example_translation TEXT        DEFAULT NULL COMMENT '例句中文翻译'              AFTER example,
    ADD COLUMN collocation        TEXT         DEFAULT NULL COMMENT '常见搭配，分号分隔'        AFTER example_translation,
    ADD COLUMN source             VARCHAR(32)  DEFAULT 'manual' COMMENT '来源: manual/daily_word/quiz' AFTER collocation,
    ADD COLUMN review_count       INT          DEFAULT 0    COMMENT '累计复习次数'            AFTER source,
    ADD COLUMN mastery_level      INT          DEFAULT 0    COMMENT '掌握度等级0-5，5为已掌握' AFTER review_count,
    ADD COLUMN last_reviewed_at   DATETIME     DEFAULT NULL COMMENT '上次复习时间'             AFTER mastery_level,
    ADD COLUMN next_review_at     DATETIME     DEFAULT NULL COMMENT '下次复习时间（按艾宾浩斯曲线）' AFTER last_reviewed;

-- 为"今日待复习"查询添加索引
ALTER TABLE user_vocabulary
    ADD INDEX idx_user_review (user_id, next_review_at);
