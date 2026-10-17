import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  traceId?: string
}

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        // 登录、注册接口的 401 是业务错误（如密码错误），不跳转，让调用方显示错误提示
        const url = response.config.url || ''
        const isAuthRequest = url.includes('/user/login') || url.includes('/user/register')
        if (!isAuthRequest) {
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('user')
          window.location.href = '/login'
          return Promise.reject(new Error(res.message || '请求失败'))
        }
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常'
    return Promise.reject(new Error(message))
  }
)

export const get = <T>(url: string, params?: Record<string, any>): Promise<T> => {
  return instance.get(url, { params })
}

export const post = <T>(url: string, data?: Record<string, any>): Promise<T> => {
  return instance.post(url, data)
}

export const put = <T>(url: string, data?: Record<string, any>): Promise<T> => {
  return instance.put(url, data)
}

export const del = <T>(url: string, params?: Record<string, any>): Promise<T> => {
  return instance.delete(url, { params })
}

export default instance
