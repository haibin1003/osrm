import request from './request';
import type { StorageBackend, StorageBackendFormData, BackendQueryParams, PageResult, StorageType, ConnectionTestResult } from '@/types/storage';

export interface TestConnectionRequest {
  backendType: string;
  endpoint: string;
  accessKey: string;
  secretKey: string;
}

export const storageApi = {
  // Get storage backend list
  getList(params: BackendQueryParams) {
    return request.get<PageResult<StorageBackend>>('/v1/storage-backends', { params });
  },

  // Get storage backend detail
  getDetail(id: number) {
    return request.get<StorageBackend>(`/v1/storage-backends/${id}`);
  },

  // Create storage backend
  create(data: StorageBackendFormData) {
    return request.post<StorageBackend>('/v1/storage-backends', data);
  },

  // Update storage backend
  update(id: number, data: Partial<StorageBackendFormData>) {
    return request.put<StorageBackend>(`/v1/storage-backends/${id}`, data);
  },

  // Delete storage backend
  delete(id: number) {
    return request.delete(`/v1/storage-backends/${id}`);
  },

  // Test connection before create
  testConnection(data: TestConnectionRequest) {
    return request.post<ConnectionTestResult>('/v1/storage-backends/test-connection', data);
  },

  // Get storage types
  getTypes() {
    return request.get<StorageType[]>('/v1/storage-backends/types');
  },

  // Health check
  healthCheck(id: number) {
    return request.post<{ healthStatus: string; lastHealthCheck: string; errorMessage: string | null; responseTimeMs: number }>(`/v1/storage-backends/${id}/health`);
  },

  // Set as default
  setDefault(id: number) {
    return request.put<StorageBackend>(`/v1/storage-backends/${id}/default`);
  },

  // Enable/Disable
  setEnabled(id: number, enabled: boolean) {
    return request.put<StorageBackend>(`/v1/storage-backends/${id}/status?enabled=${enabled}`);
  }
};
