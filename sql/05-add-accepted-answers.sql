-- ============================================================
-- 阶段一：语义判对 —— 题目表新增 AI 可接受答案字段
-- ============================================================

-- 1. question_info 表新增 accepted_answers（JSON 数组，存储 AI 学到的可接受答案）
ALTER TABLE `question_info`
    ADD COLUMN `accepted_answers` JSON NULL COMMENT 'AI学习到的可接受答案列表（JSON数组），如 ["circumstance","condition"]'
    AFTER `source`;

-- 2. question_info 表新增 ai_generated 标记（0=人工/导入，1=AI生成的可接受答案待审核）
ALTER TABLE `question_info`
    ADD COLUMN `ai_generated` TINYINT DEFAULT 0 COMMENT '是否AI生成：0=原始题目，1=AI生成的可接受答案待审核'
    AFTER `accepted_answers`;