import { get, post } from './request'

/**
 * 复习任务视图
 */
export interface ReviewTaskVO {
    /** 记忆曲线记录ID */
    curveId: number
    /** 题目ID */
    questionId: number
    /** 模块类型 */
    moduleType: number
    /** 掌握等级: 1=陌生, 2=模糊, 3=熟悉, 4=熟练, 5=精通 */
    masteryLevel: number
    /** 下次复习时间 */
    nextReviewTime: string
    /** 复习次数 */
    reviewCount: number
    /** 题干 */
    stem?: string
    /** 正确答案 */
    correctAnswer?: string
    /** 题目解析 */
    analysis?: string
}

/**
 * 更新遗忘曲线请求
 */
export interface UpdateCurveRequest {
    userId: number
    questionId: number
    isCorrect: boolean
    moduleType?: number
}

/**
 * 遗忘曲线统计
 */
export interface CurveStats {
    totalQuestions: number
    todayReviewCount: number
    averageMastery: number
    masteryDistribution: Record<string, number>
}

/**
 * 获取每日复习任务
 * GET /learning/review-tasks/{userId}
 */
export const getReviewTasks = (userId: number): Promise<ReviewTaskVO[]> => {
    return get(`/learning/review-tasks/${userId}`)
}

/**
 * 更新遗忘曲线
 * POST /learning/update-curve
 */
export const updateCurve = (data: UpdateCurveRequest): Promise<string> => {
    return post('/learning/update-curve', data)
}

/**
 * 遗忘曲线统计
 * GET /learning/curve-stats/{userId}
 */
export const getCurveStats = (userId: number): Promise<CurveStats> => {
    return get(`/learning/curve-stats/${userId}`)
}
