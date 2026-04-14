<template>
  <div class="home-page">
    <div class="page-header">
      <h1 class="page-title">欢迎回来，{{ authStore.userInfo?.realName || '管理员' }}</h1>
      <p class="page-subtitle">开源软件仓库管理系统</p>
    </div>

    <!-- 统计卡片 - 4列 (Stripe style with gradient top) -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, rgba(99, 91, 255, 0.1), rgba(162, 89, 255, 0.1));">
          <el-icon :size="24" color="#635bff"><Box /></el-icon>
        </div>
        <div class="stat-value">{{ stats.totalPackages }}</div>
        <div class="stat-label">软件包总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(36, 180, 126, 0.1);">
          <el-icon :size="24" color="#24b47e"><Collection /></el-icon>
        </div>
        <div class="stat-value">{{ Object.keys(stats.totalByType).length }}</div>
        <div class="stat-label">软件类型</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(217, 119, 6, 0.1);">
          <el-icon :size="24" color="#d97706"><DataLine /></el-icon>
        </div>
        <div class="stat-value">{{ overview.totalSubscriptions }}</div>
        <div class="stat-label">订购总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, rgba(99, 91, 255, 0.1), rgba(162, 89, 255, 0.1));">
          <el-icon :size="24" color="#635bff"><Star /></el-icon>
        </div>
        <div class="stat-value">{{ popular.length }}</div>
        <div class="stat-label">热门软件</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <div class="chart-card stripe-card">
        <div class="chart-header">
          <span class="chart-title">软件类型分布</span>
        </div>
        <v-chart class="chart" :option="pieOption" autoresize />
      </div>

      <div class="chart-card stripe-card">
        <div class="chart-header">
          <span class="chart-title">近7天订购趋势</span>
        </div>
        <v-chart class="chart" :option="lineOption" autoresize />
      </div>
    </div>

    <!-- 最近发布 + 快捷操作 -->
    <div class="bottom-row">
      <div class="recent-card stripe-card">
        <div class="card-header">
          <span class="card-title">最近发布</span>
          <el-button type="primary" link @click="$router.push('/software/packages')">查看全部</el-button>
        </div>
        <el-table :data="recent">
          <el-table-column prop="packageName" label="软件名称" min-width="180" />
          <el-table-column prop="softwareTypeName" label="类型" width="100">
            <template #default="{ row }"><el-tag size="small">{{ row.softwareTypeName }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="statusName" label="状态" width="90" align="center">
            <template #default="{ row }">
              <span class="status-badge" :class="row.status === 'PUBLISHED' ? 'success' : 'warning'">
                {{ row.statusName }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
        </el-table>
      </div>

      <div class="quick-card stripe-card">
        <div class="card-header">
          <span class="card-title">快捷操作</span>
          <el-badge v-if="overview.pendingCount > 0" :value="overview.pendingCount" type="danger" />
        </div>
        <div class="quick-actions">
          <div v-if="canCreatePackage" class="action-item" @click="$router.push('/software/packages/create')">
            <div class="action-icon" style="background: linear-gradient(135deg, #635bff, #a259ff);">
              <el-icon :size="20" color="#fff"><Plus /></el-icon>
            </div>
            <span>新增软件包</span>
          </div>
          <div v-if="canApplySubscription" class="action-item" @click="$router.push('/subscription/apply')">
            <div class="action-icon" style="background: rgba(36, 180, 126, 0.15);">
              <el-icon :size="20" color="#24b47e"><ShoppingCart /></el-icon>
            </div>
            <span>申请订购</span>
          </div>
          <div v-if="canViewBusinessSystem" class="action-item" @click="$router.push('/business/systems')">
            <div class="action-icon" style="background: rgba(217, 119, 6, 0.15);">
              <el-icon :size="20" color="#d97706"><OfficeBuilding /></el-icon>
            </div>
            <span>业务系统</span>
          </div>
          <div v-if="canViewStorage" class="action-item" @click="$router.push('/system/storage')">
            <div class="action-icon" style="background: rgba(99, 91, 255, 0.15);">
              <el-icon :size="20" color="#635bff"><Files /></el-icon>
            </div>
            <span>存储配置</span>
          </div>
          <div v-if="canRegisterInventory" class="action-item" @click="$router.push('/inventory/create')">
            <div class="action-icon" style="background: rgba(217, 119, 6, 0.15);">
              <el-icon :size="20" color="#d97706"><Document /></el-icon>
            </div>
            <span>存量登记</span>
          </div>
          <div v-if="canViewMyInventory" class="action-item" @click="$router.push('/inventory/my')">
            <div class="action-icon" style="background: rgba(36, 180, 126, 0.15);">
              <el-icon :size="20" color="#24b47e"><List /></el-icon>
            </div>
            <span>我的存量</span>
          </div>
          <div class="action-item" @click="$router.push('/portal')">
            <div class="action-icon" style="background: linear-gradient(135deg, #635bff, #a259ff);">
              <el-icon :size="20" color="#fff"><View /></el-icon>
            </div>
            <span>软件门户</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/modules/auth'
import { Plus, ShoppingCart, OfficeBuilding, Files, View, Box, Collection, DataLine, Star, Document, List } from '@element-plus/icons-vue'
import { portalApi } from '@/api/portal'
import type { SoftwarePackage } from '@/types/software'
import type { PortalStats, StatsTrendItem } from '@/api/portal'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'

// Register ECharts components
use([CanvasRenderer, PieChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const authStore = useAuthStore()
const recent = ref<SoftwarePackage[]>([])
const popular = ref<SoftwarePackage[]>([])
const trend = ref<StatsTrendItem[]>([])
const stats = reactive<PortalStats>({ totalPackages: 0, totalByType: {} })
const overview = reactive({ totalPackages: 0, publishedCount: 0, pendingCount: 0, draftCount: 0, totalSubscriptions: 0 })

// 快捷操作权限
const canCreatePackage = computed(() => authStore.hasPermission('package:create'))
const canApplySubscription = computed(() => authStore.hasPermission('subscription:create'))
const canViewBusinessSystem = computed(() => authStore.hasPermission('business-system:read'))
const canViewStorage = computed(() => authStore.hasPermission('storage:read'))
const canRegisterInventory = computed(() => authStore.hasPermission('inventory:create'))
const canViewMyInventory = computed(() => authStore.hasPermission('inventory:read'))

// Pie chart option (Stripe style)
const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: '5%', left: 'center', textStyle: { color: '#525f7f' } },
  series: [{
    name: '软件类型',
    type: 'pie',
    radius: ['45%', '70%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { show: false, position: 'center' },
    emphasis: { label: { show: true, fontSize: 14, fontWeight: '500' } },
    data: [
      { value: stats.dockerCount || 0, name: 'Docker', itemStyle: { color: '#635bff' } },
      { value: stats.helmCount || 0, name: 'Helm', itemStyle: { color: '#24b47e' } },
      { value: stats.mavenCount || 0, name: 'Maven', itemStyle: { color: '#d97706' } },
      { value: stats.npmCount || 0, name: 'NPM', itemStyle: { color: '#a259ff' } },
      { value: stats.pypiCount || 0, name: 'PyPI', itemStyle: { color: '#e25950' } },
      { value: stats.genericCount || 0, name: 'Generic', itemStyle: { color: '#3b82f6' } }
    ].filter(item => item.value > 0)
  }]
}))

// Line chart option (Stripe style)
const lineOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: trend.value.map(t => t.date.slice(5)), axisLine: { lineStyle: { color: '#e6ebf1' } }, axisLabel: { color: '#525f7f' } },
  yAxis: { type: 'value', minInterval: 1, axisLine: { show: false }, splitLine: { lineStyle: { color: '#f6f9fc' } }, axisLabel: { color: '#525f7f' } },
  series: [{
    name: '订购数',
    type: 'line',
    smooth: true,
    data: trend.value.map(t => t.count),
    areaStyle: { color: 'rgba(99, 91, 255, 0.08)' },
    itemStyle: { color: '#635bff' },
    lineStyle: { width: 2 }
  }]
}))

onMounted(async () => {
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
})
</script>

<style scoped lang="scss">
.home-page {
  .page-header {
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

  // Stats Grid (Stripe style with gradient top bar)
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-lg);
    margin-bottom: var(--space-xl);
  }

  .stat-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-lg);
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

    .stat-icon {
      width: 40px;
      height: 40px;
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: var(--space-md);
    }

    .stat-value {
      font-size: 28px;
      font-weight: var(--font-weight-light);
      color: var(--color-text-primary);
      letter-spacing: -0.5px;
    }
    .stat-label {
      font-size: var(--font-size-xs);
      color: var(--color-text-tertiary);
      margin-top: var(--space-xs);
      font-weight: var(--font-weight-normal);
    }
  }

  // Charts Row
  .charts-row {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
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
      padding: var(--space-lg) var(--space-lg) var(--space-md);
      border-bottom: 1px solid var(--color-border-light);
    }

    .chart-title {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-medium);
      color: var(--color-text-primary);
    }

    .chart {
      height: 220px;
      padding: var(--space-md);
    }
  }

  // Bottom Row
  .bottom-row {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: var(--space-lg);
  }

  .recent-card,
  .quick-card {
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

  // Quick Actions
  .quick-actions {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-sm);
    padding: var(--space-md);
  }

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-sm);
    padding: var(--space-md);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--transition-fast);

    &:hover {
      background: var(--color-bg-page);
    }

    span {
      font-size: var(--font-size-xs);
      color: var(--color-text-secondary);
      text-align: center;
      font-weight: var(--font-weight-normal);
    }
  }

  .action-icon {
    width: 36px;
    height: 36px;
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

@media (max-width: 1200px) {
  .home-page {
    .stats-grid { grid-template-columns: repeat(2, 1fr); }
    .charts-row { grid-template-columns: 1fr; }
    .bottom-row { grid-template-columns: 1fr; }
  }
}

@media (max-width: 768px) {
  .home-page {
    .stats-grid { grid-template-columns: 1fr; }
    .quick-actions { grid-template-columns: repeat(2, 1fr); }
  }
}
</style>
