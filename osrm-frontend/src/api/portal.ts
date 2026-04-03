import request from './request';
import type { SoftwarePackage, SoftwareVersion } from '@/types/software';

export interface PortalVersion {
  id: number;
  versionNo: string;
  releaseNotes: string;
  status: string;
  isLatest: boolean;
  createdAt: string;
}

export interface PortalStats {
  totalPackages: number;
  publishedCount: number;
  pendingCount: number;
  dockerCount: number;
  helmCount: number;
  mavenCount: number;
  npmCount: number;
  pypiCount: number;
  genericCount: number;
}

export interface StatsTrendItem {
  date: string;
  count: number;
}

export interface DependencyNode {
  id: string;
  name: string;
  type: string;
}

export interface DependencyLink {
  source: string;
  target: string;
  version: string;
}

export interface DependencyGraph {
  nodes: DependencyNode[];
  links: DependencyLink[];
}

export interface SecurityReport {
  score: number;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  scanTime: string;
  status: 'SAFE' | 'WARNING' | 'DANGER';
}

export const portalApi = {
  listSoftware(params: { keyword?: string; type?: string; page?: number; size?: number }) {
    return request.get<{ content: SoftwarePackage[]; totalElements: number }>('/v1/portal/software', { params });
  },
  getSoftwareDetail(id: number) {
    return request.get<SoftwarePackage & { versions: SoftwareVersion[] }>(`/v1/portal/software/${id}`);
  },
  getStats() {
    return request.get<PortalStats>('/v1/portal/stats');
  },
  listPublished(params: { keyword?: string; type?: string; page?: number; size?: number }) {
    return request.get<{ content: any[]; totalElements: number; totalPages: number; size: number; page: number }>('/v1/portal/software', { params });
  },
  getPopular() {
    return request.get<SoftwarePackage[]>('/v1/portal/popular');
  },
  getStatsOverview() {
    return request.get<{ totalPackages: number; publishedCount: number; pendingCount: number; draftCount: number; totalSubscriptions: number }>('/v1/portal/stats/overview');
  },
  getStatsTrend(days: number = 7) {
    return request.get<StatsTrendItem[]>('/v1/portal/stats/trend', { params: { days } });
  },
  getDependencies(id: number) {
    return request.get<DependencyGraph>(`/v1/portal/software/${id}/dependencies`);
  },
  getSecurityReport(id: number) {
    return request.get<SecurityReport>(`/v1/portal/software/${id}/security`);
  },
  getVersions(id: number) {
    return request.get<PortalVersion[]>(`/v1/portal/software/${id}/versions`);
  }
};