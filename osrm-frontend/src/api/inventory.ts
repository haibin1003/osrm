import request from './request';
import type { PageResult } from '@/types/storage';

export interface InventoryRecord {
  id: number;
  recordNo: string;
  userId: number;
  userName?: string;
  packageId?: number;
  packageName: string;
  versionNo?: string;
  softwareType?: string;
  responsiblePerson: string;
  businessSystemId?: number;
  businessSystemName?: string;
  deployEnvironment?: string;
  serverCount: number;
  usageScenario?: string;
  sourceType: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  statusName: string;
  approvedBy?: number;
  approvedByName?: string;
  approvedAt?: string;
  rejectReason?: string;
  remarks?: string;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryForm {
  packageId?: number;
  packageName: string;
  versionNo?: string;
  softwareType?: string;
  responsiblePerson?: string;
  businessSystemId?: number;
  deployEnvironment?: string;
  serverCount: number;
  usageScenario?: string;
  remarks?: string;
}

export interface InventorySettings {
  enableInventoryFeature: boolean;
}

export const inventoryApi = {
  // 获取存量功能设置
  getSettings() {
    return request.get<InventorySettings>('/v1/inventory/settings');
  },

  // 我的存量列表
  getMyList(params: { status?: string; page?: number; size?: number }) {
    return request.get<PageResult<InventoryRecord>>('/v1/inventory/my', { params });
  },

  // 待审批列表（管理员）
  getPendingList(params: { page?: number; size?: number }) {
    return request.get<PageResult<InventoryRecord>>('/v1/inventory/pending', { params });
  },

  // 所有存量列表（管理员）
  getAllList(params: {
    userId?: number;
    status?: string;
    packageName?: string;
    businessSystemId?: number;
    page?: number;
    size?: number;
  }) {
    return request.get<PageResult<InventoryRecord>>('/v1/inventory/all', { params });
  },

  // 获取详情
  getById(id: number) {
    return request.get<InventoryRecord>(`/v1/inventory/${id}`);
  },

  // 创建登记
  create(data: InventoryForm) {
    return request.post<InventoryRecord>('/v1/inventory', data);
  },

  // 更新登记
  update(id: number, data: InventoryForm) {
    return request.put<InventoryRecord>(`/v1/inventory/${id}`, data);
  },

  // 批准
  approve(id: number) {
    return request.post<InventoryRecord>(`/v1/inventory/${id}/approve`);
  },

  // 驳回
  reject(id: number, reason: string) {
    return request.post<InventoryRecord>(`/v1/inventory/${id}/reject`, { reason });
  },

  // 删除
  delete(id: number) {
    return request.delete(`/v1/inventory/${id}`);
  }
};
