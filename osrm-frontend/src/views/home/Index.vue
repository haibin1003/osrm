<template>
  <div class="home-page" :class="{ 'fullscreen-mode': isFullscreen }">
    <!-- 大屏特效层 -->
    <canvas ref="particleCanvas" class="particle-layer" v-if="isFullscreen"></canvas>
    <div class="scan-line" v-if="isFullscreen"></div>

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
      <div class="header-bg"></div>
      <div class="header-title">OSRM 开源软件仓库数据驾驶舱</div>
      <div class="header-time">{{ currentTime }}</div>
    </div>

    <!-- 统计卡片区（普通模式） -->
    <div class="stats-grid" v-if="!isFullscreen">
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

    <!-- 全屏模式轮播卡片 -->
    <div class="fullscreen-carousel" v-if="isFullscreen">
      <div class="carousel-card" v-for="(card, idx) in fullscreenStatsConfig" :key="idx" :class="{ active: idx === 2 }">
        <div class="card-icon">
          <el-icon :size="28"><component :is="card.icon" /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-label">{{ card.label }}</div>
          <div class="card-value">{{ card.value }}</div>
          <div class="card-trend" :class="card.trendClass">
            <span>{{ card.trendText }}</span>
          </div>
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

    <!-- 图表区域（普通模式） -->
    <div class="main-content" v-if="!isFullscreen">
      <!-- 订购趋势 - 全宽 -->
      <div class="chart-card trend-card">
        <div class="chart-header">
          <div class="header-left">
            <span class="chart-title">订购趋势</span>
            <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :label="7">近7天</el-radio-button>
              <el-radio-button :label="14">近14天</el-radio-button>
              <el-radio-button :label="30">近30天</el-radio-button>
            </el-radio-group>
          </div>
        </div>
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
        <v-chart class="chart trend-chart" :option="trendChartOption" autoresize />
      </div>

      <!-- 中间行: 热度排行 + 类型分布 并排 -->
      <div class="middle-row">
        <div class="ranking-card content-card">
          <div class="card-header">
            <span class="card-title">软件包热度排行</span>
            <el-radio-group v-model="rankingSortBy" size="small" @change="loadPopularity">
              <el-radio-button label="subscription_count">按订购数</el-radio-button>
              <el-radio-button label="business_system_count">按业务系统数</el-radio-button>
            </el-radio-group>
          </div>
          <el-table :data="popularity.data" size="small" :show-header="true" :resizable="true">
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
          <el-link type="primary" @click="$router.push('/business/systems')">查看全部</el-link>
        </div>
        <div class="distribution-summary">
          <div class="summary-box">
            <span class="summary-num">{{ businessDistribution.totalBusinessSystems }}</span>
            <span class="summary-text">活跃系统</span>
          </div>
          <div class="summary-box">
            <span class="summary-num">{{ businessDistribution.totalSubscriptions }}</span>
            <span class="summary-text">总订购</span>
          </div>
        </div>
        <div class="distribution-list">
          <template v-for="item in displayDistributionItems" :key="item.systemId">
            <div class="distribution-item">
              <div class="item-info">
                <span class="item-name">{{ item.systemName }}</span>
                <span class="item-code">{{ item.systemCode }}</span>
              </div>
              <div class="item-stats">
                <el-tag size="small" type="primary">{{ item.packageCount }}</el-tag>
                <el-tag size="small" type="success">{{ item.subscriptionCount }}</el-tag>
              </div>
              <el-progress :percentage="item.percentage" :stroke-width="6" :show-text="false" />
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- 全屏模式主区域 -->
    <div class="fullscreen-main" :class="{ 'graph-fullscreen': isGraphFullscreen }" v-if="isFullscreen">
      <!-- 左栏：折线图 + 数据表格 -->
      <div class="fs-left" v-show="!isGraphFullscreen">
        <div class="fs-panel trend-panel">
          <div class="panel-title">
            <span class="title-icon"></span>
            <span>订购趋势</span>
          </div>
          <v-chart class="fs-chart" :option="trendChartOptionDark" autoresize />
        </div>
        <div class="fs-panel table-panel">
          <div class="panel-title">
            <span class="title-icon"></span>
            <span>软件包热度排行</span>
          </div>
          <div class="fs-table-wrap">
            <table class="fs-table fs-table-head">
              <thead>
                <tr>
                  <th style="width:44px">序号</th>
                  <th>软件包</th>
                  <th style="width:55px">类型</th>
                  <th style="width:50px">订购</th>
                  <th style="width:50px">系统</th>
                  <th style="width:70px">趋势</th>
                </tr>
              </thead>
            </table>
            <div class="fs-table-body-scroll" ref="rankingTableRef">
              <table class="fs-table">
                <tbody>
                  <tr v-for="(item, idx) in scrollingPopularityItems" :key="`${item.packageId}-${idx}`">
                    <td style="width:44px"><span class="fs-rank" :class="{ 'fs-rank-top': idx < 3 }">{{ idx + 1 }}</span></td>
                    <td>{{ item.packageName }}</td>
                    <td style="width:55px">{{ formatType(item.softwareType) }}</td>
                    <td style="width:50px">{{ item.subscriptionCount }}</td>
                    <td style="width:50px">{{ item.businessSystemCount }}</td>
                    <td style="width:70px">
                      <span class="fs-trend" :class="item.trend">
                        <el-icon :size="10"><ArrowUp v-if="item.trend === 'up'" /><ArrowDown v-else-if="item.trend === 'down'" /><Minus v-else /></el-icon>
                        {{ item.change > 0 ? '+' + item.change : item.change }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- 中栏：系统与软件关联图 -->
      <div class="fs-center">
        <div class="fs-panel graph-panel">
          <div class="panel-title">
            <span class="title-icon"></span>
            <span>系统与软件关联图</span>
            <el-switch
              v-model="graphShowAll"
              size="small"
              active-text="全部"
              inactive-text="关联"
              @change="loadGraph"
              style="margin-left: auto; --el-switch-on-color: #00B4FF; --el-switch-off-color: #4a5568"
            />
            <el-button
              type="primary"
              link
              size="small"
              @click="$router.push('/fullscreen-graph')"
              style="margin-left: 8px; color: #00B4FF;"
            >
              <el-icon><FullScreen /></el-icon>
              全屏查看
            </el-button>
          </div>
          <v-chart ref="graphChartRef" class="fs-chart" :option="graphChartOptionDark" autoresize @click="handleGraphClick" />

          <!-- 节点详情浮动面板 -->
          <div class="node-detail-float" v-if="selectedGraphNode && isGraphFullscreen">
            <div class="detail-header">{{ selectedGraphNode.name }}</div>
            <div class="detail-body">
              <div class="detail-row" v-if="selectedGraphNode.systemCode">
                <span class="detail-label">系统代码</span>
                <span class="detail-value">{{ selectedGraphNode.systemCode }}</span>
              </div>
              <div class="detail-row" v-if="selectedGraphNode.domain">
                <span class="detail-label">业务域</span>
                <span class="detail-value">{{ selectedGraphNode.domain }}</span>
              </div>
              <div class="detail-row" v-if="selectedGraphNode.softwareType">
                <span class="detail-label">软件类型</span>
                <span class="detail-value">{{ formatType(selectedGraphNode.softwareType) }}</span>
              </div>
              <div class="detail-row" v-if="nodeDetailPackages.length > 0">
                <span class="detail-label">关联软件</span>
                <span class="detail-value">{{ nodeDetailPackages.length }} 个</span>
              </div>
              <div class="detail-packages" v-if="nodeDetailPackages.length > 0">
                <span v-for="pkg in nodeDetailPackages" :key="pkg.packageId" class="pkg-tag">
                  {{ pkg.packageName }}
                </span>
              </div>
            </div>
          </div>
        </div>
        <div class="globe-stats">
          <div class="gstat">
            <div class="gstat-value">{{ graphData.metadata?.totalSystems || 0 }}</div>
            <div class="gstat-label">系统总数</div>
          </div>
          <div class="gstat">
            <div class="gstat-value">{{ graphData.metadata?.totalPackages || 0 }}</div>
            <div class="gstat-label">软件总数</div>
          </div>
        </div>
      </div>

      <!-- 右栏：雷达图 + 柱状图 -->
      <div class="fs-right" v-show="!isGraphFullscreen">
        <div class="fs-panel radar-panel">
          <div class="panel-title">
            <span class="title-icon"></span>
            <span>多维度数据分析</span>
          </div>
          <v-chart class="fs-chart" :option="radarChartOption" autoresize />
        </div>
        <div class="fs-panel bar-panel">
          <div class="panel-title">
            <span class="title-icon"></span>
            <span>系统软件分布</span>
          </div>
          <v-chart class="fs-chart" :option="barChartOptionDark" autoresize />
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
import { ref, reactive, onMounted, computed, onUnmounted, nextTick } from 'vue'
import { useAuthStore } from '@/stores/modules/auth'
import { Plus, ShoppingCart, OfficeBuilding, Files, View, Box, Collection, DataLine, Star, Document, List, ArrowUp, ArrowDown, Minus, Calendar, FullScreen, Close } from '@element-plus/icons-vue'
import { portalApi } from '@/api/portal'
import { statisticsApi } from '@/api/statistics'
import { trackingApi } from '@/api/tracking'
import type { SoftwarePackage } from '@/types/software'
import type { PortalStats, StatsTrendItem } from '@/api/portal'
import type { StatisticsOverviewDTO, TrendDataDTO, BusinessDistributionDTO, PopularityRankingDTO, TypeDistributionDTO } from '@/api/statistics'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart, BarChart, RadarChart, GraphChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent, RadarComponent } from 'echarts/components'
import VChart from 'vue-echarts'

// Register ECharts components
use([CanvasRenderer, PieChart, LineChart, BarChart, RadarChart, GraphChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, RadarComponent])

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
const graphData = reactive<{ nodes: any[]; edges: any[]; metadata: { totalSystems: number; totalPackages: number; totalSubscriptions: number } }>({
  nodes: [], edges: [], metadata: { totalSystems: 0, totalPackages: 0, totalSubscriptions: 0 }
})

// 大屏模式
const isFullscreen = ref(false)
const graphShowAll = ref(false)
const isGraphFullscreen = ref(false)
const selectedGraphNode = ref<any>(null)
const nodeDetailPackages = ref<any[]>([])
const graphChartRef = ref<any>(null)
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
    const dt = Math.min((now - lastTime) / 1000, 0.1)
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

// 排行列表轮播（大屏模式）
const RANKING_ITEM_HEIGHT = 44
const rankingTableRef = ref<HTMLElement>()
const rankingScrollOffset = ref(0)
let rankingScrollRafId: number | null = null

const scrollingPopularityItems = computed(() => {
  const data = popularity.data || []
  if (data.length === 0) return []
  if (!isFullscreen.value) return data
  return [...data, ...data]
})

function startRankingScroll() {
  if (rankingScrollRafId !== null) return
  let lastTime = performance.now()
  function tick(now: number) {
    const dt = Math.min((now - lastTime) / 1000, 0.1)
    lastTime = now
    const count = popularity.data?.length || 0
    if (count > 0 && rankingTableRef.value) {
      const lapHeight = count * RANKING_ITEM_HEIGHT
      rankingScrollOffset.value += SCROLL_SPEED * dt
      if (rankingScrollOffset.value >= lapHeight) {
        rankingScrollOffset.value -= lapHeight
      }
      rankingTableRef.value.scrollTop = rankingScrollOffset.value
    }
    rankingScrollRafId = requestAnimationFrame(tick)
  }
  rankingScrollRafId = requestAnimationFrame(tick)
}

function stopRankingScroll() {
  if (rankingScrollRafId !== null) {
    cancelAnimationFrame(rankingScrollRafId)
    rankingScrollRafId = null
  }
  rankingScrollOffset.value = 0
  if (rankingTableRef.value) {
    rankingTableRef.value.scrollTop = 0
  }
}

// 粒子系统
const particleCanvas = ref<HTMLCanvasElement>()
let particleAnimId = 0

function initParticles() {
  const canvas = particleCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  const resize = () => { canvas.width = window.innerWidth; canvas.height = window.innerHeight }
  resize()
  window.addEventListener('resize', resize)
  interface P { x: number; y: number; vx: number; vy: number; size: number; alpha: number }
  const particles: P[] = []
  for (let i = 0; i < 60; i++) {
    particles.push({ x: Math.random() * canvas.width, y: Math.random() * canvas.height, vx: (Math.random() - 0.5) * 0.3, vy: (Math.random() - 0.5) * 0.3, size: Math.random() * 2 + 0.5, alpha: Math.random() * 0.5 + 0.2 })
  }
  function draw() {
    if (!ctx || !canvas) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    for (let i = 0; i < particles.length; i++) {
      const p = particles[i]
      p.x += p.vx; p.y += p.vy
      if (p.x < 0) p.x = canvas.width; if (p.x > canvas.width) p.x = 0
      if (p.y < 0) p.y = canvas.height; if (p.y > canvas.height) p.y = 0
      ctx.beginPath(); ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(96, 165, 250, ${p.alpha})`; ctx.fill()
      for (let j = i + 1; j < particles.length; j++) {
        const p2 = particles[j]
        const dx = p.x - p2.x, dy = p.y - p2.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 150) {
          ctx.beginPath(); ctx.moveTo(p.x, p.y); ctx.lineTo(p2.x, p2.y)
          ctx.strokeStyle = `rgba(96, 165, 250, ${0.1 * (1 - dist / 150)})`
          ctx.lineWidth = 0.5; ctx.stroke()
        }
      }
    }
    particleAnimId = requestAnimationFrame(draw)
  }
  draw()
  return () => { window.removeEventListener('resize', resize); cancelAnimationFrame(particleAnimId) }
}

// 快捷操作权限
const canCreatePackage = computed(() => authStore.hasPermission('package:create'))
const canApplySubscription = computed(() => authStore.hasPermission('subscription:create'))
const canViewBusinessSystem = computed(() => authStore.hasPermission('business-system:read'))
const canViewStorage = computed(() => authStore.hasPermission('storage:read'))
const canRegisterInventory = computed(() => authStore.hasPermission('inventory:create'))
const canViewMyInventory = computed(() => authStore.hasPermission('inventory:read'))

// 趋势图表配置（亮色 - 普通模式）
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

// 趋势图表配置（暗色 - 大屏模式）
const trendChartOptionDark = computed(() => {
  const dates = trendData.data?.map(d => d.date.slice(5)) || []
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(11, 26, 47, 0.9)',
      borderColor: 'rgba(0, 180, 255, 0.3)',
      textStyle: { color: '#ffffff' },
      axisPointer: { type: 'cross', lineStyle: { color: 'rgba(0, 180, 255, 0.3)' } }
    },
    legend: { data: ['总订购', '已审批', '待审批'], textStyle: { color: '#B0E0FF', fontSize: 11 }, bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '14%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category', data: dates,
      axisLine: { lineStyle: { color: 'rgba(0, 180, 255, 0.2)' } },
      axisLabel: { color: '#B0E0FF', fontSize: 11 }, axisTick: { show: false }
    },
    yAxis: {
      type: 'value', minInterval: 1, axisLine: { show: false },
      axisLabel: { color: '#B0E0FF', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(0, 180, 255, 0.08)' } }
    },
    series: [
      {
        name: '总订购', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        data: trendData.data?.map(d => d.subscriptionCount) || [],
        itemStyle: { color: '#00B4FF' }, lineStyle: { width: 2, color: '#00B4FF' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(0, 180, 255, 0.35)' }, { offset: 1, color: 'rgba(0, 180, 255, 0.01)' }] } }
      },
      {
        name: '已审批', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
        data: trendData.data?.map(d => d.approvedCount) || [],
        itemStyle: { color: '#00F0FF' }, lineStyle: { width: 2, color: '#00F0FF' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(0, 240, 255, 0.25)' }, { offset: 1, color: 'rgba(0, 240, 255, 0.01)' }] } }
      },
      {
        name: '待审批', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
        data: trendData.data?.map(d => d.pendingCount) || [],
        itemStyle: { color: '#FFD700' }, lineStyle: { width: 2, type: 'dashed', color: '#FFD700' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(255, 215, 0, 0.15)' }, { offset: 1, color: 'rgba(255, 215, 0, 0.01)' }] } }
      }
    ]
  }
})

// 类型分布图表配置（亮色 - 普通模式）
const PIE_COLORS = ['#3B6FF5', '#16A349', '#F59E0B', '#EF4444', '#8B5CF6', '#06B6D4', '#EC4899', '#F97316']
const PIE_COLORS_DARK = ['#00d4ff', '#00ff88', '#ffcc00', '#ff3366', '#b388ff', '#00e5ff', '#ff66b2', '#ff9933']

// 关联图暗色配色 - 高饱和荧光色，在深色背景上对比度强
const GRAPH_TYPE_COLORS: Record<string, string> = {
  'DOCKER_IMAGE': '#22D3EE',   // 亮青
  'HELM_CHART': '#34D399',     // 亮绿
  'MAVEN': '#FBBF24',          // 亮琥珀
  'NPM': '#F87171',            // 亮红
  'PYPI': '#A78BFA',           // 亮紫
  'GENERIC': '#CBD5E1'         // 亮灰白
}

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

// 类型分布图表配置（暗色 - 大屏模式）
const typeChartOptionDark = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(2, 4, 10, 0.9)',
    borderColor: 'rgba(0, 212, 255, 0.3)',
    textStyle: { color: '#e0e0e0' },
    formatter: '{b}: {c} ({d}%)'
  },
  legend: {
    orient: 'vertical',
    right: 10,
    top: 'center',
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { fontSize: 11, color: '#94a3b8' }
  },
  series: [{
    type: 'pie',
    radius: ['42%', '65%'],
    center: ['38%', '50%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 4, borderColor: 'rgba(2, 4, 10, 0.9)', borderWidth: 2 },
    label: { show: false },
    emphasis: {
      label: { show: true, fontSize: 13, fontWeight: 'bold', color: '#e0e0e0' },
      itemStyle: { shadowBlur: 24, shadowColor: 'rgba(0, 212, 255, 0.5)' }
    },
    data: typeDistribution.data?.map((item, i) => ({
      value: item.packageCount,
      name: item.typeName,
      itemStyle: { color: PIE_COLORS_DARK[i % PIE_COLORS_DARK.length] }
    })) || []
  }]
}))

// 大屏轮播卡片配置
const fullscreenStatsConfig = computed(() => [
  {
    icon: Box,
    label: '已发布软件包',
    value: overview.totalPackages || 0,
    trendClass: getTrendClass(overview.trends?.totalPackagesChange || 0),
    trendText: formatTrend(overview.trends?.totalPackagesChange || 0)
  },
  {
    icon: ShoppingCart,
    label: '总订购数',
    value: overview.totalSubscriptions || 0,
    trendClass: getTrendClass(overview.trends?.totalSubscriptionsChange || 0),
    trendText: formatTrend(overview.trends?.totalSubscriptionsChange || 0)
  },
  {
    icon: OfficeBuilding,
    label: '活跃业务系统',
    value: overview.activeBusinessSystems || 0,
    trendClass: getTrendClass(overview.trends?.activeBusinessSystemsChange || 0),
    trendText: formatTrend(overview.trends?.activeBusinessSystemsChange || 0)
  },
  {
    icon: Calendar,
    label: '本月新增订购',
    value: overview.newSubscriptionsThisMonth || 0,
    trendClass: getTrendClass(overview.trends?.newSubscriptionsThisMonthChange || 0),
    trendText: formatTrend(overview.trends?.newSubscriptionsThisMonthChange || 0)
  },
  {
    icon: Collection,
    label: '软件类型数',
    value: typeDistribution.data?.length || 0,
    trendClass: 'trend-stable',
    trendText: '-'
  },
  {
    icon: Star,
    label: '热门软件包',
    value: popularity.data?.length || 0,
    trendClass: 'trend-stable',
    trendText: '-'
  }
])

// 雷达图配置（大屏模式）
const radarChartOption = computed(() => {
  const indicator = [
    { name: '软件包数', max: Math.max((overview.totalPackages || 0) * 1.5, 20) },
    { name: '订购数', max: Math.max((overview.totalSubscriptions || 0) * 1.5, 20) },
    { name: '业务系统', max: Math.max((overview.activeBusinessSystems || 0) * 1.5, 20) },
    { name: '本月新增', max: Math.max((overview.newSubscriptionsThisMonth || 0) * 1.5, 10) },
    { name: '类型覆盖', max: Math.max((typeDistribution.data?.length || 0) * 1.5, 8) }
  ]
  const currentValues = [
    overview.totalPackages || 0,
    overview.totalSubscriptions || 0,
    overview.activeBusinessSystems || 0,
    overview.newSubscriptionsThisMonth || 0,
    typeDistribution.data?.length || 0
  ]
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: 'rgba(11, 26, 47, 0.9)', borderColor: 'rgba(0, 180, 255, 0.3)', textStyle: { color: '#fff' } },
    radar: {
      indicator,
      axisName: { color: '#B0E0FF', fontSize: 11 },
      splitArea: { areaStyle: { color: ['rgba(0, 180, 255, 0.02)', 'rgba(0, 180, 255, 0.05)'] } },
      axisLine: { lineStyle: { color: 'rgba(0, 180, 255, 0.2)' } },
      splitLine: { lineStyle: { color: 'rgba(0, 180, 255, 0.12)' } }
    },
    series: [{
      type: 'radar',
      data: [
        { value: currentValues, name: '当前数据', itemStyle: { color: '#00B4FF' }, lineStyle: { width: 2, color: '#00B4FF' }, areaStyle: { color: 'rgba(0, 180, 255, 0.25)' }, symbol: 'circle', symbolSize: 6 }
      ]
    }]
  }
})

// 关联图配置（大屏模式）
const graphChartOptionDark = computed(() => {
  const isShowAll = graphShowAll.value
  const nodeCount = graphData.nodes.length
  // 节点多时自动缩小尺寸、降低标签密度
  const baseSize = isShowAll ? 28 : 42
  const pkgSize = isShowAll ? 20 : 32
  const labelFontSize = isShowAll ? 8 : 11
  const showLabel = !isShowAll || nodeCount < 80

  const nodes = graphData.nodes.map((node: any) => ({
    ...node,
    symbol: node.type === 'system' ? 'circle' : 'roundRect',
    symbolSize: node.type === 'system'
      ? (node.systemId ? baseSize : baseSize - 4)
      : pkgSize,
    itemStyle: {
      color: node.type === 'system'
        ? '#60A5FA'
        : (GRAPH_TYPE_COLORS[node.softwareType] || '#CBD5E1'),
      borderColor: node.type === 'system' ? '#93C5FD' : 'transparent',
      borderWidth: node.type === 'system' ? 2 : 0,
      shadowBlur: node.type === 'system' ? 20 : 14,
      shadowColor: node.type === 'system'
        ? 'rgba(96, 165, 250, 0.6)'
        : 'rgba(203, 213, 225, 0.25)'
    },
    label: {
      show: showLabel,
      color: node.type === 'system' ? '#FFFFFF' : '#F8FAFC',
      fontSize: labelFontSize,
      fontWeight: node.type === 'system' ? 'bold' : 'normal',
      position: 'bottom',
      distance: 5,
      textBorderColor: 'rgba(2, 6, 23, 0.85)',
      textBorderWidth: 3
    }
  }))

  const edges = graphData.edges.map((edge: any) => ({
    ...edge,
    lineStyle: {
      color: edge.sourceType === 'INVENTORY'
        ? 'rgba(251, 191, 36, 0.7)'
        : 'rgba(148, 163, 184, 0.35)',
      width: edge.sourceType === 'INVENTORY' ? 2 : 1,
      curveness: 0.15
    }
  }))

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.95)',
      borderColor: 'rgba(96, 165, 250, 0.5)',
      borderWidth: 1,
      textStyle: { color: '#F1F5F9' },
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          const d = params.data
          return `<div style="font-weight:bold;margin-bottom:4px;color:#60A5FA">${d.name}</div>
            ${d.type === 'system' ? `<div>代码: ${d.systemCode}</div><div>域: ${d.domain || '-'}</div>` : `<div>类型: ${d.softwareType || '-'}</div>`}`
        }
        return `${params.data.source} → ${params.data.target}`
      }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      cursor: 'pointer',
      data: nodes,
      links: edges,
      roam: true,
      draggable: true,
      force: {
        // 节点多时增大斥力、缩短边长，防止重叠
        repulsion: isShowAll ? 800 : 400,
        edgeLength: isShowAll ? [30, 80] : [60, 120],
        gravity: isShowAll ? 0.08 : 0.15,
        layoutAnimation: true
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 3, color: '#60A5FA' },
        itemStyle: {
          shadowBlur: 30,
          shadowColor: 'rgba(96, 165, 250, 0.8)'
        }
      }
    }]
  }
})

// 柱状图配置（大屏模式 - 系统软件分布）
const barChartOptionDark = computed(() => {
  const data = businessDistribution.data || []
  const sorted = [...data].sort((a, b) => b.packageCount - a.packageCount).slice(0, 8)
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(11, 26, 47, 0.9)', borderColor: 'rgba(0, 180, 255, 0.3)', textStyle: { color: '#fff' }, axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(0, 180, 255, 0.05)' } } },
    grid: { left: '3%', right: '4%', bottom: '8%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: sorted.map(d => d.systemName?.slice(0, 4) || ''),
      axisLine: { lineStyle: { color: 'rgba(0, 180, 255, 0.15)' } },
      axisLabel: { color: '#B0E0FF', fontSize: 10 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: '#B0E0FF', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0, 180, 255, 0.06)' } }
    },
    series: [{
      type: 'bar',
      data: sorted.map(d => d.packageCount),
      itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#00F0FF' }, { offset: 1, color: '#00B4FF' }] }, borderRadius: [4, 4, 0, 0] },
      barWidth: '50%',
      emphasis: { itemStyle: { color: '#00F0FF', shadowBlur: 12, shadowColor: 'rgba(0, 240, 255, 0.5)' } }
    }]
  }
})

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
let particleCleanup: (() => void) | null = null

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  if (isFullscreen.value) {
    document.body.classList.add('fullscreen-body')
    startScroll()
    startRankingScroll()
    loadGraph()
    setTimeout(() => {
      particleCleanup = initParticles() || null
    }, 100)
  } else {
    document.body.classList.remove('fullscreen-body')
    stopScroll()
    stopRankingScroll()
    if (particleCleanup) {
      particleCleanup()
      particleCleanup = null
    }
    // 退出大屏时同时退出关联图独占模式
    isGraphFullscreen.value = false
    selectedGraphNode.value = null
    nodeDetailPackages.value = []
  }
}

function toggleGraphFullscreen() {
  isGraphFullscreen.value = !isGraphFullscreen.value
  if (!isGraphFullscreen.value) {
    selectedGraphNode.value = null
    nodeDetailPackages.value = []
  }
}

async function handleGraphClick(params: any) {
  if (params.dataType !== 'node') return
  selectedGraphNode.value = params.data
  nodeDetailPackages.value = []

  if (params.data.type === 'system' && params.data.systemId) {
    try {
      const deps = await trackingApi.getSystemDependencies(params.data.systemId)
      nodeDetailPackages.value = deps.packages || []
    } catch (e) {
      console.error('Failed to load node dependencies:', e)
    }
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

async function loadGraph() {
  try {
    const data = await trackingApi.getRelationshipGraph({ showAll: graphShowAll.value || undefined })
    Object.assign(graphData, data)
  } catch (e) {
    console.error('Failed to load graph:', e)
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
  stopRankingScroll()
  if (particleCleanup) { particleCleanup(); particleCleanup = null }
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

  // 大屏特效层
  .particle-layer {
    position: fixed;
    top: 0; left: 0;
    width: 100%; height: 100%;
    pointer-events: none;
    z-index: 0;
  }
  .scan-line {
    position: fixed;
    top: 0; left: 0;
    width: 100%; height: 2px;
    background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.6), transparent);
    box-shadow: 0 0 20px rgba(0, 212, 255, 0.4), 0 0 60px rgba(0, 212, 255, 0.1);
    z-index: 2;
    pointer-events: none;
    animation: scanMove 6s linear infinite;
  }

  // ===== Fullscreen Mode - New Design =====
  &.fullscreen-mode {
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    z-index: 9999;
    background: linear-gradient(180deg, #0B1A2F 0%, #132744 100%);
    padding: 12px 16px;
    overflow: hidden;
    color: #ffffff;
    display: flex;
    flex-direction: column;

    // 顶部扫描线特效
    &::before {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0;
      height: 1px;
      background: linear-gradient(90deg, transparent, rgba(0, 180, 255, 0.6), transparent);
      z-index: 10;
      pointer-events: none;
    }

    .fullscreen-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;
      position: relative;
      height: 48px;
      flex-shrink: 0;
      padding: 0 8px;

      .header-bg {
        position: absolute;
        top: 50%; left: 50%;
        transform: translate(-50%, -50%);
        width: 600px; height: 40px;
        background: radial-gradient(ellipse at center, rgba(0, 180, 255, 0.08) 0%, transparent 70%);
        pointer-events: none;
      }

      .header-title {
        font-size: 22px;
        font-weight: 800;
        color: #ffffff;
        letter-spacing: 3px;
        text-shadow: 0 0 20px rgba(0, 180, 255, 0.6), 0 0 40px rgba(0, 180, 255, 0.2);
      }
      .header-time {
        font-size: 13px;
        color: #00B4FF;
        font-family: 'Courier New', monospace;
        text-shadow: 0 0 10px rgba(0, 180, 255, 0.5);
        letter-spacing: 1px;
      }
    }

    // 轮播卡片
    .fullscreen-carousel {
      display: flex;
      gap: 12px;
      margin-bottom: 12px;
      flex-shrink: 0;
      overflow-x: auto;
      scrollbar-width: none;
      &::-webkit-scrollbar { display: none; }

      .carousel-card {
        flex: 1;
        min-width: 180px;
        background: rgba(0, 180, 255, 0.04);
        border: 1px solid rgba(0, 180, 255, 0.12);
        border-radius: 8px;
        padding: 14px 16px;
        display: flex;
        align-items: center;
        gap: 12px;
        position: relative;
        overflow: hidden;
        transition: all 0.3s ease;

        &.active {
          border-color: rgba(0, 180, 255, 0.4);
          box-shadow: 0 0 20px rgba(0, 180, 255, 0.1), inset 0 0 20px rgba(0, 180, 255, 0.03);
        }

        &::before {
          content: '';
          position: absolute;
          top: 0; left: 0; right: 0;
          height: 2px;
          background: linear-gradient(90deg, transparent, #00B4FF, transparent);
          opacity: 0.6;
        }

        .card-icon {
          width: 44px; height: 44px;
          display: flex; align-items: center; justify-content: center;
          background: rgba(0, 180, 255, 0.08);
          border-radius: 10px;
          border: 1px solid rgba(0, 180, 255, 0.15);
          color: #00B4FF;
          flex-shrink: 0;
        }
        .card-info {
          flex: 1;
          min-width: 0;
          .card-label {
            font-size: 11px;
            color: #B0E0FF;
            margin-bottom: 4px;
          }
          .card-value {
            font-size: 22px;
            font-weight: 800;
            color: #ffffff;
            line-height: 1;
            text-shadow: 0 0 12px rgba(0, 180, 255, 0.4);
          }
          .card-trend {
            font-size: 10px;
            margin-top: 4px;
            &.trend-up { color: #00ff88; }
            &.trend-down { color: #ff6b6b; }
            &.trend-stable { color: #8899aa; }
          }
        }
      }
    }

    // 主区域三栏布局
    .fullscreen-main {
      display: grid;
      grid-template-columns: 26% 1fr 26%;
      gap: 12px;
      flex: 1;
      min-height: 0;
      overflow: hidden;

      .fs-left, .fs-center, .fs-right {
        display: flex;
        flex-direction: column;
        gap: 12px;
        min-height: 0;
        overflow: hidden;
      }

      .fs-panel {
        background: rgba(0, 60, 120, 0.08);
        border: 1px solid rgba(0, 180, 255, 0.12);
        border-radius: 10px;
        position: relative;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        min-height: 0;

        &::before {
          content: '';
          position: absolute;
          top: 0; left: 0; right: 0;
          height: 2px;
          background: linear-gradient(90deg, transparent, #00B4FF, transparent);
          opacity: 0.5;
        }

        .panel-title {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 10px 14px;
          font-size: 14px;
          font-weight: 700;
          color: #ffffff;
          flex-shrink: 0;

          .title-icon {
            width: 4px; height: 14px;
            background: linear-gradient(180deg, #00B4FF, #00F0FF);
            border-radius: 2px;
            box-shadow: 0 0 8px rgba(0, 180, 255, 0.4);
          }
        }

        .fs-chart {
          flex: 1;
          min-height: 0;
          padding: 0 10px 10px;
        }
      }

      .trend-panel { flex: 1.2; }
      .table-panel { flex: 1; }
      .graph-panel { flex: 1; }
      .radar-panel { flex: 1; }
      .bar-panel { flex: 1.2; }

      // 表格样式
      .fs-table-wrap {
        flex: 1;
        display: flex;
        flex-direction: column;
        padding: 0 10px 10px;
        min-height: 0;
        overflow: hidden;

        .fs-table-head {
          flex-shrink: 0;
        }

        .fs-table-body-scroll {
          flex: 1;
          overflow-y: auto;
          scrollbar-width: none;
          &::-webkit-scrollbar { display: none; }
        }

        .fs-table {
          width: 100%;
          border-collapse: collapse;
          font-size: 12px;

          th {
            color: #00B4FF;
            font-weight: 600;
            text-align: left;
            padding: 8px 10px;
            border-bottom: 1px solid rgba(0, 180, 255, 0.15);
            font-size: 11px;
            text-shadow: 0 0 8px rgba(0, 180, 255, 0.3);
          }
          td {
            color: #ffffff;
            padding: 7px 10px;
            border-bottom: 1px solid rgba(0, 180, 255, 0.05);
          }
          tr:hover td {
            background: rgba(0, 180, 255, 0.04);
          }

          .fs-rank {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 20px; height: 20px;
            border-radius: 4px;
            font-size: 11px;
            font-weight: 700;
            color: #B0E0FF;
            background: rgba(0, 180, 255, 0.08);

            &.fs-rank-top {
              color: #00F0FF;
              background: rgba(0, 240, 255, 0.12);
              box-shadow: 0 0 8px rgba(0, 240, 255, 0.2);
            }
          }
          .fs-trend {
            font-size: 11px;
            display: inline-flex; align-items: center; gap: 2px;
            &.up { color: #00ff88; }
            &.down { color: #ff6b6b; }
            &.stable { color: #8899aa; }
          }
        }
      }

      // 中栏统计数字
      .globe-stats {
        display: flex;
        gap: 32px;
        justify-content: center;
        padding: 16px 0;
        flex-shrink: 0;

        .gstat {
          text-align: center;
          .gstat-value {
            font-size: 28px;
            font-weight: 800;
            color: #00F0FF;
            text-shadow: 0 0 16px rgba(0, 240, 255, 0.5);
          }
          .gstat-label {
            font-size: 12px;
            color: #B0E0FF;
            margin-top: 4px;
          }
        }
      }
    }

    .exit-fullscreen-btn {
      position: fixed;
      bottom: 12px;
      right: 16px;
      z-index: 10000;
      border-radius: 6px;
      padding: 8px 16px;
      background: rgba(0, 180, 255, 0.1);
      border: 1px solid rgba(0, 180, 255, 0.25);
      color: #00B4FF;
      backdrop-filter: blur(8px);
      font-size: 12px;
      transition: all 0.3s ease;

      &:hover {
        background: rgba(0, 180, 255, 0.2);
        box-shadow: 0 0 20px rgba(0, 180, 255, 0.2);
        color: #00F0FF;
      }
    }

    // 关联图独占全屏
    .fullscreen-main.graph-fullscreen {
      grid-template-columns: 1fr;
    }

    // 节点详情浮动面板
    .node-detail-float {
      position: absolute;
      top: 44px;
      right: 10px;
      width: 260px;
      background: rgba(11, 26, 47, 0.95);
      border: 1px solid rgba(0, 180, 255, 0.25);
      border-radius: 8px;
      padding: 14px;
      z-index: 100;
      backdrop-filter: blur(8px);
      max-height: calc(100% - 60px);
      overflow-y: auto;

      &::before {
        content: '';
        position: absolute;
        top: 0; left: 0; right: 0;
        height: 2px;
        background: linear-gradient(90deg, transparent, #00B4FF, transparent);
      }

      .detail-header {
        font-size: 14px;
        font-weight: 700;
        color: #00B4FF;
        margin-bottom: 10px;
        text-shadow: 0 0 8px rgba(0, 180, 255, 0.4);
      }

      .detail-row {
        display: flex;
        justify-content: space-between;
        padding: 6px 0;
        border-bottom: 1px solid rgba(0, 180, 255, 0.08);
        font-size: 12px;

        .detail-label {
          color: #B0E0FF;
        }
        .detail-value {
          color: #ffffff;
          font-weight: 500;
        }
      }

      .detail-packages {
        margin-top: 10px;
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .pkg-tag {
          padding: 3px 8px;
          background: rgba(0, 180, 255, 0.1);
          border: 1px solid rgba(0, 180, 255, 0.2);
          border-radius: 4px;
          font-size: 11px;
          color: #B0E0FF;
        }
      }
    }
  }
}

// Keep existing scan animation
@keyframes scanMove {
  0% { top: -2px; }
  100% { top: 100%; }
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
