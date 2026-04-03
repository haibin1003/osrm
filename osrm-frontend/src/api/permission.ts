import request from './request'

export interface Permission {
  id: number
  parentId: number | null
  permissionCode: string
  permissionName: string
  resourceType: string
  action: string
  path: string
  icon: string
  sortOrder: number
  children?: Permission[]
}

export interface CreatePermissionData {
  parentId?: number
  permissionName: string
  permissionCode: string
  resourceType: string
  action?: string
  path?: string
  icon?: string
  sortOrder?: number
  description?: string
}

export const permissionApi = {
  // 获取权限树
  getTree() {
    return request.get('/v1/permissions/tree')
  },

  // 获取权限列表
  getList(params?: { permissionName?: string; permissionCode?: string; resourceType?: string }) {
    return request.get('/v1/permissions', { params })
  },

  // 创建权限
  create(data: CreatePermissionData) {
    return request.post('/v1/permissions', data)
  },

  // 删除权限
  delete(id: number) {
    return request.delete(`/v1/permissions/${id}`)
  }
}
