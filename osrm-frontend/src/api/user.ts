import request from './request'

export interface User {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  roles: string[]
  enabled: boolean
  lastLoginTime: string
  createTime: string
}

export interface UserListParams {
  page?: number
  size?: number
  username?: string
  realName?: string
  enabled?: boolean
}

export interface CreateUserData {
  username: string
  realName: string
  email: string
  phone: string
  password: string
  roleIds: number[]
  enabled: boolean
}

export interface UpdateUserData {
  realName: string
  email: string
  phone: string
  roleIds: number[]
  enabled: boolean
}

export const userApi = {
  // 获取用户列表
  getList(params: UserListParams) {
    return request.get('/v1/users', { params })
  },

  // 获取用户详情
  getById(id: number) {
    return request.get(`/v1/users/${id}`)
  },

  // 创建用户
  create(data: CreateUserData) {
    return request.post('/v1/users', data)
  },

  // 更新用户
  update(id: number, data: UpdateUserData) {
    return request.put(`/v1/users/${id}`, data)
  },

  // 删除用户
  delete(id: number) {
    return request.delete(`/v1/users/${id}`)
  },

  // 重置密码
  resetPassword(id: number, newPassword: string) {
    return request.put(`/v1/users/${id}/password`, { newPassword })
  },

  // 切换状态
  toggleStatus(id: number, enabled: boolean) {
    return request.put(`/v1/users/${id}/status`, { enabled })
  }
}
