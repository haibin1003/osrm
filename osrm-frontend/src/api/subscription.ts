import request from './request';
import type { SoftwarePackage } from '@/types/software';

export interface Subscription {
  id: number;
  packageId: number;
  packageName: string;
  versionId: number;
  versionNumber: string;
  businessSystemId: number;
  systemName: string;
  useScene: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED';
  statusName: string;
  applicantId: number;
  createdAt: string;
}

export interface SubscriptionForm {
  packageId: number;
  versionId: number;
  businessSystemId: number;
  useScene: string;
}

export interface DownloadTokenDTO {
  id: number;
  subscriptionId: number;
  token: string;
  expireAt: string;
  maxDownloads: number;
  usedCount: number;
  enabled: boolean;
}

export const subscriptionApi = {
  mySubscriptions(page = 1, size = 10) {
    return request.get<{ content: Subscription[]; totalElements: number }>('/v1/subscriptions/my', { params: { page, size } });
  },
  apply(data: SubscriptionForm) {
    return request.post<Subscription>('/v1/subscriptions', data);
  },
  pending(page = 1, size = 10) {
    return request.get<{ content: Subscription[]; totalElements: number }>('/v1/subscriptions/pending', { params: { page, size } });
  },
  approve(id: number) {
    return request.post(`/v1/subscriptions/${id}/approve`);
  },
  reject(id: number) {
    return request.post(`/v1/subscriptions/${id}/reject`);
  },
  getToken(id: number) {
    return request.get<DownloadTokenDTO>(`/v1/subscriptions/${id}/token`);
  }
};
