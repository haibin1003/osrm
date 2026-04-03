<template>
  <div class="stats-page">
    <div class="page-header">
      <h1 class="page-title">使用统计看板</h1>
      <p class="page-subtitle">平台整体使用情况数据可视化分析</p>
    </div>

    <!-- 统计概览卡片 -->
    <div class="stats-grid">
      <div class="stat-card card-hover-lift">
        <div class="stat-icon" style="background: var(--color-primary-light); color: var(--color-primary);">
          <el-icon :size="28"><Box /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ overview.totalPackages }}</div>
          <div class="stat-label">已发布软件包</div>
          <div class="stat-trend" :class="getTrendClass(overview.trends?.totalPackagesChange)">
            <el-icon><component :is="getTrendIcon(overview.trends?.totalPackagesChange)" /></el-icon>
            <span>{{ formatTrend(overview.trends?.totalPackagesChange) }}</span>
          </div>
        </div>
      </div>

      <div class="stat-card card-hover-lift">
        <div class="stat-icon" style="background: var(--color-success-light); color: var(--color-success);">
          <el-icon :size="28"><ShoppingCart /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ overview.totalSubscriptions }}</div>
          <div class="stat-label">总订购数</div>
          <div class="stat-trend" :class="getTrendClass(overview.trends?.totalSubscriptionsChange)">
            <el-icon><component :is="getTrendIcon(overview.trends?.totalSubscriptionsChange)" /></el-icon>
            <span>{{ formatTrend(overview.trends?.totalSubscriptionsChange) }}</span>
          </div>
        </div>
      </div>

      <div class="stat-card card-hover-lift">
        <div class="stat-icon" style="background: var(--color-warning-light); color: var(--color-warning);">
          <el-icon :size="28"><OfficeBuilding /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ overview.activeBusinessSystems }}</div>
          <div class="stat-label">活跃业务系统</div>
          <div class="stat-trend" :class="getTrendClass(overview.trends?.activeBusinessSystemsChange)">
            <el-icon><component :is="getTrendIcon(overview.trends?.activeBusinessSystemsChange)" /></el-icon>
            <span>{{ formatTrend(overview.trends?.activeBusinessSystemsChange) }}</span>
          </div>
        </div>
      </div>

      <div class="stat-card card-hover-lift">
        <div class="stat-icon" style="background: var(--color-info-light); color: var(--color-info);">
          <el-icon :size="28"><Calendar /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ overview.newSubscriptionsThisMonth }}</div>
          <div class="stat-label">本月新增订购</div>
          <div class="stat-trend" :class="getTrendClass(overview.trends?.newSubscriptionsThisMonthChange)">
            <el-icon><component :is="getTrendIcon(overview.trends?.newSubscriptionsThisMonthChange)" /></el-icon>
            <span>{{ formatTrend(overview.trends?.newSubscriptionsThisMonthChange) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <!-- 订购趋势图 -->
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-header-title">
            <span>订购趋势</span>
            <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :label="7">近7天</el-radio-button>
              <el-radio-button :label="14">近14天</el-radio-button>
              <el-radio-button :label="30">近30天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="trend-summary" v-if="trend.summary">
          <div class="summary-item">
            <span class="summary-label">总订购</span>
            <span class="summary-value">{{ trend.summary.totalSubscriptionCount }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">已审批</span>
            <span class="summary-value success">{{ trend.summary.totalApprovedCount }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">已拒绝</span>
            <span class="summary-value danger">{{ trend.summary.totalRejectedCount }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">日均</span>
            <span class="summary-value">{{ trend.summary.averageDaily }}</span>
          </div>
        </div>
        <v-chart class="chart" :option="trendChartOption" autoresize />
      </el-card>

      <!-- 软件类型分布图 -->
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-header-title">软件类型分布</div>
        </template>
        <v-chart class="chart pie-chart" :option="typeChartOption" autoresize />
        <div class="type-legend">
          <div v-for="item in typeDistribution.data" :key="item.type" class="legend-item">
            <span class="legend-color" :style="{ background: item.color }"></span>
            <span class="legend-label">{{ item.typeName }}</span>
            <span class="legend-value">{{ item.packageCount }} ({{ item.percentage }}%)</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 下方区域 -->
    <div class="bottom-row">
      <!-- 软件包热度排行 -->
      <el-card shadow="never" class="ranking-card">
        <template #header>
          <div class="card-header-title">
            <span>软件包热度排行</span>
            <el-radio-group v-model="rankingSortBy" size="small" @change="loadPopularity">
              <el-radio-button label="subscription_count">按订购数</el-radio-button>
              <el-radio-button label="business_system_count">按业务系统数</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <el-table :data="popularity.data" stripe size="small">
          <el-table-column type="index" label="排名" width="60" align="center">
            <template #default="{ $index }">
              <div class="rank-cell" :class="{ 'top-3': $index < 3 }">
                {{ $index + 1 }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="packageName" label="软件包" min-width="180" show-overflow-tooltip />
          <el-table-column prop="softwareType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ formatType(row.softwareType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="subscriptionCount" label="订购数" width="90" align="center" sortable />
          <el-table-column prop="businessSystemCount" label="业务系统数" width="110" align="center" sortable />
          <el-table-column prop="trend" label="趋势" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.trend === 'up' ? 'success' : row.trend === 'down' ? 'danger' : 'info'" size="small">
                <el-icon>
                  <ArrowUp v-if="row.trend === 'up'" />
                  <ArrowDown v-else-if="row.trend === 'down'" />
                  <Minus v-else />
                </el-icon>
                {{ row.change > 0 ? '+' + row.change : row.change }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 业务系统分布 -->
      <el-card shadow="never" class="distribution-card">
        <template #header>
          <div class="card-header-title">
            <span>业务系统分布</span>
            <el-link type="primary" @click="$router.push('/business/systems')">查看全部</el-link>
          </div>
        </template>
        <div class="distribution-summary">
          <div class="summary-box">
            <div class="summary-num">{{ businessDistribution.totalBusinessSystems }}</div>
            <div class="summary-text">活跃业务系统</div>
          </div>
          <div class="summary-box">
            <div class="summary-num">{{ businessDistribution.totalSubscriptions }}</div>
            <div class="summary-text">总订购数</div>
          </div>
        </div>
        <div class="distribution-list">
          <div v-for="item in businessDistribution.data" :key="item.systemId" class="distribution-item">
            <div class="item-info">
              <div class="item-name">{{ item.systemName }}</div>
              <div class="item-code">{{ item.systemCode }}</div>
            </div>
            <div class="item-stats">
              <el-tag size="small" type="primary">{{ item.packageCount }} 软件包</el-tag>
              <el-tag size="small" type="success">{{ item.subscriptionCount }} 订购</el-tag>
            </div>
            <el-progress :percentage="item.percentage" :stroke-width="6" :show-text="false" />
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Box, ShoppingCart, OfficeBuilding, Calendar, ArrowUp, ArrowDown, Minus } from '@element-plus/icons-vue'
import { statisticsApi, type StatisticsOverviewDTO, type TrendDataDTO, type BusinessDistributionDTO, type PopularityRankingDTO, type TypeDistributionDTO } from '@/api/statistics'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'

// Register ECharts components
use([CanvasRenderer, BarChart, PieChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

// Data
const overview = reactive<Partial<StatisticsOverviewDTO>>({})
const trend = reactive<Partial<TrendDataDTO>>({ summary: undefined, data: [] })
const businessDistribution = reactive<Partial<BusinessDistributionDTO>>({ data: [] })
const popularity = reactive<Partial<PopularityRankingDTO>>({ data: [] })
const typeDistribution = reactive<Partial<TypeDistributionDTO>>({ data: [] })

// UI state
const trendDays = ref(7)
const rankingSortBy = ref('subscription_count')

// 趋势图表配置
const trendChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' }
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    top: '10%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: trend.data?.map(d => d.date.slice(5)) || [],
    axisLine: { lineStyle: { color: '#e0e0e0' } },
    axisLabel: { color: '#666' }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#f0f0f0' } }
  },
  series: [
    {
      name: '总订购',
      type: 'bar',
      data: trend.data?.map(d => d.subscriptionCount) || [],
      itemStyle: { color: '#6366f1', borderRadius: [4, 4, 0, 0] }
    },
    {
      name: '已审批',
      type: 'line',
      data: trend.data?.map(d => d.approvedCount) || [],
      smooth: true,
      itemStyle: { color: '#22c55e' },
      lineStyle: { width: 2 }
    },
    {
      name: '待审批',
      type: 'line',
      data: trend.data?.map(d => d.pendingCount) || [],
      smooth: true,
      itemStyle: { color: '#f59e0b' },
      lineStyle: { width: 2 }
    }
  ]
}))

// 类型分布图表配置
const typeChartOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)'
  },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        }
      },
      data: typeDistribution.data?.map(item => ({
        value: item.packageCount,
        name: item.typeName,
        itemStyle: { color: item.color }
      })) || []
    }
  ]
}))

// Helper functions
function getTrendClass(value: number): string {
  if (value > 0) return 'trend-up'
  if (value < 0) return 'trend-down'
  return 'trend-stable'
}

function getTrendIcon(value: number) {
  if (value > 0) return ArrowUp
  if (value < 0) return ArrowDown
  return Minus
}

function formatTrend(value: number): string {
  if (value === undefined || value === null) return '-'
  const prefix = value > 0 ? '+' : ''
  return `${prefix}${value}%`
}

function formatType(type: string): string {
  const typeMap: Record<string, string> = {
    'DOCKER_IMAGE': 'Docker',
    'HELM_CHART': 'Helm',
    'MAVEN': 'Maven',
    'NPM': 'NPM',
    'PYPI': 'PyPI',
    'GENERIC': '通用'
  }
  return typeMap[type] || type
}

// Load functions
async function loadOverview() {
  try {
    const data = await statisticsApi.getOverview()
    Object.assign(overview, data)
  } catch (e) {
    console.error('Failed to load overview:', e)
  }
}

async function loadTrend() {
  try {
    const data = await statisticsApi.getTrend(trendDays.value)
    Object.assign(trend, data)
  } catch (e) {
    console.error('Failed to load trend:', e)
  }
}

async function loadBusinessDistribution() {
  try {
    const data = await statisticsApi.getBusinessDistribution(10)
    Object.assign(businessDistribution, data)
  } catch (e) {
    console.error('Failed to load business distribution:', e)
  }
}

async function loadPopularity() {
  try {
    const data = await statisticsApi.getPopularity(10, rankingSortBy.value)
    Object.assign(popularity, data)
  } catch (e) {
    console.error('Failed to load popularity:', e)
  }
}

async function loadTypeDistribution() {
  try {
    const data = await statisticsApi.getTypeDistribution()
    Object.assign(typeDistribution, data)
  } catch (e) {
    console.error('Failed to load type distribution:', e)
  }
}

onMounted(() => {
  loadOverview()
  loadTrend()
  loadBusinessDistribution()
  loadPopularity()
  loadTypeDistribution()
})
</script>

<style scoped lang="scss">
.stats-page {
  .page-header {
    margin-bottom: var(--space-2xl);

    .page-title {
      font-size: var(--font-size-4xl);
      font-weight: var(--font-weight-bold);
      margin: 0;
    }

    .page-subtitle {
      font-size: var(--font-size-md);
      color: var(--color-text-secondary);
      margin: var(--space-xs) 0 0;
    }
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-lg);
    margin-bottom: var(--space-2xl);
  }

  .stat-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-xl);
    display: flex;
    align-items: flex-start;
    gap: var(--space-md);

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .stat-content {
      flex: 1;
    }

    .stat-value {
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
      line-height: 1.2;
    }

    .stat-label {
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
      margin-top: var(--space-xs);
    }

    .stat-trend {
      display: flex;
      align-items: center;
      gap: var(--space-xs);
      margin-top: var(--space-sm);
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-medium);

      &.trend-up {
        color: var(--color-success);
      }

      &.trend-down {
        color: var(--color-danger);
      }

      &.trend-stable {
        color: var(--color-text-secondary);
      }
    }
  }

  .charts-row {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: var(--space-lg);
    margin-bottom: var(--space-lg);
  }

  .chart-card {
    .card-header-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: var(--font-weight-medium);
    }

    .trend-summary {
      display: flex;
      gap: var(--space-lg);
      margin-bottom: var(--space-md);
      padding: var(--space-md);
      background: var(--color-bg-secondary);
      border-radius: var(--radius-md);

      .summary-item {
        display: flex;
        flex-direction: column;
        align-items: center;

        .summary-label {
          font-size: var(--font-size-xs);
          color: var(--color-text-secondary);
        }

        .summary-value {
          font-size: var(--font-size-lg);
          font-weight: var(--font-weight-bold);
          color: var(--color-text-primary);

          &.success {
            color: var(--color-success);
          }

          &.danger {
            color: var(--color-danger);
          }
        }
      }
    }

    .chart {
      height: 260px;
    }

    .pie-chart {
      height: 200px;
    }

    .type-legend {
      margin-top: var(--space-md);
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: var(--space-sm);

      .legend-item {
        display: flex;
        align-items: center;
        gap: var(--space-xs);
        font-size: var(--font-size-xs);

        .legend-color {
          width: 12px;
          height: 12px;
          border-radius: var(--radius-sm);
        }

        .legend-label {
          color: var(--color-text-secondary);
          flex: 1;
        }

        .legend-value {
          color: var(--color-text-primary);
          font-weight: var(--font-weight-medium);
        }
      }
    }
  }

  .bottom-row {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: var(--space-lg);
  }

  .ranking-card,
  .distribution-card {
    .card-header-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: var(--font-weight-medium);
    }

    .rank-cell {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: var(--color-bg-secondary);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-bold);
      margin: 0 auto;

      &.top-3 {
        background: var(--color-primary);
        color: white;
      }
    }
  }

  .distribution-card {
    .distribution-summary {
      display: flex;
      gap: var(--space-lg);
      margin-bottom: var(--space-lg);

      .summary-box {
        flex: 1;
        text-align: center;
        padding: var(--space-md);
        background: var(--color-bg-secondary);
        border-radius: var(--radius-md);

        .summary-num {
          font-size: var(--font-size-2xl);
          font-weight: var(--font-weight-bold);
          color: var(--color-primary);
        }

        .summary-text {
          font-size: var(--font-size-xs);
          color: var(--color-text-secondary);
          margin-top: var(--space-xs);
        }
      }
    }

    .distribution-list {
      display: flex;
      flex-direction: column;
      gap: var(--space-md);

      .distribution-item {
        padding: var(--space-md);
        background: var(--color-bg-secondary);
        border-radius: var(--radius-md);

        .item-info {
          margin-bottom: var(--space-sm);

          .item-name {
            font-weight: var(--font-weight-medium);
            color: var(--color-text-primary);
          }

          .item-code {
            font-size: var(--font-size-xs);
            color: var(--color-text-secondary);
          }
        }

        .item-stats {
          display: flex;
          gap: var(--space-xs);
          margin-bottom: var(--space-sm);
        }
      }
    }
  }
}

@media (max-width: 1200px) {
  .stats-page {
    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .charts-row {
      grid-template-columns: 1fr;
    }

    .bottom-row {
      grid-template-columns: 1fr;
    }
  }
}

@media (max-width: 768px) {
  .stats-page {
    .stats-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
