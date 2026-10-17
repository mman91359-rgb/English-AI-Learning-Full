-- ==========================================
-- 在 exercise_db 库中执行
-- ==========================================
USE exercise_db;

-- 收藏句表
CREATE TABLE IF NOT EXISTS user_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    sentence_text TEXT NOT NULL COMMENT '收藏的句子/题干',
    question_id BIGINT COMMENT '关联题目ID（可空）',
    source_type VARCHAR(32) DEFAULT 'SENTENCE' COMMENT '来源: SENTENCE/EXERCISE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_favorite (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏句';

-- 单词本表
CREATE TABLE IF NOT EXISTS user_vocabulary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    word VARCHAR(255) NOT NULL COMMENT '单词',
    translation TEXT COMMENT '翻译/释义',
    is_mastered TINYINT DEFAULT 0 COMMENT '是否已掌握: 0=未掌握, 1=已掌握',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_vocab (user_id),
    UNIQUE KEY uk_user_word (user_id, word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户单词本';
