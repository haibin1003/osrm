<template>
  <div class="software-detail">
    <div class="page-header">
      <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
    </div>

    <div v-if="loading" style="padding: 40px"><el-skeleton :rows="8" animated /></div>

    <template v-else-if="pkg">
      <!-- Hero Section -->
      <div class="hero-section" :class="`type-${pkg.softwareType?.toLowerCase()}`">
        <div class="hero-content">
          <div class="hero-left">
            <div class="pkg-icon">{{ pkg.packageName?.charAt(0).toUpperCase() }}</div>
            <div class="pkg-info">
              <h1 class="name">{{ pkg.packageName }}</h1>
              <div class="tags">
                <el-tag size="small" :type="getTypeColor(pkg.softwareType)">{{ pkg.softwareTypeName }}</el-tag>
                <el-tag :type="pkg.status === 'PUBLISHED' ? 'success' : 'warning'" size="small">{{ pkg.statusName }}</el-tag>
                <el-tag v-if="pkg.currentVersion" size="small" type="info">v{{ pkg.currentVersion }}</el-tag>
              </div>
              <p v-if="pkg.description" class="desc">{{ pkg.description }}</p>
            </div>
          </div>
          <div class="hero-right">
            <div class="stats-row">
              <div class="stat-item">
                <span class="stat-value">{{ pkg.viewCount || 0 }}</span>
                <span class="stat-label">浏览</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ pkg.downloadCount || 0 }}</span>
                <span class="stat-label">下载</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ pkg.subscriptionCount || 0 }}</span>
                <span class="stat-label">订购</span>
              </div>
            </div>
            <el-button type="primary" size="large" @click="handleSubscribe" :disabled="pkg.status !== 'PUBLISHED'">
              <el-icon><ShoppingCart /></el-icon>订购此软件
            </el-button>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="charts-row" v-if="dependencies.nodes.length > 0 || securityReport">
        <!-- 依赖图谱 -->
        <el-card v-if="dependencies.nodes.length > 1" shadow="never" class="chart-card">
          <template #header><span class="section-title">依赖图谱</span></template>
          <v-chart class="chart" :option="graphOption" autoresize />
        </el-card>

        <!-- 安全报告 -->
        <el-card v-if="securityReport" shadow="never" class="chart-card security-card">
          <template #header><span class="section-title">安全报告</span></template>
          <div class="security-content">
            <v-chart class="gauge-chart" :option="gaugeOption" autoresize />
            <div class="security-stats">
              <div class="vuln-row">
                <div class="vuln-item critical">
                  <span class="count">{{ securityReport.criticalCount }}</span>
                  <span class="label">严重</span>
                </div>
                <div class="vuln-item high">
                  <span class="count">{{ securityReport.highCount }}</span>
                  <span class="label">高危</span>
                </div>
                <div class="vuln-item medium">
                  <span class="count">{{ securityReport.mediumCount }}</span>
                  <span class="label">中危</span>
                </div>
                <div class="vuln-item low">
                  <span class="count">{{ securityReport.lowCount }}</span>
                  <span class="label">低危</span>
                </div>
              </div>
              <div class="scan-time">
                扫描时间: {{ securityReport.scanTime }}
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 版本列表 -->
      <el-card shadow="never" style="margin-bottom: var(--space-lg);">
        <template #header><span class="section-title">版本列表</span></template>
        <el-table :data="versions" stripe>
          <el-table-column prop="versionNo" label="版本号" width="140">
            <template #default="{ row }">
              <el-tag v-if="row.isLatest" type="warning" size="small">最新</el-tag>
              <el-tag v-else size="small">{{ row.versionNo }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="releaseNotes" label="发布说明" min-width="250" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="发布时间" width="170" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleSubscribeVersion(row)">订购</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 下载命令 -->
      <el-card shadow="never">
        <template #header><span class="section-title">获取方式</span></template>
        <div class="download-commands">
          <div v-for="cmd in downloadCommands" :key="cmd.label" class="cmd-item">
            <span class="cmd-label">{{ cmd.label }}</span>
            <div class="cmd-code-wrapper">
              <code class="cmd-code">{{ cmd.command }}</code>
              <el-button size="small" link @click="copyCommand(cmd.command)">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </template>

    <el-empty v-else description="软件包不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ShoppingCart, CopyDocument } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/modules/auth'
import { portalApi } from '@/api/portal'
import { softwareApi } from '@/api/software'
import type { SoftwarePackage, SoftwareVersion } from '@/types/software'
import type { DependencyGraph, SecurityReport } from '@/api/portal'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GraphChart, GaugeChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'

// Register ECharts components
use([CanvasRenderer, GraphChart, GaugeChart, TitleComponent, TooltipComponent, LegendComponent])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(true)
const pkg = ref<(SoftwarePackage & { versions?: SoftwareVersion[] }) | null>(null)
const versions = ref<SoftwareVersion[]>([])
const dependencies = ref<DependencyGraph>({ nodes: [], links: [] })
const securityReport = ref<SecurityReport | null>(null)

const packageId = Number(route.params.id)

const typeColorMap: Record<string, string> = {
  'DOCKER_IMAGE': 'primary',
  'HELM_CHART': 'info',
  'MAVEN': 'warning',
  'NPM': 'danger',
  'PYPI': 'success',
  'GENERIC': 'info'
}

const getTypeColor = (type: string) => typeColorMap[type] || 'info'

const typeGradient: Record<string, string> = {
  'DOCKER_IMAGE': 'linear-gradient(135deg, #1e3a5f 0%, #0f766e 100%)',
  'HELM_CHART': 'linear-gradient(135deg, #1e3a5f 0%, #0ea5e9 100%)',
  'MAVEN': 'linear-gradient(135deg, #3f2c22 0%, #c2410c 100%)',
  'NPM': 'linear-gradient(135deg, #3f2c22 0%, #dc2626 100%)',
  'PYPI': 'linear-gradient(135deg, #1e3a5f 0%, #16a34a 100%)',
  'GENERIC': 'linear-gradient(135deg, #1a1a2e 0%, #4b5563 100%)'
}

const downloadCommands = computed(() => {
  if (!pkg.value) return []
  const cmds: { label: string; command: string }[] = []
  switch (pkg.value.softwareType) {
    case 'DOCKER_IMAGE':
      cmds.push({ label: 'Docker Pull', command: `docker pull registry.example.com/${pkg.value.packageName}:${pkg.value.currentVersion || 'latest'}` })
      break
    case 'HELM_CHART':
      cmds.push({ label: 'Helm Pull', command: `helm pull ${pkg.value.packageName} --version ${pkg.value.currentVersion || 'latest'}` })
      break
    case 'MAVEN':
      cmds.push({ label: 'Maven (pom.xml)', command: `<dependency>\n  <groupId>com.example</groupId>\n  <artifactId>${pkg.value.packageName}</artifactId>\n  <version>${pkg.value.currentVersion || 'latest'}</version>\n</dependency>` })
      break
    case 'NPM':
      cmds.push({ label: 'NPM Install', command: `npm install ${pkg.value.packageName}@${pkg.value.currentVersion || 'latest'}` })
      break
    case 'PYPI':
      cmds.push({ label: 'Pip Install', command: `pip install ${pkg.value.packageName}==${pkg.value.currentVersion || 'latest'}` })
      break
    default:
      cmds.push({ label: '下载链接', command: `https://registry.example.com/download/${pkg.value.packageName}/${pkg.value.currentVersion || 'latest'}` })
  }
  return cmds
})

// Graph chart option
const graphOption = computed(() => ({
  tooltip: {},
  series: [{
    type: 'graph',
    layout: 'force',
    data: dependencies.value.nodes.map((n: any) => ({ ...n, symbolSize: n.id.startsWith('pkg-') ? 50 : 35 })),
    links: dependencies.value.links,
    roam: true,
    label: { show: true, position: 'inside', fontSize: 10 },
    force: { repulsion: 100, edgeLength: 100 },
    lineStyle: { color: 'source', curveness: 0.3 }
  }]
}))

// Gauge chart option
const gaugeOption = computed(() => {
  const score = securityReport.value?.score || 0
  const status = securityReport.value?.status || 'SAFE'
  const colors: Record<string, string> = {
    'SAFE': '#22c55e',
    'WARNING': '#f59e0b',
    'DANGER': '#ef4444'
  }
  return {
    series: [{
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: 0,
      max: 100,
      splitNumber: 5,
      radius: '90%',
      axisLine: {
        lineStyle: {
          width: 10,
          color: [
            [0.6, '#ef4444'],
            [0.85, '#f59e0b'],
            [1, '#22c55e']
          ]
        }
      },
      pointer: { length: '50%', width: 6, itemStyle: { color: 'auto' } },
      axisTick: { distance: -10, length: 6, lineStyle: { color: '#fff', width: 1 } },
      splitLine: { distance: -10, length: 15, lineStyle: { color: '#fff', width: 2 } },
      axisLabel: { color: 'inherit', distance: 20, fontSize: 12 },
      detail: {
        valueAnimation: true,
        formatter: '{value}分\n' + (status === 'SAFE' ? '安全' : status === 'WARNING' ? '警告' : '危险'),
        color: colors[status] || '#22c55e',
        fontSize: 16,
        offsetCenter: [0, '30%']
      },
      data: [{ value: score }]
    }]
  }
})

const copyCommand = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch { ElMessage.error('复制失败') }
}

const requireAuth = (target: string) => {
  if (!authStore.isAuthenticated) {
    ElMessageBox.confirm('订购软件需要先登录', '请先登录', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info'
    }).then(() => {
      router.push({ path: '/login', query: { redirect: target } })
    }).catch(() => {})
    return false
  }
  return true
}

const handleSubscribe = () => {
  const target = `/subscription/apply?packageId=${packageId}`
  if (requireAuth(target)) router.push(target)
}

const handleSubscribeVersion = (ver: SoftwareVersion) => {
  const target = `/subscription/apply?packageId=${packageId}&versionId=${ver.id}`
  if (requireAuth(target)) router.push(target)
}

onMounted(async () => {
  try {
    const [detail, verList, deps, sec] = await Promise.all([
      portalApi.getSoftwareDetail(packageId),
      portalApi.getVersions(packageId).catch(() => []),
      portalApi.getDependencies(packageId).catch(() => ({ nodes: [], links: [] })),
      portalApi.getSecurityReport(packageId).catch(() => null)
    ])
    pkg.value = detail
    versions.value = verList
    dependencies.value = deps
    securityReport.value = sec
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally { loading.value = false }
})
</script>

<style scoped lang="scss">
.software-detail {
  .page-header { margin-bottom: var(--space-lg); }

  .hero-section {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    border-radius: var(--radius-xl);
    padding: var(--space-3xl);
    margin-bottom: var(--space-2xl);
    color: white;

    &.type-docker_image { background: linear-gradient(135deg, #1e3a5f 0%, #0f766e 100%); }
    &.type-helm_chart { background: linear-gradient(135deg, #1e3a5f 0%, #0ea5e9 100%); }
    &.type-maven { background: linear-gradient(135deg, #3f2c22 0%, #c2410c 100%); }
    &.type-npm { background: linear-gradient(135deg, #3f2c22 0%, #dc2626 100%); }
    &.type-pypi { background: linear-gradient(135deg, #1e3a5f 0%, #16a34a 100%); }

    .hero-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: var(--space-2xl);

      .hero-left {
        display: flex;
        gap: var(--space-xl);
        align-items: flex-start;
      }

      .hero-right {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: var(--space-lg);
      }
    }

    .pkg-icon {
      width: 80px;
      height: 80px;
      background: rgba(255,255,255,0.2);
      border-radius: var(--radius-xl);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: var(--font-size-4xl);
      font-weight: var(--font-weight-bold);
      backdrop-filter: blur(10px);
    }

    .name { font-size: var(--font-size-3xl); font-weight: var(--font-weight-bold); margin: 0 0 var(--space-md); }
    .tags { display: flex; gap: var(--space-sm); margin-bottom: var(--space-md); }
    .desc { font-size: var(--font-size-md); opacity: 0.9; margin: 0; max-width: 500px; line-height: 1.5; }

    .stats-row {
      display: flex;
      gap: var(--space-xl);

      .stat-item {
        text-align: center;

        .stat-value {
          font-size: var(--font-size-2xl);
          font-weight: var(--font-weight-bold);
          display: block;
        }
        .stat-label {
          font-size: var(--font-size-xs);
          opacity: 0.8;
        }
      }
    }
  }

  .charts-row {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-xl);
    margin-bottom: var(--space-2xl);

    .chart-card {
      .chart {
        height: 300px;
      }
    }

    .security-card {
      .security-content {
        display: flex;
        gap: var(--space-xl);
        align-items: center;

        .gauge-chart {
          width: 200px;
          height: 200px;
        }

        .security-stats {
          flex: 1;

          .vuln-row {
            display: flex;
            gap: var(--space-md);
            margin-bottom: var(--space-lg);

            .vuln-item {
              text-align: center;
              padding: var(--space-md);
              border-radius: var(--radius-lg);
              min-width: 60px;

              .count {
                display: block;
                font-size: var(--font-size-2xl);
                font-weight: var(--font-weight-bold);
              }
              .label {
                font-size: var(--font-size-xs);
              }

              &.critical { background: #fef2f2; color: #dc2626; }
              &.high { background: #fff7ed; color: #c2410c; }
              &.medium { background: #fefce8; color: #a16207; }
              &.low { background: #f0fdf4; color: #16a34a; }
            }
          }

          .scan-time {
            font-size: var(--font-size-sm);
            color: var(--color-text-secondary);
          }
        }
      }
    }
  }

  .download-commands {
    background: #0f172a;
    border-radius: var(--radius-lg);
    padding: var(--space-lg);

    .cmd-item {
      display: flex;
      align-items: flex-start;
      gap: var(--space-md);
      padding: var(--space-md) 0;
      border-bottom: 1px solid rgba(255,255,255,0.1);

      &:last-child { border-bottom: none; }

      .cmd-label {
        font-weight: var(--font-weight-medium);
        width: 100px;
        flex-shrink: 0;
        color: #94a3b8;
      }

      .cmd-code-wrapper {
        flex: 1;
        display: flex;
        align-items: flex-start;
        gap: var(--space-sm);

        .cmd-code {
          flex: 1;
          background: #1e293b;
          padding: var(--space-sm) var(--space-md);
          border-radius: var(--radius-sm);
          font-size: var(--font-size-sm);
          font-family: var(--font-mono);
          color: #e2e8f0;
          word-break: break-all;
          white-space: pre-wrap;
        }

        .el-button {
          color: #94a3b8;

          &:hover {
            color: #fff;
          }
        }
      }
    }
  }
}

@media (max-width: 992px) {
  .software-detail {
    .hero-section .hero-content {
      flex-direction: column;
      align-items: flex-start;

      .hero-right {
        align-items: flex-start;
      }
    }

    .charts-row {
      grid-template-columns: 1fr;
    }
  }
}
</style>
