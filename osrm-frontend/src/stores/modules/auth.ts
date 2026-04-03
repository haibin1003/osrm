import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, TokenPair } from '@/types/auth'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const userInfo = ref<UserInfo | null>(null)

  // Getters
  const isAuthenticated = computed(() => !!accessToken.value)

  const hasPermission = (permission: string) => {
    return userInfo.value?.permissions?.includes(permission) ?? false
  }

  const hasRole = (role: string) => {
    return userInfo.value?.roles?.includes(role) ?? false
  }

  // Actions
  const login = async (credentials: { username: string; password: string; rememberMe?: boolean }) => {
    const data = await authApi.login(credentials)

    setToken({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken
    })

    userInfo.value = data.user

    return data
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } finally {
      clearToken()
      userInfo.value = null
    }
  }

  const fetchCurrentUser = async () => {
    const data = await authApi.getCurrentUser()
    userInfo.value = data
    return data
  }

  const setToken = (tokenPair: TokenPair) => {
    accessToken.value = tokenPair.accessToken
    refreshToken.value = tokenPair.refreshToken
    localStorage.setItem('accessToken', tokenPair.accessToken)
    localStorage.setItem('refreshToken', tokenPair.refreshToken)
  }

  const clearToken = () => {
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isAuthenticated,
    hasPermission,
    hasRole,
    login,
    logout,
    fetchCurrentUser,
    setToken,
    clearToken
  }
}, {
  persist: {
    paths: ['accessToken', 'refreshToken', 'userInfo']
  }
})
