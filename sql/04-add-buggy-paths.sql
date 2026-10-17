CREATE TABLE IF NOT EXISTS `question_buggy_path` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `question_id` BIGINT NOT NULL COMMENT '关联题目ID',
    `wrong_answer` VARCHAR(500) NOT NULL COMMENT '错误答案',
    `heuristic_hint` VARCHAR(1000) NOT NULL COMMENT '启发式提示（引导思考，不直接给答案）',
    `error_type` VARCHAR(50) COMMENT '错误类型：SPELLING/GRAMMAR/PREPOSITION/ARTICLE/TENSE/VOCABULARY/UNKNOWN',
    `knowledge_point_id` VARCHAR(100) COMMENT '关联知识点ID（多个用逗号分隔）',
    `occurrence_count` INT DEFAULT 1 COMMENT '出现次数',
    `is_ai_generated` TINYINT DEFAULT 0 COMMENT '是否AI生成：0=人工预设，1=AI生成',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_question_id` (`question_id`),
    INDEX `idx_wrong_answer` (`wrong_answer`(100)),
    INDEX `idx_error_type` (`error_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目常见错误路径表(Buggy Paths)';

INSERT INTO `question_buggy_path` (`question_id`, `wrong_answer`, `heuristic_hint`, `error_type`, `knowledge_point_id`, `occurrence_count`, `is_ai_generated`) VALUES
(1, 'estabilish', '注意字母顺序：是 \"establish\" 而不是 \"estabilish\"，结尾是 \"sh\" 不是 \"s\"', 'SPELLING', '1', 5, 0),
(1, 'establishe', '动词第三人称单数才加 \"es\"，这里不需要哦', 'GRAMMAR', '2', 3, 0),
(2, 'effect', '注意区分 \"affect\"（动词，影响）和 \"effect\"（名词，效果），这里需要动词', 'VOCABULARY', '3', 8, 0),
(2, 'affects', '主语是复数 \"they\"，动词不需要加 \"s\"', 'GRAMMAR', '2', 4, 0),
(3, 'a hour', '\"hour\" 虽然以字母 \"h\" 开头，但发音以元音音素开头，应该用 \"an\"', 'ARTICLE', '4', 12, 0),
(3, 'an university', '\"university\" 虽然以元音字母开头，但发音以辅音音素开头，应该用 \"a\"', 'ARTICLE', '4', 6, 0),
(4, 'go to home', '\"home\" 是副词，前面不需要介词 \"to\"，直接说 \"go home\"', 'PREPOSITION', '5', 7, 0),
(4, 'at school', '通常说 \"at school\"，你的答案基本正确，检查一下其他部分', 'PREPOSITION', '5', 2, 0),
(5, 'yesterday I go', '时间状语是 \"yesterday\"，句子应该使用过去时态', 'TENSE', '6', 10, 0),
(5, 'I am go now', '时间状语是 \"now\"，应该用现在进行时态：\"I am going\"', 'TENSE', '6', 5, 0),
(6, 'since three years', '\"since\" 后面应该接时间点，时间段应该用 \"for\"：\"for three years\"', 'PREPOSITION', '5', 9, 0),
(6, 'for 2020', '\"for\" 后面接时间段，时间点应该用 \"since\"：\"since 2020\"', 'PREPOSITION', '5', 4, 0),
(7, 'informations', '\"information\" 是不可数名词，没有复数形式', 'GRAMMAR', '7', 6, 0),
(7, 'advices', '\"advice\" 是不可数名词，表达多条建议可以说 \"pieces of advice\"', 'GRAMMAR', '7', 3, 0),
(8, 'he play', '主语是第三人称单数 \"he\"，动词需要加 \"s\"', 'GRAMMAR', '2', 11, 0),
(8, 'they plays', '主语是复数 \"they\"，动词不需要加 \"s\"', 'GRAMMAR', '2', 5, 0);

CREATE TABLE IF NOT EXISTS `judgment_cache` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `user_answer` VARCHAR(500) NOT NULL COMMENT '用户答案',
    `correct_answer` VARCHAR(500) NOT NULL COMMENT '正确答案',
    `heuristic_hint` VARCHAR(1000) NOT NULL COMMENT '启发式提示',
    `error_type` VARCHAR(50) COMMENT '错误类型',
    `matched_level` INT COMMENT '匹配层级',
    `hit_count` INT DEFAULT 1 COMMENT '命中次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_question_answer` (`question_id`, `user_answer`(100)),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='判题结果缓存表';

INSERT INTO `sys_knowledge_point` (`name`, `parent_id`, `cet_level`, `sort_order`) VALUES
('冠词用法', 0, 'CET4', 1),
('主谓一致', 0, 'CET4', 2),
('近义词辨析', 0, 'CET4', 3),
('介词搭配', 0, 'CET4', 4),
('时态语态', 0, 'CET4', 5),
('名词可数性', 0, 'CET4', 6),
('固定搭配', 0, 'CET4', 7),
('拼写规则', 0, 'CET4', 8),
('冠词用法', 0, 'CET6', 1),
('主谓一致', 0, 'CET6', 2),
('近义词辨析', 0, 'CET6', 3),
('介词搭配', 0, 'CET6', 4),
('时态语态', 0, 'CET6', 5),
('名词可数性', 0, 'CET6', 6),
('固定搭配', 0, 'CET6', 7),
('拼写规则', 0, 'CET6', 8);