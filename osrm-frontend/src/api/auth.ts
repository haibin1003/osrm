import request from './request'
import type { LoginRequest, LoginResponse, UserInfo } from '@/types/auth'

export const authApi = {
  login(data: LoginRequest) {
    return request.post<LoginResponse>('/v1/auth/login', data)
  },

  logout() {
    return request.post('/v1/auth/logout')
  },

  refreshToken(refreshToken: string) {
    return request.post<LoginResponse>('/v1/auth/refresh', { refreshToken })
  },

  getCurrentUser() {
    return request.get<UserInfo>('/v1/auth/me')
  }
}
