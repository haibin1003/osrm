import request from './request';

// 图谱节点
export interface GraphNode {
  id: string;
  type: 'system' | 'package';
  name: string;
}

// 业务系统节点
export interface SystemNode extends GraphNode {
  type: 'system';
  systemId: number;
  systemCode: string;
  domain: string;
  enabled: boolean;
}

// 软件包节点
export interface PackageNode extends GraphNode {
  type: 'package';
  packageId: number;
  packageKey: string;
  softwareType: string;
  status: string;
}

// 图谱边（订阅关系）
export interface GraphEdge {
  id: string;
  source: string;  // node id
  target: string;  // node id
  versionNumber?: string;
  status?: string;
}

// 图谱元数据
export interface GraphMetadata {
  totalSystems: number;
  totalPackages: number;
  totalSubscriptions: number;
}

// 完整关联图
export interface RelationshipGraph {
  nodes: (SystemNode | PackageNode)[];
  edges: GraphEdge[];
  metadata: GraphMetadata;
}

// 系统依赖详情
export interface SystemDependencies {
  system: SystemNode;
  packages: PackageDependencyInfo[];
  statistics: {
    totalPackages: number;
    byType: Record<string, number>;
  };
}

export interface PackageDependencyInfo {
  packageId: number;
  packageName: string;
  packageKey: string;
  softwareType: string;
  versionNumber?: string;
  status?: string;
}

// 软件影响分析
export interface PackageImpact {
  packageInfo: PackageNode;
  affectedSystems: AffectedSystemInfo[];
  statistics: {
    totalSystems: number;
    byDomain: Record<string, number>;
    byVersion: Record<string, number>;
  };
}

export interface AffectedSystemInfo {
  systemId: number;
  systemName: string;
  systemCode: string;
  domain?: string;
  versionNumber?: string;
}

// 过滤器参数
export interface GraphFilters {
  domain?: string;
  softwareType?: string;
  status?: string;
}

export const trackingApi = {
  // 获取完整关联图
  getRelationshipGraph(filters?: GraphFilters) {
    return request.get<RelationshipGraph>('/v1/tracking/relationship-graph', {
      params: filters
    });
  },

  // 获取系统依赖详情
  getSystemDependencies(systemId: number) {
    return request.get<SystemDependencies>(`/v1/tracking/system/${systemId}/dependencies`);
  },

  // 获取软件影响分析
  getPackageImpact(packageId: number) {
    return request.get<PackageImpact>(`/v1/tracking/package/${packageId}/impact`);
  }
};
