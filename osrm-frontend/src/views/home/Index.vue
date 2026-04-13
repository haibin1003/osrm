<template>
  <div class="home-page">
    <div class="page-header">
      <h1 class="page-title">欢迎回来，{{ authStore.userInfo?.realName || '管理员' }}</h1>
      <p class="page-subtitle">开源软件仓库管理系统</p>
    </div>

    <!-- 统计卡片 - 4列 -->
    <div class="stats-grid">
      <div class="stat-card card-hover-lift">
        <div class="stat-icon" style="background: var(--color-primary-light); color: var(--color-primary);">
          <el-icon :size="28"><Box /></el-icon>
        </div>
        <div class="stat-value">{{ stats.totalPackages }}</div>
        <div class="stat-label">软件包总数</div>
      </div>
      <div class="stat-card card-hover-lift">
        <div class="stat-icon" style="background: var(--color-success-light); color: var(--color-success);">
          <el-icon :size="28"><Collection /></el-icon>
        </div>
        <div class="stat-value">{{ Object.keys(stats.totalByType).length }}</div>
        <div class="stat-label">软件类型</div>
      </div>
      <div class="stat-card card-hover-lift">
        <div class="stat-icon" style="background: var(--color-warning-light); color: var(--color-warning);">
          <el-icon :size="28"><DataLine /></el-icon>
        </div>
        <div class="stat-value">{{ overview.totalSubscriptions }}</div>
        <div class="stat-label">订购总数</div>
      </div>
      <div class="stat-card card-hover-lift">
        <div class="stat-icon" style="background: var(--color-info-light); color: var(--color-info);">
          <el-icon :size="28"><Star /></el-icon>
        </div>
        <div class="stat-value">{{ popular.length }}</div>
        <div class="stat-label">热门软件</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-header-title">软件类型分布</div>
        </template>
        <v-chart class="chart" :option="pieOption" autoresize />
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-header-title">近7天订购趋势</div>
        </template>
        <v-chart class="chart" :option="lineOption" autoresize />
      </el-card>
    </div>

    <!-- 最近发布 + 快捷操作 -->
    <div class="bottom-row">
      <el-card shadow="never" class="recent-card">
        <template #header>
          <div class="card-header-title">
            <span>最近发布</span>
            <el-button type="primary" link @click="$router.push('/software/packages')">查看全部</el-button>
          </div>
        </template>
        <el-table :data="recent" stripe>
          <el-table-column prop="packageName" label="软件名称" min-width="180" />
          <el-table-column prop="softwareTypeName" label="类型" width="100">
            <template #default="{ row }"><el-tag size="small" type="info">{{ row.softwareTypeName }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="statusName" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'" size="small">{{ row.statusName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
        </el-table>
      </el-card>

      <el-card shadow="never" class="quick-card">
        <template #header>
          <div class="card-header-title">
            <span>快捷操作</span>
            <el-badge v-if="overview.pendingCount > 0" :value="overview.pendingCount" type="danger" />
          </div>
        </template>
        <div class="quick-actions">
          <div v-if="canCreatePackage" class="action-item card-hover-lift" @click="$router.push('/software/packages/create')">
            <el-icon :size="36" color="var(--color-primary)"><Plus /></el-icon>
            <span>新增软件包</span>
          </div>
          <div v-if="canApplySubscription" class="action-item card-hover-lift" @click="$router.push('/subscription/apply')">
            <el-icon :size="36" color="var(--color-success)"><ShoppingCart /></el-icon>
            <span>申请订购</span>
          </div>
          <div v-if="canViewBusinessSystem" class="action-item card-hover-lift" @click="$router.push('/business/systems')">
            <el-icon :size="36" color="var(--color-warning)"><OfficeBuilding /></el-icon>
            <span>业务系统</span>
          </div>
          <div v-if="canViewStorage" class="action-item card-hover-lift" @click="$router.push('/system/storage')">
            <el-icon :size="36" color="var(--color-info)"><Files /></el-icon>
            <span>存储配置</span>
          </div>
          <div v-if="canRegisterInventory" class="action-item card-hover-lift" @click="$router.push('/inventory/create')">
            <el-icon :size="36" color="var(--color-warning)"><Document /></el-icon>
            <span>存量登记</span>
          </div>
          <div class="action-item card-hover-lift" @click="$router.push('/portal')">
            <el-icon :size="36" color="var(--color-primary)"><View /></el-icon>
            <span>软件门户</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/modules/auth'
import { Plus, ShoppingCart, OfficeBuilding, Files, View, Box, Collection, DataLine, Star, Document } from '@element-plus/icons-vue'
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

// Pie chart option
const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: '5%', left: 'center' },
  series: [{
    name: '软件类型',
    type: 'pie',
    radius: ['40%', '70%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
    label: { show: false, position: 'center' },
    emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
    data: [
      { value: stats.dockerCount || 0, name: 'Docker' },
      { value: stats.helmCount || 0, name: 'Helm' },
      { value: stats.mavenCount || 0, name: 'Maven' },
      { value: stats.npmCount || 0, name: 'NPM' },
      { value: stats.pypiCount || 0, name: 'PyPI' },
      { value: stats.genericCount || 0, name: 'Generic' }
    ].filter(item => item.value > 0)
  }]
}))

// Line chart option
const lineOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: trend.value.map(t => t.date.slice(5)) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    name: '订购数',
    type: 'line',
    smooth: true,
    data: trend.value.map(t => t.count),
    areaStyle: { color: 'rgba(99, 102, 241, 0.2)' },
    itemStyle: { color: '#6366f1' },
    lineStyle: { width: 3 }
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
    margin-bottom: var(--space-lg);
    .page-title { font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold); margin: 0; color: var(--color-text-primary); }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--color-text-secondary); margin: var(--space-xs) 0 0; }
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-md);
    margin-bottom: var(--space-lg);
  }

  .stat-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-md);
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .stat-icon {
      width: 40px;
      height: 40px;
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: var(--space-sm);
    }

    .stat-value { font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }
    .stat-label { font-size: var(--font-size-xs); color: var(--color-text-secondary); margin-top: var(--space-xs); }
  }

  .charts-row {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-md);
    margin-bottom: var(--space-lg);
  }

  .chart-card {
    .chart {
      height: 220px;
    }
  }

  .bottom-row {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: var(--space-md);
  }

  .recent-card {
    min-height: 280px;
  }

  .quick-card {
    min-height: 280px;
  }

  .quick-actions {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-sm);
  }

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-xs);
    padding: var(--space-sm);
    border: 1px solid var(--color-border-light);
    border-radius: var(--radius-md);
    cursor: pointer;
    background: var(--color-bg-card);

    span { font-size: var(--font-size-xs); color: var(--color-text-secondary); }
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
    .quick-actions { grid-template-columns: 1fr; }
  }
}
</style>
