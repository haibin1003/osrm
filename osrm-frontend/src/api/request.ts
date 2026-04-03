import axios from 'axios'
import type { AxiosError, AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Safe message display
const showMessage = (message: string, type: 'error' | 'success' = 'error') => {
  // Delay to ensure DOM is ready
  setTimeout(() => {
    if (type === 'error') {
      ElMessage.error(message)
    } else {
      ElMessage.success(message)
    }
  }, 0)
}

// Request interceptor
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// Response interceptor
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data

    if (code !== 200) {
      // 403 无权限错误不在此处提示，由调用方处理
      if (code === 403) {
        return Promise.reject(new Error(message || '无权访问'))
      }
      showMessage(message || '请求失败')
      return Promise.reject(new Error(message))
    }

    return data
  },
  (error: AxiosError) => {
    const { response } = error

    if (response?.status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      showMessage('登录已过期，请重新登录')
      window.location.href = '/login'
    } else if (response?.status === 403) {
      // 403 错误不在此处提示，由调用方处理
      return Promise.reject(error)
    } else {
      showMessage(response?.data?.message || '网络错误')
    }

    return Promise.reject(error)
  }
)

export default request
