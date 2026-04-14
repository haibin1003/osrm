<template>
  <div class="home-page" :class="{ 'fullscreen-mode': isFullscreen }">
    <div class="page-header" v-if="!isFullscreen">
      <div class="header-left">
        <h1 class="page-title">欢迎回来，{{ authStore.userInfo?.realName || '管理员' }}</h1>
        <p class="page-subtitle">开源软件仓库管理系统</p>
      </div>
      <div class="header-right">
        <el-button v-if="!isFullscreen" type="primary" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
          {{ isFullscreen ? '退出大屏' : '大屏模式' }}
        </el-button>
      </div>
    </div>

    <!-- 全屏模式顶部 -->
    <div class="fullscreen-header" v-if="isFullscreen">
      <div class="fullscreen-title">
        <h1>OSRM 数据驾驶舱</h1>
        <p>开源软件仓库管理系统 - 实时数据监控</p>
      </div>
      <div class="fullscreen-time">{{ currentTime }}</div>
    </div>

    <!-- 统计概览卡片 (增强版) -->
    <div class="stats-overview" :class="{ 'fullscreen-stats': isFullscreen }">
      <div class="stat-card-large">
        <div class="stat-icon-large" style="background: linear-gradient(135deg, rgba(99, 91, 255, 0.15), rgba(162, 89, 255, 0.15));">
          <el-icon :size="28" color="#635bff"><Box /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value-large">{{ overview.totalPackages }}</div>
          <div class="stat-label-large">已发布软件包</div>
        </div>
        <div class="stat-trend" :class="getTrendClass(overview.trends?.totalPackagesChange)">
          <el-icon><component :is="getTrendIcon(overview.trends?.totalPackagesChange)" /></el-icon>
          <span>{{ formatTrend(overview.trends?.totalPackagesChange) }}</span>
        </div>
      </div>

      <div class="stat-card-large">
        <div class="stat-icon-large" style="background: rgba(36, 180, 126, 0.15);">
          <el-icon :size="28" color="#24b47e"><ShoppingCart /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value-large">{{ overview.totalSubscriptions }}</div>
          <div class="stat-label-large">总订购数</div>
        </div>
        <div class="stat-trend" :class="getTrendClass(overview.trends?.totalSubscriptionsChange)">
          <el-icon><component :is="getTrendIcon(overview.trends?.totalSubscriptionsChange)" /></el-icon>
          <span>{{ formatTrend(overview.trends?.totalSubscriptionsChange) }}</span>
        </div>
      </div>

      <div class="stat-card-large">
        <div class="stat-icon-large" style="background: rgba(217, 119, 6, 0.15);">
          <el-icon :size="28" color="#d97706"><OfficeBuilding /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value-large">{{ overview.activeBusinessSystems }}</div>
          <div class="stat-label-large">活跃业务系统</div>
        </div>
        <div class="stat-trend" :class="getTrendClass(overview.trends?.activeBusinessSystemsChange)">
          <el-icon><component :is="getTrendIcon(overview.trends?.activeBusinessSystemsChange)" /></el-icon>
          <span>{{ formatTrend(overview.trends?.activeBusinessSystemsChange) }}</span>
        </div>
      </div>

      <div class="stat-card-large">
        <div class="stat-icon-large" style="background: rgba(99, 91, 255, 0.15);">
          <el-icon :size="28" color="#635bff"><Calendar /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value-large">{{ overview.newSubscriptionsThisMonth }}</div>
          <div class="stat-label-large">本月新增订购</div>
        </div>
        <div class="stat-trend" :class="getTrendClass(overview.trends?.newSubscriptionsThisMonthChange)">
          <el-icon><component :is="getTrendIcon(overview.trends?.newSubscriptionsThisMonthChange)" /></el-icon>
          <span>{{ formatTrend(overview.trends?.newSubscriptionsThisMonthChange) }}</span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row" :class="{ 'fullscreen-charts': isFullscreen }">
      <!-- 订购趋势图 -->
      <div class="chart-card stripe-card">
        <div class="chart-header">
          <span class="chart-title">订购趋势</span>
          <el-radio-group v-if="!isFullscreen" v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :label="7">近7天</el-radio-button>
            <el-radio-button :label="14">近14天</el-radio-button>
            <el-radio-button :label="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
        <div class="trend-summary" v-if="trend.summary && !isFullscreen">
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
      </div>

      <!-- 软件类型分布图 -->
      <div class="chart-card stripe-card">
        <div class="chart-header">
          <span class="chart-title">软件类型分布</span>
        </div>
        <v-chart class="chart pie-chart" :option="typeChartOption" autoresize />
        <div class="type-legend" v-if="!isFullscreen">
          <div v-for="item in typeDistribution.data" :key="item.type" class="legend-item">
            <span class="legend-color" :style="{ background: item.color }"></span>
            <span class="legend-label">{{ item.typeName }}</span>
            <span class="legend-value">{{ item.packageCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 下方区域 -->
    <div class="bottom-row" :class="{ 'fullscreen-bottom': isFullscreen }">
      <!-- 软件包热度排行 -->
      <div class="ranking-card stripe-card">
        <div class="card-header">
          <span class="card-title">软件包热度排行</span>
          <el-radio-group v-if="!isFullscreen" v-model="rankingSortBy" size="small" @change="loadPopularity">
            <el-radio-button label="subscription_count">按订购数</el-radio-button>
            <el-radio-button label="business_system_count">按业务系统数</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="popularity.data" size="small" :class="{ 'fullscreen-table': isFullscreen }">
          <el-table-column type="index" label="排名" width="60" align="center">
            <template #default="{ $index }">
              <div class="rank-cell" :class="{ 'top-3': $index < 3 }">{{ $index + 1 }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="packageName" label="软件包" min-width="150" show-overflow-tooltip />
          <el-table-column prop="softwareType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ formatType(row.softwareType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="subscriptionCount" label="订购数" width="90" align="center" sortable />
          <el-table-column prop="businessSystemCount" label="关联系统" width="90" align="center" sortable />
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
      </div>

      <!-- 业务系统分布 -->
      <div class="distribution-card stripe-card">
        <div class="card-header">
          <span class="card-title">业务系统分布</span>
          <el-link type="primary" @click="$router.push('/business/systems')" v-if="!isFullscreen">查看全部</el-link>
        </div>
        <div class="distribution-summary" v-if="!isFullscreen">
          <div class="summary-box">
            <div class="summary-num">{{ businessDistribution.totalBusinessSystems }}</div>
            <div class="summary-text">活跃系统</div>
          </div>
          <div class="summary-box">
            <div class="summary-num">{{ businessDistribution.totalSubscriptions }}</div>
            <div class="summary-text">总订购</div>
          </div>
        </div>
        <div class="distribution-list">
          <div v-for="item in businessDistribution.data?.slice(0, isFullscreen ? 10 : 5)" :key="item.systemId" class="distribution-item">
            <div class="item-info">
              <div class="item-name">{{ item.systemName }}</div>
              <div class="item-code">{{ item.systemCode }}</div>
            </div>
            <div class="item-stats">
              <el-tag size="small" type="primary">{{ item.packageCount }}</el-tag>
              <el-tag size="small" type="success">{{ item.subscriptionCount }}</el-tag>
            </div>
            <el-progress v-if="!isFullscreen" :percentage="item.percentage" :stroke-width="6" :show-text="false" />
          </div>
        </div>
      </div>
    </div>

    <!-- 退出大屏按钮 -->
    <el-button v-if="isFullscreen" class="exit-fullscreen-btn" type="primary" @click="toggleFullscreen">
      <el-icon><Close /></el-icon>
      退出大屏
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/modules/auth'
import { Plus, ShoppingCart, OfficeBuilding, Files, View, Box, Collection, DataLine, Star, Document, List, ArrowUp, ArrowDown, Minus, Calendar, FullScreen, Close } from '@element-plus/icons-vue'
import { portalApi } from '@/api/portal'
import { statisticsApi } from '@/api/statistics'
import type { SoftwarePackage } from '@/types/software'
import type { PortalStats, StatsTrendItem } from '@/api/portal'
import type { StatisticsOverviewDTO, TrendDataDTO, BusinessDistributionDTO, PopularityRankingDTO, TypeDistributionDTO } from '@/api/statistics'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'

// Register ECharts components
use([CanvasRenderer, PieChart, LineChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const authStore = useAuthStore()
const recent = ref<SoftwarePackage[]>([])
const popular = ref<SoftwarePackage[]>([])
const trend = ref<StatsTrendItem[]>([])
const stats = reactive<PortalStats>({ totalPackages: 0, totalByType: {} })

// 统计看板数据
const overview = reactive<Partial<StatisticsOverviewDTO>>({})
const trendData = reactive<Partial<TrendDataDTO>>({ summary: undefined, data: [] })
const businessDistribution = reactive<Partial<BusinessDistributionDTO>>({ data: [] })
const popularity = reactive<Partial<PopularityRankingDTO>>({ data: [] })
const typeDistribution = reactive<Partial<TypeDistributionDTO>>({ data: [] })

// 大屏模式
const isFullscreen = ref(false)
const currentTime = ref('')
let timeInterval: ReturnType<typeof setInterval>

// UI state
const trendDays = ref(7)
const rankingSortBy = ref('subscription_count')

// 快捷操作权限
const canCreatePackage = computed(() => authStore.hasPermission('package:create'))
const canApplySubscription = computed(() => authStore.hasPermission('subscription:create'))
const canViewBusinessSystem = computed(() => authStore.hasPermission('business-system:read'))
const canViewStorage = computed(() => authStore.hasPermission('storage:read'))
const canRegisterInventory = computed(() => authStore.hasPermission('inventory:create'))
const canViewMyInventory = computed(() => authStore.hasPermission('inventory:read'))

// 趋势图标配置
const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: trendData.data?.map(d => d.date.slice(5)) || [],
    axisLine: { lineStyle: { color: '#e6ebf1' } },
    axisLabel: { color: '#525f7f' }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#f0f0f0' } },
    axisLabel: { color: '#525f7f' }
  },
  series: [
    {
      name: '总订购',
      type: 'bar',
      data: trendData.data?.map(d => d.subscriptionCount) || [],
      itemStyle: { color: '#635bff', borderRadius: [4, 4, 0, 0] }
    },
    {
      name: '已审批',
      type: 'line',
      data: trendData.data?.map(d => d.approvedCount) || [],
      smooth: true,
      itemStyle: { color: '#24b47e' },
      lineStyle: { width: 2 }
    },
    {
      name: '待审批',
      type: 'line',
      data: trendData.data?.map(d => d.pendingCount) || [],
      smooth: true,
      itemStyle: { color: '#d97706' },
      lineStyle: { width: 2 }
    }
  ]
}))

// 类型分布图表配置
const typeChartOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
    label: { show: false },
    emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
    data: typeDistribution.data?.map(item => ({
      value: item.packageCount,
      name: item.typeName,
      itemStyle: { color: item.color }
    })) || []
  }]
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

// 大屏模式
function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  if (isFullscreen.value) {
    document.body.classList.add('fullscreen-body')
  } else {
    document.body.classList.remove('fullscreen-body')
  }
}

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// Load functions
async function loadTrend() {
  try {
    const data = await statisticsApi.getTrend(trendDays.value)
    Object.assign(trendData, data)
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

onMounted(async () => {
  // 加载首页原有数据
  try {
    const [listRes, statsRes, popularRes, overviewRes, trendRes] = await Promise.all([
      portalApi.listSoftware({ page: 1, size: 5 }),
      portalApi.getStats(),
      portalApi.getPopular(),
      portalApi.getStatsOverview(),
      portalApi.getStatsTrend(7)
    ])
    recent.value = listRes.content
    Object.assign(stats, statsRes)
    popular.value = popularRes
    Object.assign(overview, overviewRes)
    trend.value = trendRes
  } catch {
    // 忽略错误，显示空数据
  }

  // 加载统计看板数据
  try {
    const [overviewData, trendRes, businessDist, popularityData, typeDist] = await Promise.all([
      statisticsApi.getOverview(),
      statisticsApi.getTrend(7),
      statisticsApi.getBusinessDistribution(10),
      statisticsApi.getPopularity(10, rankingSortBy.value),
      statisticsApi.getTypeDistribution()
    ])
    Object.assign(overview, overviewData)
    Object.assign(trendData, trendRes)
    Object.assign(businessDistribution, businessDist)
    Object.assign(popularity, popularityData)
    Object.assign(typeDistribution, typeDist)
  } catch (e) {
    console.error('Failed to load statistics:', e)
  }

  // 启动时钟
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
  document.body.classList.remove('fullscreen-body')
})
</script>

<style scoped lang="scss">
.home-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: var(--space-xl);

    .page-title {
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-light);
      margin: 0;
      color: var(--color-text-primary);
      letter-spacing: -0.3px;
    }
    .page-subtitle {
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
      margin: var(--space-xs) 0 0;
      font-weight: var(--font-weight-light);
    }
  }

  // 统计概览卡片 (增强版)
  .stats-overview {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-lg);
    margin-bottom: var(--space-xl);
  }

  .stat-card-large {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-xl);
    display: flex;
    align-items: center;
    gap: var(--space-md);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(135deg, #635bff, #a259ff);
    }

    .stat-icon-large {
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

    .stat-value-large {
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
      line-height: 1.2;
    }

    .stat-label-large {
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
      margin-top: var(--space-xs);
    }

    .stat-trend {
      display: flex;
      align-items: center;
      gap: var(--space-xs);
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-medium);

      &.trend-up { color: var(--color-success); }
      &.trend-down { color: var(--color-danger); }
      &.trend-stable { color: var(--color-text-secondary); }
    }
  }

  // Charts Row
  .charts-row {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: var(--space-lg);
    margin-bottom: var(--space-xl);
  }

  .chart-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    overflow: hidden;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(135deg, #635bff, #a259ff);
    }

    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--space-lg) var(--space-lg) var(--space-md);
      border-bottom: 1px solid var(--color-border-light);
    }

    .chart-title {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-medium);
      color: var(--color-text-primary);
    }

    .chart {
      height: 260px;
      padding: var(--space-md);
    }

    .pie-chart {
      height: 200px;
    }

    .trend-summary {
      display: flex;
      gap: var(--space-lg);
      margin-bottom: var(--space-md);
      padding: var(--space-md);
      background: var(--color-bg-page);
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

          &.success { color: var(--color-success); }
          &.danger { color: var(--color-danger); }
        }
      }
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

  // Bottom Row
  .bottom-row {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: var(--space-lg);
  }

  .ranking-card,
  .distribution-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    overflow: hidden;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(135deg, #635bff, #a259ff);
    }
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--space-lg) var(--space-lg) var(--space-md);
    border-bottom: 1px solid var(--color-border-light);
  }

  .card-title {
    font-size: var(--font-size-sm);
    font-weight: var(--font-weight-medium);
    color: var(--color-text-primary);
  }

  // 排名单元格
  .rank-cell {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: var(--color-bg-page);
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

  // 业务系统分布
  .distribution-summary {
    display: flex;
    gap: var(--space-lg);
    margin-bottom: var(--space-lg);
    padding: var(--space-md);
    background: var(--color-bg-page);
    border-radius: var(--radius-md);

    .summary-box {
      flex: 1;
      text-align: center;

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
    padding: var(--space-md);

    .distribution-item {
      padding: var(--space-md);
      background: var(--color-bg-page);
      border-radius: var(--radius-md);
      margin-bottom: var(--space-sm);

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

  // ===== 大屏模式样式 =====
  &.fullscreen-mode {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 9999;
    background: var(--color-bg-page);
    padding: var(--space-xl);
    overflow-y: auto;

    .fullscreen-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--space-xl);
      padding-bottom: var(--space-lg);
      border-bottom: 1px solid var(--color-border);

      .fullscreen-title {
        h1 {
          font-size: var(--font-size-3xl);
          font-weight: var(--font-weight-bold);
          color: var(--color-primary);
          margin: 0 0 var(--space-xs);
        }

        p {
          font-size: var(--font-size-md);
          color: var(--color-text-secondary);
          margin: 0;
        }
      }

      .fullscreen-time {
        font-size: var(--font-size-xl);
        font-weight: var(--font-weight-medium);
        color: var(--color-text-primary);
      }
    }

    .stats-overview.fullscreen-stats {
      grid-template-columns: repeat(4, 1fr);
      gap: var(--space-xl);
    }

    .stat-card-large {
      padding: var(--space-2xl);

      .stat-icon-large {
        width: 56px;
        height: 56px;
      }

      .stat-value-large {
        font-size: 36px;
      }

      .stat-label-large {
        font-size: var(--font-size-md);
      }
    }

    .charts-row.fullscreen-charts {
      grid-template-columns: 1.5fr 1fr;
      margin-bottom: var(--space-xl);

      .chart-card .chart {
        height: 320px;
      }
    }

    .bottom-row.fullscreen-bottom {
      grid-template-columns: 1.5fr 1fr;
    }

    .exit-fullscreen-btn {
      position: fixed;
      bottom: var(--space-xl);
      right: var(--space-xl);
      z-index: 10000;
    }
  }
}

@media (max-width: 1200px) {
  .home-page {
    .stats-overview { grid-template-columns: repeat(2, 1fr); }
    .charts-row { grid-template-columns: 1fr; }
    .bottom-row { grid-template-columns: 1fr; }

    &.fullscreen-mode {
      .stats-overview.fullscreen-stats { grid-template-columns: repeat(2, 1fr); }
      .charts-row.fullscreen-charts { grid-template-columns: 1fr; }
      .bottom-row.fullscreen-bottom { grid-template-columns: 1fr; }
    }
  }
}

@media (max-width: 768px) {
  .home-page {
    .stats-overview { grid-template-columns: 1fr; }
  }
}
</style>
