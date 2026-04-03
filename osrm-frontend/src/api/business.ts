import request from './request';
import type { BusinessSystem, BusinessSystemForm } from '@/types/business';
import type { PageResult } from '@/types/storage';

export const businessApi = {
  list(params: { keyword?: string; domain?: string; enabled?: boolean | null; page?: number; size?: number }) {
    return request.get<PageResult<BusinessSystem>>('/v1/business-systems', { params });
  },

  getById(id: number) {
    return request.get<BusinessSystem>(`/v1/business-systems/${id}`);
  },

  create(data: BusinessSystemForm) {
    return request.post<BusinessSystem>('/v1/business-systems', data);
  },

  update(id: number, data: BusinessSystemForm) {
    return request.put<BusinessSystem>(`/v1/business-systems/${id}`, data);
  },

  setEnabled(id: number, enabled: boolean) {
    return request.put<BusinessSystem>(`/v1/business-systems/${id}/status?enabled=${enabled}`);
  },

  delete(id: number) {
    return request.delete(`/v1/business-systems/${id}`);
  }
};
