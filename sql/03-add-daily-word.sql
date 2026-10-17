-- ==========================================
-- 每日单词功能：系统词库 + 每日一词
-- 在 exercise_db 库中执行
-- ==========================================
USE exercise_db;

-- 系统单词词典（全局词库，含丰富元数据）
CREATE TABLE IF NOT EXISTS word_dictionary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(255) NOT NULL COMMENT '单词',
    phonetic VARCHAR(255) COMMENT '音标',
    translation TEXT COMMENT '中文释义',
    pos VARCHAR(64) COMMENT '词性',
    example TEXT COMMENT '例句(英文)',
    example_translation TEXT COMMENT '例句翻译',
    level VARCHAR(16) DEFAULT 'CET4' COMMENT '等级: CET4/CET6',
    usage_count INT DEFAULT 0 COMMENT '被选为每日单词的次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统单词词典';

-- 每日单词表（固定每天一个单词，全局统一）
CREATE TABLE IF NOT EXISTS daily_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word_id BIGINT NOT NULL COMMENT 'FK to word_dictionary.id',
    date DATE NOT NULL UNIQUE COMMENT '日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日单词';

-- 种子数据：30个CET4/CET6高频词汇
INSERT INTO word_dictionary (word, phonetic, translation, pos, example, example_translation, level) VALUES
('abandon', '/əˈbændən/', 'v. 放弃；抛弃', 'verb', 'He abandoned his plan to travel.', '他放弃了旅行计划。', 'CET4'),
('absolute', '/ˈæbsəluːt/', 'adj. 绝对的；完全的', 'adjective', 'I have absolute trust in her.', '我完全信任她。', 'CET4'),
('accomplish', '/əˈkɑːmplɪʃ/', 'v. 完成；实现', 'verb', 'She accomplished her goal in one year.', '她在一年内完成了目标。', 'CET4'),
('benefit', '/ˈbenɪfɪt/', 'n. 利益；好处 v. 有益于', 'noun', 'Exercise benefits your health.', '锻炼有益健康。', 'CET4'),
('brilliant', '/ˈbrɪliənt/', 'adj. 杰出的；明亮的', 'adjective', 'That was a brilliant idea.', '那是个绝妙的主意。', 'CET4'),
('circumstance', '/ˈsɜːrkəmstæns/', 'n. 环境；情况', 'noun', 'Under no circumstances should you lie.', '任何情况下你都不应该说谎。', 'CET4'),
('comprehensive', '/ˌkɑːmprɪˈhensɪv/', 'adj. 全面的；综合的', 'adjective', 'We need a comprehensive review.', '我们需要一次全面审查。', 'CET4'),
('demonstrate', '/ˈdemənstreɪt/', 'v. 展示；证明', 'verb', 'The experiment demonstrates the theory.', '实验证明了该理论。', 'CET4'),
('essential', '/ɪˈsenʃl/', 'adj. 基本的；必要的', 'adjective', 'Water is essential for life.', '水对生命至关重要。', 'CET4'),
('flexible', '/ˈfleksəbl/', 'adj. 灵活的；柔韧的', 'adjective', 'We offer flexible working hours.', '我们提供灵活的工作时间。', 'CET4'),
('genuine', '/ˈdʒenjuɪn/', 'adj. 真正的；真诚的', 'adjective', 'She showed genuine interest in the project.', '她对项目表现出真正的兴趣。', 'CET4'),
('hesitate', '/ˈhezɪteɪt/', 'v. 犹豫；迟疑', 'verb', 'Do not hesitate to contact us.', '请随时联系我们。', 'CET4'),
('inevitable', '/ɪnˈevɪtəbl/', 'adj. 不可避免的', 'adjective', 'Change is inevitable in life.', '生活中变化是不可避免的。', 'CET6'),
('magnificent', '/mæɡˈnɪfɪsnt/', 'adj. 壮丽的；宏伟的', 'adjective', 'The view from the top was magnificent.', '顶上的景色十分壮丽。', 'CET6'),
('negotiate', '/nɪˈɡoʊʃieɪt/', 'v. 谈判；协商', 'verb', 'They negotiated a peace agreement.', '他们谈判达成了和平协议。', 'CET4'),
('obstacle', '/ˈɑːbstəkl/', 'n. 障碍；妨碍', 'noun', 'Lack of money is the main obstacle.', '缺钱是主要障碍。', 'CET6'),
('phenomenon', '/fɪˈnɑːmɪnən/', 'n. 现象；杰出的人', 'noun', 'This is a natural phenomenon.', '这是一种自然现象。', 'CET6'),
('prosperity', '/prɑːˈsperəti/', 'n. 繁荣；兴旺', 'noun', 'Peace brings prosperity to the nation.', '和平给国家带来繁荣。', 'CET6'),
('reluctant', '/rɪˈlʌktənt/', 'adj. 不情愿的；勉强的', 'adjective', 'He was reluctant to leave his hometown.', '他不愿离开家乡。', 'CET6'),
('sophisticated', '/səˈfɪstɪkeɪtɪd/', 'adj. 复杂的；精密的', 'adjective', 'This is a highly sophisticated system.', '这是一个高度精密的系统。', 'CET6'),
('tremendous', '/trɪˈmendəs/', 'adj. 巨大的；极好的', 'adjective', 'We have made tremendous progress.', '我们取得了巨大进展。', 'CET4'),
('ultimate', '/ˈʌltɪmət/', 'adj. 最终的；根本的', 'adjective', 'The ultimate decision rests with you.', '最终决定权在你手中。', 'CET4'),
('vulnerable', '/ˈvʌlnərəbl/', 'adj. 脆弱的；易受伤害的', 'adjective', 'Children are vulnerable to diseases.', '儿童容易受到疾病侵害。', 'CET6'),
('widespread', '/ˈwaɪdspred/', 'adj. 广泛的；普遍的', 'adjective', 'The news received widespread attention.', '这条新闻受到广泛关注。', 'CET4'),
('accelerate', '/əkˈseləreɪt/', 'v. 加速；促进', 'verb', 'We need to accelerate the process.', '我们需要加快这一进程。', 'CET4'),
('diligent', '/ˈdɪlɪdʒənt/', 'adj. 勤奋的；勤勉的', 'adjective', 'She is a diligent and hardworking student.', '她是一个勤奋刻苦的学生。', 'CET4'),
('enthusiasm', '/ɪnˈθuːziæzəm/', 'n. 热情；热忱', 'noun', 'He showed great enthusiasm for the job.', '他对工作表现出极大的热情。', 'CET4'),
('flourish', '/ˈflɜːrɪʃ/', 'v. 繁荣；兴旺', 'verb', 'The arts began to flourish in the city.', '艺术在这座城市开始繁荣起来。', 'CET6'),
('gratitude', '/ˈɡrætɪtuːd/', 'n. 感激；感谢', 'noun', 'I would like to express my sincere gratitude.', '我想表达诚挚的感谢。', 'CET6'),
('harmony', '/ˈhɑːrməni/', 'n. 和谐；协调', 'noun', 'We should live in harmony with nature.', '我们应该与自然和谐相处。', 'CET4')
ON DUPLICATE KEY UPDATE word=word;
