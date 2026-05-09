<template>
  <div class="home-page" :class="{ 'fullscreen-mode': isFullscreen }">
    <!-- 顶部欢迎区 -->
    <div class="hero-section" v-if="!isFullscreen">
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="greeting">欢迎回来</span>
          <span class="username">{{ authStore.userInfo?.realName || '管理员' }}</span>
        </h1>
        <p class="hero-subtitle">开源软件仓库管理系统 - 实时数据监控</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
          大屏模式
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

    <!-- 统计卡片区 -->
    <div class="stats-grid" :class="{ 'fullscreen-stats': isFullscreen }">
      <div class="stat-card" v-for="(stat, index) in statsConfig" :key="index" :style="{ '--accent-color': stat.color }">
        <div class="stat-bg-gradient"></div>
        <div class="stat-icon-wrapper">
          <el-icon :size="20" :color="stat.color"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
        <div class="stat-trend" :class="getTrendClass(stat.change)">
          <el-icon><component :is="getTrendIcon(stat.change)" /></el-icon>
          <span>{{ formatTrend(stat.change) }}</span>
        </div>
      </div>
    </div>

    <!-- 快捷操作入口 -->
    <div class="quick-actions" v-if="!isFullscreen">
      <div class="quick-actions-card">
        <div class="quick-header">
          <span class="quick-title">快捷操作</span>
        </div>
        <div class="quick-buttons">
          <el-button v-if="canCreatePackage" type="primary" plain @click="$router.push('/software/packages?action=create')">
            <el-icon><Plus /></el-icon>
            新建软件包
          </el-button>
          <el-button v-if="canApplySubscription" type="primary" plain @click="$router.push('/subscription/my?apply=true')">
            <el-icon><ShoppingCart /></el-icon>
            申请订购
          </el-button>
          <el-button v-if="canRegisterInventory" type="primary" plain @click="$router.push('/inventory/create')">
            <el-icon><Document /></el-icon>
            存量登记
          </el-button>
          <el-button v-if="canViewMyInventory" type="primary" plain @click="$router.push('/inventory/my')">
            <el-icon><List /></el-icon>
            我的存量
          </el-button>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="main-content" :class="{ 'fullscreen-charts': isFullscreen }">
      <!-- 订购趋势 - 全宽 -->
      <div class="chart-card trend-card">
        <div class="chart-header">
          <div class="header-left">
            <span class="chart-title">订购趋势</span>
            <el-radio-group v-if="!isFullscreen" v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :label="7">近7天</el-radio-button>
              <el-radio-button :label="14">近14天</el-radio-button>
              <el-radio-button :label="30">近30天</el-radio-button>
            </el-radio-group>
          </div>
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
        <v-chart class="chart trend-chart" :option="trendChartOption" autoresize />
      </div>

      <!-- 中间行: 热度排行 + 类型分布 并排 -->
      <div class="middle-row">
        <div class="ranking-card content-card">
          <div class="card-header">
            <span class="card-title">软件包热度排行</span>
            <el-radio-group v-if="!isFullscreen" v-model="rankingSortBy" size="small" @change="loadPopularity">
              <el-radio-button label="subscription_count">按订购数</el-radio-button>
              <el-radio-button label="business_system_count">按业务系统数</el-radio-button>
            </el-radio-group>
          </div>
          <el-table :data="popularity.data" size="small" :class="{ 'fullscreen-table': isFullscreen }" :show-header="!isFullscreen" :resizable="true">
            <el-table-column type="index" label="#" width="40" align="center" :resizable="false">
              <template #default="{ $index }">
                <span class="rank-cell" :class="{ 'top-3': $index < 3 }">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="packageName" label="软件包" min-width="80" show-overflow-tooltip />
            <el-table-column prop="softwareType" label="类型" width="65">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ formatType(row.softwareType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="subscriptionCount" label="订购" width="55" align="center" sortable />
            <el-table-column prop="businessSystemCount" label="系统" width="55" align="center" sortable />
            <el-table-column prop="trend" label="趋势" width="65" align="center">
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

        <div class="chart-card type-card">
          <div class="chart-header">
            <span class="chart-title">软件类型分布</span>
          </div>
          <v-chart class="chart pie-chart" :option="typeChartOption" autoresize />
        </div>
      </div>

      <!-- 业务系统分布 -->
      <div class="distribution-card content-card">
        <div class="card-header">
          <span class="card-title">业务系统分布</span>
          <el-link type="primary" @click="$router.push('/business/systems')" v-if="!isFullscreen">查看全部</el-link>
        </div>
        <div class="distribution-summary" v-if="!isFullscreen">
          <div class="summary-box">
            <span class="summary-num">{{ businessDistribution.totalBusinessSystems }}</span>
            <span class="summary-text">活跃系统</span>
          </div>
          <div class="summary-box">
            <span class="summary-num">{{ businessDistribution.totalSubscriptions }}</span>
            <span class="summary-text">总订购</span>
          </div>
        </div>
        <div class="distribution-list" :class="{ 'distribution-scroll': isFullscreen }">
          <div class="distribution-scroll-inner" :style="scrollTransform">
            <template v-for="(item, idx) in displayDistributionItems" :key="isFullscreen ? `${item.systemId}-${idx}` : item.systemId">
              <div class="distribution-item">
                <div class="item-info">
                  <span class="item-name">{{ item.systemName }}</span>
                  <span class="item-code">{{ item.systemCode }}</span>
                </div>
                <div class="item-stats">
                  <el-tag size="small" type="primary">{{ item.packageCount }}</el-tag>
                  <el-tag size="small" type="success">{{ item.subscriptionCount }}</el-tag>
                </div>
                <el-progress v-if="!isFullscreen" :percentage="item.percentage" :stroke-width="6" :show-text="false" />
              </div>
            </template>
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

// Stats card configuration
const statsConfig = computed(() => [
  {
    icon: Box,
    value: overview.totalPackages || 0,
    label: '已发布软件包',
    change: overview.trends?.totalPackagesChange || 0,
    color: '#3B6FF5'
  },
  {
    icon: ShoppingCart,
    value: overview.totalSubscriptions || 0,
    label: '总订购数',
    change: overview.trends?.totalSubscriptionsChange || 0,
    color: '#16A349'
  },
  {
    icon: OfficeBuilding,
    value: overview.activeBusinessSystems || 0,
    label: '活跃业务系统',
    change: overview.trends?.activeBusinessSystemsChange || 0,
    color: '#d97706'
  },
  {
    icon: Calendar,
    value: overview.newSubscriptionsThisMonth || 0,
    label: '本月新增订购',
    change: overview.trends?.newSubscriptionsThisMonthChange || 0,
    color: '#6366F1'
  }
])

// 业务系统分布滚动（大屏模式）
const SCROLL_SPEED = 30 // px per second
const scrollOffset = ref(0)
let scrollRafId: number | null = null
const ITEM_HEIGHT = 72

const displayDistributionItems = computed(() => {
  const data = businessDistribution.data || []
  if (data.length === 0) return []
  if (!isFullscreen.value) return data.slice(0, 5)
  return [...data, ...data]
})

const scrollTransform = computed(() => {
  return isFullscreen.value ? { transform: `translateY(-${scrollOffset.value}px)` } : {}
})

function startScroll() {
  if (scrollRafId !== null) return
  let lastTime = performance.now()
  function tick(now: number) {
    const dt = Math.min((now - lastTime) / 1000, 0.1) // cap at 100ms to avoid jumps
    lastTime = now
    const count = businessDistribution.data?.length || 0
    if (count > 0) {
      const lapHeight = count * ITEM_HEIGHT
      scrollOffset.value += SCROLL_SPEED * dt
      if (scrollOffset.value >= lapHeight) {
        scrollOffset.value -= lapHeight
      }
    }
    scrollRafId = requestAnimationFrame(tick)
  }
  scrollRafId = requestAnimationFrame(tick)
}

function stopScroll() {
  if (scrollRafId !== null) {
    cancelAnimationFrame(scrollRafId)
    scrollRafId = null
  }
  scrollOffset.value = 0
}

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
    axisLine: { lineStyle: { color: '#E2E8F0' } },
    axisLabel: { color: '#64748B' }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#F1F5F9' } },
    axisLabel: { color: '#64748B' }
  },
  series: [
    {
      name: '总订购',
      type: 'bar',
      data: trendData.data?.map(d => d.subscriptionCount) || [],
      itemStyle: { color: '#3B6FF5', borderRadius: [4, 4, 0, 0] }
    },
    {
      name: '已审批',
      type: 'line',
      data: trendData.data?.map(d => d.approvedCount) || [],
      smooth: true,
      itemStyle: { color: '#16A349' },
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
const PIE_COLORS = ['#3B6FF5', '#16A349', '#F59E0B', '#EF4444', '#8B5CF6', '#06B6D4', '#EC4899', '#F97316']

const typeChartOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: {
    orient: 'vertical',
    right: 10,
    top: 'center',
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { fontSize: 12, color: '#64748B' }
  },
  series: [{
    type: 'pie',
    radius: ['45%', '75%'],
    center: ['35%', '50%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
    label: {
      show: true,
      position: 'inside',
      formatter: '{d}%',
      fontSize: 12,
      fontWeight: 'bold',
      color: '#fff'
    },
    data: typeDistribution.data?.map((item, i) => ({
      value: item.packageCount,
      name: item.typeName,
      itemStyle: { color: item.color || PIE_COLORS[i % PIE_COLORS.length] }
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
    startScroll()
  } else {
    document.body.classList.remove('fullscreen-body')
    stopScroll()
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
  stopScroll()
  document.body.classList.remove('fullscreen-body')
})
</script>

<style scoped lang="scss">
.home-page {
  // Hero Section
  .hero-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-md);
    padding: var(--space-lg);
    background: var(--color-bg-card);
    border-radius: var(--radius-xl);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      bottom: 0;
      width: 4px;
      background: var(--color-primary);
    }

    &::after {
      content: '';
      position: absolute;
      top: -50%;
      right: -10%;
      width: 300px;
      height: 300px;
      background: radial-gradient(circle, rgba(59, 111, 245, 0.06) 0%, transparent 70%);
      pointer-events: none;
    }

    .hero-content {
      position: relative;
      z-index: 1;

      .hero-title {
        margin: 0 0 var(--space-xs);
        display: flex;
        align-items: baseline;
        gap: var(--space-sm);

        .greeting {
          font-size: var(--font-size-2xl);
          font-weight: var(--font-weight-light);
          color: var(--color-text-secondary);
        }

        .username {
          font-size: var(--font-size-3xl);
          font-weight: var(--font-weight-semibold);
          color: var(--color-text-primary);
        }
      }

      .hero-subtitle {
        font-size: var(--font-size-sm);
        color: var(--color-text-secondary);
        margin: 0;
      }
    }

    .hero-actions {
      position: relative;
      z-index: 1;
    }
  }

  // Stats Grid
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-md);
    margin-bottom: var(--space-md);
  }

  .stat-card {
    background: var(--color-bg-card);
    border-radius: var(--radius-lg);
    padding: var(--space-sm) var(--space-lg);
    position: relative;
    overflow: hidden;
    transition: all 0.3s ease;
    border: 1px solid transparent;
    display: flex;
    align-items: center;
    gap: var(--space-md);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
      border-color: var(--accent-color);
    }

    .stat-bg-gradient {
      position: absolute;
      top: -20px;
      right: -20px;
      width: 60px;
      height: 60px;
      background: radial-gradient(circle, rgba(var(--accent-color), 0.1) 0%, transparent 70%);
      opacity: 0.5;
    }

    .stat-icon-wrapper {
      width: 36px;
      height: 36px;
      border-radius: var(--radius-md);
      background: var(--color-primary-subtle);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .stat-info {
      flex: 1;
      min-width: 0;
      display: flex;
      align-items: baseline;
      gap: 6px;

      .stat-value {
        font-size: 20px;
        font-weight: var(--font-weight-bold);
        color: var(--color-text-primary);
        line-height: 1;
      }

      .stat-label {
        font-size: var(--font-size-xs);
        color: var(--color-text-secondary);
        white-space: nowrap;
      }
    }

    .stat-trend {
      display: inline-flex;
      align-items: center;
      gap: var(--space-xs);
      font-size: 11px;
      font-weight: var(--font-weight-medium);
      padding: 2px var(--space-sm);
      border-radius: var(--radius-md);
      background: var(--color-bg-page);
      flex-shrink: 0;

      &.trend-up {
        color: var(--color-success);
        background: var(--color-success-light);
      }
      &.trend-down {
        color: var(--color-danger);
        background: var(--color-danger-light);
      }
      &.trend-stable {
        color: var(--color-text-secondary);
      }
    }
  }

  // Quick Actions
  .quick-actions {
    margin-bottom: var(--space-md);

    .quick-actions-card {
      background: var(--color-bg-card);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-xl);
      padding: var(--space-md) var(--space-lg);
      position: relative;

      .quick-header {
        margin-bottom: var(--space-sm);

        .quick-title {
          font-size: var(--font-size-sm);
          font-weight: var(--font-weight-semibold);
          color: var(--color-text-primary);
        }
      }

      .quick-buttons {
        display: flex;
        gap: var(--space-sm);
        flex-wrap: wrap;

        .el-button {
          border-radius: var(--radius-md);
        }
      }
    }
  }

  // Main Content
  .main-content {
    display: flex;
    flex-direction: column;
    gap: var(--space-md);
  }

  .trend-card {
    .trend-chart {
      height: 220px;
    }
  }

  .middle-row {
    display: grid;
    grid-template-columns: 3fr 2fr;
    gap: var(--space-md);
  }

  .type-card {
    .pie-chart {
      height: 200px;
    }
  }

  // Chart Card
  .chart-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-xl);
    overflow: hidden;
    position: relative;

    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--space-md) var(--space-lg);

      .header-left {
        display: flex;
        align-items: center;
        gap: var(--space-md);
      }
    }

    .chart-title {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
    }

    .chart {
      height: 200px;
      padding: var(--space-md);
    }

    .trend-summary {
      display: flex;
      gap: var(--space-md);
      padding: var(--space-sm) var(--space-md);
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

  }

  // Ranking Card
  .ranking-card {
    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: var(--space-md) var(--space-lg);
    }

    .card-title {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
    }
  }

  // Rank Cell
  .rank-cell {
    font-size: var(--font-size-xs);
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-secondary);

    &.top-3 {
      color: var(--color-primary);
      font-weight: var(--font-weight-bold);
    }
  }

  // Distribution Card
  .distribution-card {
    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: var(--space-md) var(--space-lg);
    }

    .card-title {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
    }

    .distribution-summary {
      display: flex;
      gap: var(--space-md);
      padding: var(--space-sm) var(--space-md);
      background: var(--color-bg-page);
      border-radius: var(--radius-md);

      .summary-box {
        flex: 1;
        text-align: center;

        .summary-num {
          font-size: var(--font-size-xl);
          font-weight: var(--font-weight-bold);
          color: var(--color-primary);
        }

        .summary-text {
          font-size: var(--font-size-xs);
          color: var(--color-text-secondary);
        }
      }
    }

    .distribution-list {
      padding: var(--space-sm) var(--space-md);

      &.distribution-scroll {
        max-height: 340px;
        overflow: hidden;
        padding: var(--space-sm);

        .distribution-scroll-inner {
          will-change: transform;
        }

        .distribution-item {
          padding: var(--space-xs) var(--space-md);
          margin-bottom: 2px;
        }
      }

      .distribution-item {
        display: flex;
        align-items: center;
        gap: var(--space-sm);
        padding: var(--space-sm) var(--space-md);
        background: var(--color-bg-page);
        border-radius: var(--radius-md);
        margin-bottom: var(--space-xs);
        transition: all 0.2s;

        &:hover {
          background: var(--color-bg-hover);
        }

        .item-info {
          flex: 1;
          min-width: 0;
          display: flex;
          align-items: center;
          gap: var(--space-sm);

          .item-name {
            font-weight: var(--font-weight-medium);
            color: var(--color-text-primary);
            font-size: var(--font-size-sm);
            white-space: nowrap;
          }

          .item-code {
            font-size: var(--font-size-xs);
            color: var(--color-text-tertiary);
          }
        }

        .item-stats {
          display: flex;
          gap: var(--space-xs);
          flex-shrink: 0;
        }
      }
    }
  }

  // ===== Fullscreen Mode - Dark Data Cockpit =====
  &.fullscreen-mode {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 9999;
    background: #0B1120;
    padding: var(--space-2xl);
    overflow-y: auto;

    .fullscreen-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--space-2xl);
      padding-bottom: var(--space-xl);
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);

      .fullscreen-title {
        h1 {
          font-size: 32px;
          font-weight: var(--font-weight-bold);
          color: #F1F5F9;
          margin: 0 0 var(--space-xs);
          letter-spacing: -0.5px;
        }

        p {
          font-size: var(--font-size-md);
          color: #64748B;
          margin: 0;
        }
      }

      .fullscreen-time {
        font-size: var(--font-size-xl);
        font-weight: var(--font-weight-medium);
        color: #94A3B8;
        font-family: var(--font-mono);
      }
    }

    .stat-card {
      background: rgba(30, 41, 59, 0.6);
      border: 1px solid rgba(255, 255, 255, 0.06);
      backdrop-filter: blur(12px);
      border-radius: var(--radius-xl);

      .stat-value {
        color: #F1F5F9;
      }
      .stat-label {
        color: #94A3B8;
      }

      &:hover {
        border-color: var(--accent-color);
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
      }
    }

    .chart-card,
    .ranking-card,
    .distribution-card {
      background: rgba(30, 41, 59, 0.6);
      border: 1px solid rgba(255, 255, 255, 0.06);
      backdrop-filter: blur(12px);
      border-radius: var(--radius-xl);

      .chart-title,
      .card-title {
        color: #F1F5F9;
      }
    }

    .stats-grid.fullscreen-stats {
      grid-template-columns: repeat(4, 1fr);
      gap: var(--space-xl);
      margin-bottom: var(--space-2xl);
    }

    .main-content.fullscreen-charts {
      .trend-chart {
        height: 280px;
      }
      .chart {
        height: 280px;
      }
    }

    .middle-row {
      grid-template-columns: 5fr 2fr;
    }

    .exit-fullscreen-btn {
      position: fixed;
      bottom: var(--space-2xl);
      right: var(--space-2xl);
      z-index: 10000;
      border-radius: var(--radius-lg);
      padding: var(--space-md) var(--space-xl);
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.15);
      color: #F1F5F9;
      backdrop-filter: blur(8px);

      &:hover {
        background: rgba(255, 255, 255, 0.15);
      }
    }
  }
}

// Responsive
@media (max-width: 1400px) {
  .home-page {
    .main-content {
      grid-template-columns: 1fr;
    }

    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }
}

@media (max-width: 1200px) {
  .home-page {
    &.fullscreen-mode {
      .stats-grid.fullscreen-stats {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  }
}

@media (max-width: 768px) {
  .home-page {
    .hero-section {
      flex-direction: column;
      align-items: flex-start;
      gap: var(--space-md);
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
