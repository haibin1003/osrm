import request from './request';
import type { SoftwarePackage, SoftwareVersion, PackageForm, VersionForm, SoftwareTypeOption } from '@/types/software';
import type { PageResult } from '@/types/storage';

export const softwareApi = {
  list(params: { keyword?: string; type?: string; status?: string; page?: number; size?: number }) {
    return request.get<PageResult<SoftwarePackage>>('/v1/software-packages', { params });
  },
  getById(id: number) {
    return request.get<SoftwarePackage>(`/v1/software-packages/${id}`);
  },
  create(data: PackageForm) {
    return request.post<SoftwarePackage>('/v1/software-packages', data);
  },
  update(id: number, data: { packageName?: string; description?: string }) {
    return request.put<SoftwarePackage>(`/v1/software-packages/${id}`, data);
  },
  submit(id: number) {
    return request.post<SoftwarePackage>(`/v1/software-packages/${id}/submit`);
  },
  approve(id: number) {
    return request.post<SoftwarePackage>(`/v1/software-packages/${id}/approve`);
  },
  reject(id: number) {
    return request.post<SoftwarePackage>(`/v1/software-packages/${id}/reject`);
  },
  offline(id: number, reason?: string) {
    return request.post<SoftwarePackage>(`/v1/software-packages/${id}/offline`, null, { params: { reason } });
  },
  republish(id: number) {
    return request.post<SoftwarePackage>(`/v1/software-packages/${id}/republish`);
  },
  delete(id: number) {
    return request.delete(`/v1/software-packages/${id}`);
  },
  getSoftwareTypes() {
    return request.get<SoftwareTypeOption[]>('/v1/software-packages/types');
  },
  getVersions(packageId: number) {
    return request.get<SoftwareVersion[]>(`/v1/software-packages/${packageId}/versions`);
  },
  createVersion(packageId: number, data: VersionForm) {
    return request.post<SoftwareVersion>(`/v1/software-packages/${packageId}/versions`, data);
  },
  publishVersion(packageId: number, versionId: number) {
    return request.post<SoftwareVersion>(`/v1/software-packages/${packageId}/versions/${versionId}/publish`);
  },
  offlineVersion(packageId: number, versionId: number) {
    return request.post<SoftwareVersion>(`/v1/software-packages/${packageId}/versions/${versionId}/offline`);
  },
  deleteVersion(packageId: number, versionId: number) {
    return request.delete(`/v1/software-packages/${packageId}/versions/${versionId}`);
  }
};
