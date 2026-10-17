import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, type LoginRequest, type RegisterRequest, type LoginResponse } from '@/api/user'
import { getExerciseStats } from '@/api/exercise'

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split('.')[1]
    // JWT 使用 base64url 编码，需要转换 + - / _ 为标准 base64
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(base64))
  } catch {
    return null
  }
}

function isTokenValid(token: string | null): boolean {
  if (!token) return false
  const payload = decodeJwtPayload(token)
  if (!payload || !payload.exp) return false
  return (payload.exp as number) * 1000 > Date.now()
}

export const useUserStore = defineStore('user', () => {
  // 使用 sessionStorage：刷新页面不丢失，关闭浏览器自动清除（无七天自动登录）
  const storedToken = sessionStorage.getItem('token')
  const validToken = isTokenValid(storedToken) ? storedToken : null
  const storedUser = validToken ? sessionStorage.getItem('user') : null
  if (storedToken && !validToken) {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
  }
  const token = ref<string | null>(validToken)
  let parsedUser: LoginResponse | null = null
if (storedUser) {
  try {
    parsedUser = JSON.parse(storedUser)
  } catch {
    sessionStorage.removeItem('user')
  }
}
const user = ref<LoginResponse | null>(parsedUser)
  const isLoading = ref(false)

  // 连续学习天数（从后端接口获取的真实数据）
  const streakDays = ref(0)
  const activeDays = ref(0)
  const masteredWords = ref(0)

  const isLoggedIn = computed(() => isTokenValid(token.value) && !!user.value)
  const userId = computed(() => user.value?.userId)
  const username = computed(() => user.value?.username || '')
  const avatarUrl = computed(() => user.value?.avatarUrl || '')
  const phone = computed(() => user.value?.phone || '')

  /** 从后端获取用户学习统计数据 */
  const loadUserStats = async (): Promise<void> => {
    if (!userId.value) return
    try {
      const stats = await getExerciseStats(userId.value)
      if (stats) {
        streakDays.value = stats.streakDays || 0
        activeDays.value = stats.activeDays || 0
        masteredWords.value = stats.masteredWords || 0
      }
    } catch (e) {
      console.warn('获取用户统计数据失败', e)
    }
  }

  const login = async (data: LoginRequest): Promise<void> => {
    isLoading.value = true
    try {
      const response = await loginApi(data)
      token.value = response.token
      user.value = response
      sessionStorage.setItem('token', response.token)
      sessionStorage.setItem('user', JSON.stringify(response))
      // 登录成功后加载真实统计数据
      await loadUserStats()
    } finally {
      isLoading.value = false
    }
  }

  const register = async (data: RegisterRequest): Promise<number> => {
    isLoading.value = true
    try {
      return await registerApi(data)
    } finally {
      isLoading.value = false
    }
  }

  const logout = (): void => {
    token.value = null
    user.value = null
    streakDays.value = 0
    activeDays.value = 0
    masteredWords.value = 0
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
  }

  const setUser = (newUser: LoginResponse): void => {
    user.value = newUser
    sessionStorage.setItem('user', JSON.stringify(newUser))
  }

  /** 更新头像URL（上传成功后调用） */
  const updateAvatar = (newAvatarUrl: string): void => {
    if (user.value) {
      user.value = { ...user.value, avatarUrl: newAvatarUrl }
      sessionStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  /** 更新昵称（修改成功后调用） */
  const updateNickname = (newNickname: string): void => {
    if (user.value) {
      user.value = { ...user.value, nickname: newNickname }
      sessionStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  /** 更新手机号（绑定成功后调用） */
  const updatePhone = (newPhone: string): void => {
    if (user.value) {
      user.value = { ...user.value, phone: newPhone }
      sessionStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  return {
    token,
    user,
    isLoading,
    isLoggedIn,
    userId,
    username,
    avatarUrl,
    phone,
    streakDays,
    activeDays,
    masteredWords,
    login,
    register,
    logout,
    setUser,
    updateAvatar,
    updateNickname,
    updatePhone,
    loadUserStats,
  }
})


