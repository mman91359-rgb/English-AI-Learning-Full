import { post, get } from './request'
import instance from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  nickname: string
  avatarUrl: string
  phone: string
  cetLevel: string
  roles: string
  eloScore: number
  eloLevel: string
}

export interface RegisterRequest {
  username: string
  password: string
  phone?: string
  cetLevel?: string
}

export interface AssessSubmitRequest {
  userId: number
  difficultyLevel: number
  moduleType: number
  isCorrect: boolean
}

export const login = (data: LoginRequest): Promise<LoginResponse> => {
  return post('/user/login', data)
}

export const register = (data: RegisterRequest): Promise<number> => {
  return post('/user/register', data)
}

export const getUserInfo = (userId: number): Promise<any> => {
  return get(`/user/info/${userId}`)
}

export const getUserByUsername = (username: string): Promise<any> => {
  return get(`/user/by-username/${username}`)
}

export const getRadarData = (userId: number): Promise<any> => {
  return get(`/user/radar/${userId}`)
}

export const submitAssessAnswer = (data: AssessSubmitRequest): Promise<any> => {
  return post('/user/assess/submit', data)
}

/**
 * 上传用户头像
 * @param userId 用户ID
 * @param file 头像文件
 * @returns 返回头像URL
 */
export const uploadAvatar = (userId: number, file: File): Promise<string> => {
  const formData = new FormData()
  formData.append('userId', String(userId))
  formData.append('file', file)
  return instance.post('/user/avatar/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then((res: any) => res)
}

/**
 * 修改用户昵称
 * @param userId 用户ID
 * @param nickname 新昵称
 * @returns 返回更新后的昵称
 */
export const updateNickname = (userId: number, nickname: string): Promise<string> => {
  return post('/user/nickname/update', { userId, nickname })
}

/**
 * 修改用户密码
 * @param userId 用户ID
 * @param oldPassword 原密码
 * @param newPassword 新密码
 */
export const updatePassword = (userId: number, oldPassword: string, newPassword: string): Promise<boolean> => {
  return post('/user/password/update', { userId, oldPassword, newPassword })
}

/**
 * 绑定/更新用户手机号
 * @param userId 用户ID
 * @param phone 手机号
 */
export const updatePhone = (userId: number, phone: string): Promise<string> => {
  return post('/user/phone/update', { userId, phone })
}
