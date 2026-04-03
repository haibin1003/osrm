import request from './request';

// 统计概览响应
export interface StatisticsOverviewDTO {
  totalPackages: number;
  totalSubscriptions: number;
  activeBusinessSystems: number;
  newSubscriptionsThisMonth: number;
  trends: {
    totalPackagesChange: number;
    totalSubscriptionsChange: number;
    activeBusinessSystemsChange: number;
    newSubscriptionsThisMonthChange: number;
  };
}

// 趋势数据
export interface TrendDataDTO {
  period: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  data: DailyData[];
  summary: TrendSummary;
}

export interface DailyData {
  date: string;
  subscriptionCount: number;
  approvedCount: number;
  rejectedCount: number;
  pendingCount: number;
}

export interface TrendSummary {
  totalSubscriptionCount: number;
  totalApprovedCount: number;
  totalRejectedCount: number;
  averageDaily: number;
}

// 业务系统分布
export interface BusinessDistributionDTO {
  totalBusinessSystems: number;
  totalPackages: number;
  totalSubscriptions: number;
  data: BusinessSystemStats[];
}

export interface BusinessSystemStats {
  systemId: number;
  systemName: string;
  systemCode: string;
  domain: string;
  packageCount: number;
  subscriptionCount: number;
  percentage: number;
}

// 软件包热度排行
export interface PopularityRankingDTO {
  sortBy: string;
  total: number;
  data: PackagePopularity[];
}

export interface PackagePopularity {
  rank: number;
  packageId: number;
  packageName: string;
  packageKey: string;
  softwareType: string;
  subscriptionCount: number;
  businessSystemCount: number;
  trend: 'up' | 'down' | 'stable';
  change: number;
}

// 软件类型分布
export interface TypeDistributionDTO {
  totalPackages: number;
  data: TypeStats[];
}

export interface TypeStats {
  type: string;
  typeName: string;
  packageCount: number;
  subscriptionCount: number;
  percentage: number;
  color: string;
}

export const statisticsApi = {
  // 获取统计概览
  getOverview() {
    return request.get<StatisticsOverviewDTO>('/v1/statistics/overview');
  },

  // 获取趋势数据
  getTrend(days: number = 7) {
    return request.get<TrendDataDTO>('/v1/statistics/trend', { params: { days } });
  },

  // 获取业务系统分布
  getBusinessDistribution(limit: number = 10) {
    return request.get<BusinessDistributionDTO>('/v1/statistics/business-distribution', { params: { limit } });
  },

  // 获取软件包热度排行
  getPopularity(limit: number = 10, sortBy: string = 'subscription_count') {
    return request.get<PopularityRankingDTO>('/v1/statistics/popularity', { params: { limit, sortBy } });
  },

  // 获取软件类型分布
  getTypeDistribution() {
    return request.get<TypeDistributionDTO>('/v1/statistics/type-distribution');
  }
};
