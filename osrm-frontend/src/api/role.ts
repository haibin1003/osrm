import request from './request'

export interface Role {
  id: number
  roleCode: string
  roleName: string
  description: string
  permissionCount: number
  createTime: string
}

export interface RoleListParams {
  page?: number
  size?: number
  roleName?: string
  roleCode?: string
}

export interface CreateRoleData {
  roleCode: string
  roleName: string
  description: string
}

export interface UpdateRoleData {
  roleName: string
  description: string
}

export const roleApi = {
  // 获取角色列表
  getList(params: RoleListParams) {
    return request.get('/v1/roles', { params })
  },

  // 获取角色详情
  getById(id: number) {
    return request.get(`/v1/roles/${id}`)
  },

  // 创建角色
  create(data: CreateRoleData) {
    return request.post('/v1/roles', data)
  },

  // 更新角色
  update(id: number, data: UpdateRoleData) {
    return request.put(`/v1/roles/${id}`, data)
  },

  // 删除角色
  delete(id: number) {
    return request.delete(`/v1/roles/${id}`)
  },

  // 获取角色权限
  getPermissions(id: number) {
    return request.get(`/v1/roles/${id}/permissions`)
  },

  // 配置角色权限
  configurePermissions(id: number, permissionIds: number[]) {
    return request.put(`/v1/roles/${id}/permissions`, { permissionIds })
  }
}
