import { get } from './request'

/**
 * 能力雷达图数据
 */
export interface RadarData {
    userId: number
    eloScore: number
    eloLevel: string
    targetLevel: string
    vocabularyScore: number
    grammarScore: number
    listeningScore: number
    readingScore: number
    vocabularyPercent: number
    grammarPercent: number
    listeningPercent: number
    readingPercent: number
    eloFloor: number
    eloCeil: number
    eloProgress: number
}

/**
 * 用户完整信息（含能力雷达）
 */
export interface UserFullInfo {
    id: number
    username: string
    nickname: string
    avatarUrl: string
    cetLevel: string
    roles: string
    status: number
    lastLoginAt: string
    createdAt: string
}

/**
 * 获取能力雷达图数据
 * GET /user/radar/{userId}
 */
export const getRadarData = (userId: number): Promise<RadarData> => {
    return get(`/user/radar/${userId}`)
}

/**
 * 获取用户信息
 * GET /user/info/{userId}
 */
export const getUserInfo = (userId: number): Promise<UserFullInfo> => {
    return get(`/user/info/${userId}`)
}
