-- ============================================================
-- 06-add-pending-questions.sql
-- 功能描述：创建 AI 生成待审核题目表，用于"AI 离线批量生成新题"功能
-- 创建时间：2026-07-02
-- ============================================================

-- 创建待审核题目表
CREATE TABLE IF NOT EXISTS `question_pending` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `module_type` INT NOT NULL COMMENT '模块类型: 1=词汇, 2=语法',
    `target_level` VARCHAR(10) NOT NULL COMMENT '目标等级: CET4/CET6',
    `difficulty_level` INT DEFAULT 3 COMMENT '难度等级: 1~8',
    `question_type` VARCHAR(20) NOT NULL COMMENT '题型: FILL_BLANK/SENTENCE_MAKING',
    `stem` TEXT NOT NULL COMMENT '题干',
    `options` VARCHAR(2000) COMMENT '选项(JSON数组)',
    `correct_answer` VARCHAR(500) NOT NULL COMMENT '正确答案',
    `analysis` TEXT COMMENT '题目解析',
    `knowledge_point_name` VARCHAR(200) COMMENT '关联知识点名称',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING=待审核, APPROVED=已通过, REJECTED=已驳回',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `review_comment` VARCHAR(500) COMMENT '审核意见',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_status` (`status`),
    INDEX `idx_module_type` (`module_type`),
    INDEX `idx_target_level` (`target_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成待审核题目表';