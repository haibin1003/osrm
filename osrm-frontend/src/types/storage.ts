export type BackendType = 'HARBOR' | 'NEXUS' | 'NAS';

// Backend returns: ONLINE, OFFLINE, ERROR, UNKNOWN
export type HealthStatus = 'ONLINE' | 'OFFLINE' | 'ERROR' | 'UNKNOWN';

export interface HarborConfig {
  protocol?: 'HTTP' | 'HTTPS';
  project?: string;
  apiVersion?: string;
}

export interface NexusConfig {
  protocol?: 'HTTP' | 'HTTPS';
  mavenRepo?: string;
  npmRepo?: string;
  pypiRepo?: string;
  rawRepo?: string;
}

export type StorageConfig = HarborConfig | NexusConfig | Record<string, unknown>;

export interface StorageBackend {
  id: number;
  backendCode: string;
  backendName: string;
  backendType: BackendType;
  endpoint: string;
  namespace?: string;
  accessKey?: string;
  secretKey?: string;
  config?: StorageConfig;
  isDefault: boolean;
  enabled: boolean;
  healthStatus: HealthStatus;
  lastHealthCheck?: string;
  errorMessage?: string;
  description?: string;
  createdBy?: number;
  createdAt: string;
  updatedAt: string;
}

export interface StorageBackendFormData {
  backendName: string;
  backendType: BackendType;
  endpoint: string;
  namespace?: string;
  accessKey?: string;
  secretKey?: string;
  config?: StorageConfig;
  isDefault: boolean;
  description?: string;
}

export interface BackendQueryParams {
  keyword?: string;
  type?: BackendType;
  status?: HealthStatus;
  page?: number;
  size?: number;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface StorageType {
  code: string;
  name: string;
  description: string;
}

export interface ConnectionTestResult {
  success: boolean;
  message: string;
  serverInfo?: Record<string, unknown>;
  responseTimeMs?: number;
}
