import { get, post, del, put } from './request'

/**
 * 题目视图对象
 */
export interface QuestionVO {
    /** 题目ID */
    id: number
    /** 模块类型: 1=词汇, 2=语法 */
    moduleType: number
    /** 难度等级: 1~8 */
    difficultyLevel: number
    /** 题型 */
    questionType: string
    /** 题干 */
    stem: string
    /** 选项(JSON字符串) */
    options: string
    /** 正确答案（练习模式不返回，提交后才展示） */
    correctAnswer?: string
    /** 提示（正确答案前2个字母，练习模式可用） */
    hint?: string
    /** 题目解析 */
    analysis?: string
    /** 是否展示答案 */
    showAnswer?: boolean
}

/**
 * 提交答案请求
 */
export interface SubmitRequest {
    userId: number
    questionId: number
    userAnswer: string
    moduleType: number
    durationSeconds?: number
}

/**
 * 提交答案响应
 */
export interface SubmitResponse {
    /** 是否正确 */
    isCorrect: boolean
    /** 用户提交的答案（部分页面本地兜底使用） */
    userAnswer?: string
    /** 正确答案（完整文本） */
    correctAnswer: string
    /** 正确答案选项（仅字母） */
    correctAnswerKey: string
    /** 题目解析 */
    analysis: string
    /** 追踪ID */
    traceId: string
    /** 启发式反馈提示 */
    heuristicHint?: string
    /** 错误类型 */
    errorType?: string
    /** 匹配层级 */
    matchedLevel?: number
    /** 额外建议列表 */
    suggestions?: string[]
    /** 是否为语义匹配（近义词/同义表达被判对），此时应展示标准答案供参考 */
    isSemanticMatch?: boolean
}

/**
 * 知识点
 */
export interface SysKnowledgePoint {
    id: number
    name: string
    parentId?: number
    cetLevel: string
    sortOrder: number
}

/**
 * 智能选题
 * GET /exercise/questions?userId=1&moduleType=1&count=10
 */
export const selectQuestions = (userId: number, moduleType: number = 1, count: number = 10): Promise<QuestionVO[]> => {
    return get('/exercise/questions', { userId, moduleType, count })
}

/**
 * 定级测试题
 * GET /exercise/assess-questions
 */
export const getAssessQuestions = (): Promise<QuestionVO[]> => {
    return get('/exercise/assess-questions')
}

/**
 * 单题详情
 * GET /exercise/question/{id}
 */
export const getQuestionById = (id: number, showAnswer: boolean = false): Promise<QuestionVO> => {
    return get(`/exercise/question/${id}`, { showAnswer })
}

/**
 * 提交答案
 * POST /exercise/submit
 */
export const submitAnswer = (data: SubmitRequest): Promise<SubmitResponse> => {
    return post('/exercise/submit', data)
}

/**
 * 错题本
 * GET /exercise/error-book/{userId}
 */
export const getErrorBook = (userId: number): Promise<QuestionVO[]> => {
    return get(`/exercise/error-book/${userId}`)
}

/**
 * 知识点树
 * GET /exercise/knowledge-points?cetLevel=CET4
 */
export const getKnowledgePoints = (cetLevel: string = 'CET4'): Promise<SysKnowledgePoint[]> => {
    return get('/exercise/knowledge-points', { cetLevel })
}

/**
 * 用户答题统计返回类型
 */
export interface UserExerciseStats {
    totalAnswered: number
    correctCount: number
    errorCount: number
    correctRate: number
    totalDurationSeconds: number
    totalHours: number
    totalMinutes: number
    todayDurationSeconds: number
    streakDays: number
    activeDays: number
    masteredWords: number
    nationalRankPercent: number
    vocabularyComprehension: number
    heatmapData: { date: string; questionCount: number; activityLevel: number; title?: string }[]
    recentWeekMinutes: number[]
    weeklyAverageQuestions: number
    yearlyMonthQuestions: number[]
    yearlyStudyMinutes: number[]
    hourlyStudyMinutes: number[]
    todayAnsweredCount: number
    todayVocabAnswered: number
    todayGrammarAnswered: number
}

/**
 * 用户答题统计
 * GET /exercise/stats/{userId}
 */
export const getExerciseStats = (userId: number): Promise<UserExerciseStats> => {
    return get(`/exercise/stats/${userId}`)
}

// ========== 收藏句 ==========

export interface FavoriteVO {
    id: number
    userId: number
    sentenceText: string
    questionId?: number
    sourceType: string
    createdAt: string
}

export const addFavorite = (data: { userId: number; sentenceText: string; questionId?: number }): Promise<FavoriteVO> => {
    return post('/exercise/favorites', data)
}

export const getFavorites = (userId: number): Promise<FavoriteVO[]> => {
    return get(`/exercise/favorites/${userId}`)
}

export const removeFavorite = (id: number, userId: number): Promise<string> => {
    return del(`/exercise/favorites/${id}?userId=${userId}`)
}

// ========== 单词本 ==========

export interface VocabularyVO {
    id: number
    userId: number
    word: string
    translation: string
    /** 音标 */
    phonetic?: string
    /** 词性 */
    pos?: string
    /** 英文例句 */
    example?: string
    /** 例句中文翻译 */
    exampleTranslation?: string
    /** 常见搭配 */
    collocation?: string
    /** 来源: manual/daily_word/quiz */
    source?: string
    /** 累计复习次数 */
    reviewCount?: number
    /** 掌握度等级 0-5 */
    masteryLevel?: number
    /** 上次复习时间 */
    lastReviewedAt?: string
    /** 下次复习时间 */
    nextReviewAt?: string
    isMastered: number
    createdAt: string
    updatedAt: string
}

/** 智能添加单词请求参数 */
export interface AddVocabularySmartRequest {
    userId: number
    word: string
    /** 来源: manual/daily_word/quiz */
    source?: string
}

/** 复习结果请求参数 */
export interface ReviewResultRequest {
    userId: number
    isCorrect: boolean
}

export const addVocabulary = (data: { userId: number; word: string; translation?: string }): Promise<VocabularyVO> => {
    return post('/exercise/vocabulary', data)
}

/**
 * 智能添加单词：只传 userId 和 word，AI 自动补全音标/词性/释义/例句/搭配
 * 并初始化艾宾浩斯复习计划（next_review_at = 明天）
 */
export const addVocabularySmart = (data: AddVocabularySmartRequest): Promise<VocabularyVO> => {
    return post('/exercise/vocabulary/smart', data)
}

export const getVocabulary = (userId: number): Promise<VocabularyVO[]> => {
    return get(`/exercise/vocabulary/${userId}`)
}

export const removeVocabulary = (id: number, userId: number): Promise<string> => {
    return del(`/exercise/vocabulary/${id}?userId=${userId}`)
}

export const toggleMastered = (id: number, userId: number): Promise<VocabularyVO> => {
    return put(`/exercise/vocabulary/${id}/toggle?userId=${userId}`)
}

/** 获取今日待复习单词列表 */
export const getReviewList = (userId: number): Promise<VocabularyVO[]> => {
    return get(`/exercise/vocabulary/review/${userId}`)
}

/** 提交单词复习结果（记住/没记住） */
export const reviewWord = (id: number, data: ReviewResultRequest): Promise<VocabularyVO> => {
    return put(`/exercise/vocabulary/${id}/review`, data)
}

// ========== 每日单词 ==========

export interface DailyWordVO {
    id: number
    word: string
    phonetic: string
    translation: string
    pos: string
    example: string
    exampleTranslation: string
    level: string
    date: string
}

export const getDailyWord = (level: string = 'CET4'): Promise<DailyWordVO> => {
    return get('/exercise/daily-word', { level })
}

// ========== 学习历史 ==========

export interface ExerciseHistoryVO {
    logId: number
    questionId: number
    stem: string
    userAnswer: string
    correctAnswer: string
    isCorrect: number
    durationSeconds: number
    moduleType: number
    questionType: string
    difficultyLevel: number
    createdAt: string
    analysis?: string
}

export interface HistoryPage {
    records: ExerciseHistoryVO[]
    total: number
    page: number
    size: number
}

export const getExerciseHistory = (
    userId: number,
    page: number = 1,
    size: number = 20,
    moduleType?: number,
    isCorrect?: number,
    startTime?: string
): Promise<HistoryPage> => {
    const params: Record<string, any> = { page, size }
    if (moduleType !== undefined && moduleType !== 0) params.moduleType = moduleType
    if (isCorrect !== undefined && isCorrect >= 0) params.isCorrect = isCorrect
    if (startTime) params.startTime = startTime
    return get(`/exercise/history/${userId}`, params)
}

// ========== AI 题目审核管理 ==========

/**
 * 待审核题目实体
 */
export interface PendingQuestion {
    /** 主键ID */
    id: number
    /** 模块类型: 1=词汇, 2=语法 */
    moduleType: number
    /** 目标等级: CET4/CET6 */
    targetLevel: string
    /** 难度等级: 1~8 */
    difficultyLevel: number
    /** 题型: FILL_BLANK/SENTENCE_MAKING */
    questionType: string
    /** 题干 */
    stem: string
    /** 选项(JSON数组字符串) */
    options: string
    /** 正确答案 */
    correctAnswer: string
    /** 题目解析 */
    analysis: string
    /** 关联知识点名称 */
    knowledgePointName: string
    /** 审核状态: PENDING/APPROVED/REJECTED */
    status: string
    /** 审核人ID */
    reviewerId?: number
    /** 审核意见 */
    reviewComment?: string
    /** 创建时间 */
    createdAt: string
    /** 更新时间 */
    updatedAt?: string
}

/**
 * AI 批量生成题目
 * POST /exercise/generate-questions
 * @returns 成功保存到待审核表的题目数量
 */
export const generateQuestions = (data: {
    knowledgePoint: string
    cetLevel: string
    difficulty: number
    questionType: string
    count: number
}): Promise<number> => {
    return post('/exercise/generate-questions', data)
}

/**
 * 查询待审核题目列表
 * GET /exercise/pending-questions?status=PENDING
 */
export const getPendingQuestions = (status: string): Promise<PendingQuestion[]> => {
    return get('/exercise/pending-questions', { status })
}

/**
 * 审核通过题目
 * POST /exercise/pending-questions/{id}/approve
 */
export const approvePendingQuestion = (id: number, reviewerId: number = 1): Promise<string> => {
    return post(`/exercise/pending-questions/${id}/approve`, { reviewerId })
}

/**
 * 驳回题目
 * POST /exercise/pending-questions/{id}/reject
 */
export const rejectPendingQuestion = (id: number, comment: string, reviewerId: number = 1): Promise<string> => {
    return post(`/exercise/pending-questions/${id}/reject`, { reviewerId, comment })
}

// ========== 错题驱动同类题生成 ==========

/**
 * 薄弱知识点
 */
export interface WeakKnowledgePoint {
    /** 知识点ID */
    knowledgePointId: number
    /** 知识点名称 */
    knowledgePointName: string
    /** 该知识点错题数量 */
    errorCount: number
}

/**
 * 获取用户薄弱知识点列表
 * GET /exercise/weak-points/{userId}
 * 统计用户所有未解决错题中每个知识点的出现次数，按次数降序返回前10个
 */
export const getWeakPoints = (userId: number): Promise<WeakKnowledgePoint[]> => {
    return get(`/exercise/weak-points/${userId}`)
}

/**
 * 生成同类强化练习题
 * POST /exercise/generate-similar
 * 请求体: { userId, questionId, count }
 * 优先取正式题库中同知识点题目，不足部分由 AI 生成
 */
export const generateSimilar = (data: {
    userId: number
    questionId: number
    count: number
}): Promise<QuestionVO[]> => {
    return post('/exercise/generate-similar', data)
}
