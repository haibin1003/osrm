import request from './request'

export interface Profile {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  bio: string
  avatar: string
  roles: string[]
  lastLoginTime: string
  createTime: string
}

export interface UpdateProfileData {
  realName: string
  email: string
  phone: string
  bio: string
}

export interface ChangePasswordData {
  currentPassword: string
  newPassword: string
}

export const profileApi = {
  // 获取个人信息
  getProfile() {
    return request.get('/v1/profile')
  },

  // 更新个人信息
  updateProfile(data: UpdateProfileData) {
    return request.put('/v1/profile', data)
  },

  // 修改密码
  changePassword(data: ChangePasswordData) {
    return request.put('/v1/profile/password', data)
  }
}
