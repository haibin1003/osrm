export type SoftwareType = 'DOCKER_IMAGE' | 'HELM_CHART' | 'MAVEN' | 'NPM' | 'PYPI' | 'GENERIC';
export type PackageStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'OFFLINE' | 'ARCHIVED';
export type VersionStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE';

export interface SoftwarePackage {
  id: number;
  packageName: string;
  packageKey: string;
  softwareType: SoftwareType;
  softwareTypeName: string;
  description: string | null;
  websiteUrl: string | null;
  licenseType: string | null;
  currentVersion: string | null;
  viewCount: number;
  downloadCount: number;
  subscriptionCount: number;
  status: PackageStatus;
  statusName: string;
  categoryId: number | null;
  versionCount: number;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
}

export interface SoftwareVersion {
  id: number;
  packageId: number;
  versionNo: string;
  status: VersionStatus;
  statusName: string;
  storageBackendId: number;
  storageBackendName: string | null;
  storagePath: string | null;
  artifactUrl: string | null;
  releaseNotes: string | null;
  fileSize: number | null;
  checksum: string | null;
  isLatest: boolean;
  publishedBy: number | null;
  publishedAt: string | null;
  createdAt: string;
}

export interface PackageForm {
  packageName: string;
  packageKey: string;
  softwareType: SoftwareType;
  description?: string;
}

export interface VersionForm {
  versionNo: string;
  storageBackendId: number | null;
  releaseNotes?: string;
}

export interface SoftwareTypeOption {
  value: string;
  label: string;
  storageBackend: string;
}
