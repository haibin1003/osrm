<template>
  <div class="tracking-page">
    <div class="page-header">
      <h1 class="page-title">系统-软件关联图</h1>
      <p class="page-subtitle">可视化展示业务系统与软件包的依赖关系网络</p>
    </div>

    <div class="tracking-container">
      <!-- 左侧过滤器 -->
      <div class="filter-panel">
        <div class="panel-section">
          <h3 class="panel-title">过滤器</h3>

          <div class="filter-group">
            <label>业务域</label>
            <el-select v-model="filters.domain" clearable placeholder="全部" @change="loadGraph">
              <el-option label="业务域" value="BUSINESS" />
              <el-option label="运营域" value="OPERATION" />
              <el-option label="资源域" value="RESOURCE" />
              <el-option label="服务域" value="SERVICE" />
              <el-option label="数据域" value="DATA" />
            </el-select>
          </div>

          <div class="filter-group">
            <label>软件类型</label>
            <el-select v-model="filters.softwareType" clearable placeholder="全部" @change="loadGraph">
              <el-option label="Docker镜像" value="DOCKER_IMAGE" />
              <el-option label="Helm Chart" value="HELM_CHART" />
              <el-option label="Maven依赖" value="MAVEN" />
              <el-option label="NPM包" value="NPM" />
              <el-option label="PyPI包" value="PYPI" />
              <el-option label="通用文件" value="GENERIC" />
            </el-select>
          </div>

          <div class="filter-group">
            <label>订阅状态</label>
            <el-select v-model="filters.status" clearable placeholder="全部" @change="loadGraph">
              <el-option label="已审批" value="APPROVED" />
              <el-option label="待审批" value="PENDING" />
              <el-option label="已拒绝" value="REJECTED" />
            </el-select>
          </div>

          <el-button type="primary" @click="resetFilters" style="width: 100%">
            <el-icon><Refresh /></el-icon> 重置过滤器
          </el-button>
        </div>

        <div class="panel-section">
          <h3 class="panel-title">图例</h3>
          <div class="legend-list">
            <div class="legend-item">
              <span class="legend-symbol system" :style="{ background: systemColor }"></span>
              <span class="legend-label">业务系统</span>
            </div>
            <div class="legend-item" v-for="(color, type) in typeColors" :key="type">
              <span class="legend-symbol package" :style="{ background: color }"></span>
              <span class="legend-label">{{ formatType(type) }}</span>
            </div>
          </div>
        </div>

        <div class="panel-section">
          <h3 class="panel-title">统计信息</h3>
          <div class="stats-list">
            <div class="stats-item">
              <span class="stats-label">业务系统</span>
              <span class="stats-value">{{ graphData.metadata?.totalSystems || 0 }}</span>
            </div>
            <div class="stats-item">
              <span class="stats-label">软件包</span>
              <span class="stats-value">{{ graphData.metadata?.totalPackages || 0 }}</span>
            </div>
            <div class="stats-item">
              <span class="stats-label">订阅关系</span>
              <span class="stats-value">{{ graphData.metadata?.totalSubscriptions || 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间图表区 -->
      <div class="graph-container">
        <div class="graph-toolbar">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="global">全局视图</el-radio-button>
            <el-radio-button label="system">系统视图</el-radio-button>
            <el-radio-button label="package">软件视图</el-radio-button>
          </el-radio-group>
          <el-button-group size="small">
            <el-button @click="zoomIn" title="放大">
              <el-icon><ZoomIn /></el-icon>
            </el-button>
            <el-button @click="zoomOut" title="缩小">
              <el-icon><ZoomOut /></el-icon>
            </el-button>
            <el-button @click="resetZoom" title="重置视图">
              <el-icon><FullScreen /></el-icon>
            </el-button>
            <el-button @click="toggleFullscreen" title="全屏">
              <el-icon><FullScreenIcon /></el-icon>
            </el-button>
            <el-button @click="refreshGraph" title="刷新">
              <el-icon><Refresh /></el-icon>
            </el-button>
            <el-button @click="toggleScatter" :type="isScattered ? 'warning' : 'default'" title="散开/聚拢">
              <el-icon><Operation /></el-icon>
            </el-button>
          </el-button-group>
        </div>

        <div class="graph-wrapper">
          <v-chart
            ref="chartRef"
            class="graph-chart"
            :option="chartOption"
            autoresize
            @click="handleChartClick"
            @dblclick="handleChartDblClick"
            @mouseup="handleChartMouseUp"
          />
        </div>

        <!-- 全屏模式详情浮层 -->
        <div class="detail-overlay" v-if="selectedNode && isFullscreen">
          <div class="overlay-header">
            <span class="overlay-title">{{ selectedNode.type === 'system' ? '业务系统详情' : '软件包详情' }}</span>
            <el-button text @click="selectedNode = null">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <div class="overlay-content">
            <div class="detail-item">
              <label>名称</label>
              <span>{{ selectedNode.name }}</span>
            </div>
            <template v-if="selectedNode.type === 'system'">
              <div class="detail-item">
                <label>系统代码</label>
                <span>{{ selectedNode.systemCode }}</span>
              </div>
              <div class="detail-item">
                <label>业务域</label>
                <el-tag size="small">{{ formatDomain(selectedNode.domain) }}</el-tag>
              </div>
            </template>
            <template v-else>
              <div class="detail-item">
                <label>唯一标识</label>
                <span>{{ selectedNode.packageKey }}</span>
              </div>
              <div class="detail-item">
                <label>软件类型</label>
                <el-tag :color="typeColors[selectedNode.softwareType]" size="small">{{ formatType(selectedNode.softwareType) }}</el-tag>
              </div>
            </template>
          </div>
          <div class="overlay-section" v-if="nodeDetails && nodeDetails.length > 0">
            <h4 class="overlay-section-title">{{ selectedNode.type === 'system' ? '依赖软件' : '使用系统' }}</h4>
            <div class="overlay-relation-list">
              <div v-for="item in nodeDetails" :key="item.id" class="overlay-relation-item">
                <span class="relation-name">{{ item.name }}</span>
                <el-tag size="small" v-if="item.version">v{{ item.version }}</el-tag>
              </div>
            </div>
          </div>
          <div class="overlay-actions">
            <el-button type="primary" size="small" @click="selectedNode.type === 'system' ? loadSystemDependencies() : loadPackageImpact()">
              {{ nodeDetails && nodeDetails.length > 0 ? '刷新' : '查看详情' }}
            </el-button>
          </div>
        </div>

        <div class="graph-hint">
          <el-tag size="small" type="info">
            <el-icon><InfoFilled /></el-icon>
            提示：单击节点查看详情，双击聚焦，拖拽调整布局，滚轮缩放，橙色边为存量数据
          </el-tag>
        </div>
      </div>

      <!-- 右侧详情面板 -->
      <div class="detail-panel" v-if="selectedNode && !isFullscreen">
        <div class="panel-section" v-if="selectedNode.type === 'system'">
          <h3 class="panel-title">业务系统详情</h3>
          <div class="detail-content">
            <div class="detail-item">
              <label>系统名称</label>
              <span>{{ selectedNode.name }}</span>
            </div>
            <div class="detail-item">
              <label>系统代码</label>
              <span>{{ selectedNode.systemCode }}</span>
            </div>
            <div class="detail-item">
              <label>业务域</label>
              <el-tag size="small">{{ formatDomain(selectedNode.domain) }}</el-tag>
            </div>
            <div class="detail-item">
              <label>状态</label>
              <el-tag :type="selectedNode.enabled ? 'success' : 'danger'" size="small">
                {{ selectedNode.enabled ? '启用' : '禁用' }}
              </el-tag>
            </div>
          </div>
          <el-button type="primary" @click="loadSystemDependencies" style="width: 100%; margin-top: 12px">
            查看依赖详情
          </el-button>
        </div>

        <div class="panel-section" v-if="selectedNode.type === 'package'">
          <h3 class="panel-title">软件包详情</h3>
          <div class="detail-content">
            <div class="detail-item">
              <label>软件名称</label>
              <span>{{ selectedNode.name }}</span>
            </div>
            <div class="detail-item">
              <label>唯一标识</label>
              <span>{{ selectedNode.packageKey }}</span>
            </div>
            <div class="detail-item">
              <label>软件类型</label>
              <el-tag :color="typeColors[selectedNode.softwareType]" size="small">
                {{ formatType(selectedNode.softwareType) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>发布状态</label>
              <el-tag :type="getStatusType(selectedNode.status)" size="small">
                {{ formatStatus(selectedNode.status) }}
              </el-tag>
            </div>
          </div>
          <el-button type="primary" @click="loadPackageImpact" style="width: 100%; margin-top: 12px">
            查看影响分析
          </el-button>
        </div>

        <div class="panel-section" v-if="nodeDetails">
          <h3 class="panel-title">
            {{ selectedNode.type === 'system' ? '依赖软件' : '使用系统' }}
          </h3>
          <div class="relation-list">
            <div
              v-for="item in nodeDetails"
              :key="item.id"
              class="relation-item"
              @click="focusNode(item.id)"
            >
              <div class="relation-name">{{ item.name }}</div>
              <div class="relation-meta">
                <el-tag size="small" v-if="item.version">v{{ item.version }}</el-tag>
                <el-tag size="small" :type="item.status === 'APPROVED' ? 'success' : 'warning'">
                  {{ formatStatus(item.status) }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="detail-panel empty" v-else>
        <el-empty description="点击图谱中的节点查看详情">
          <template #image>
            <el-icon :size="64" color="#909399"><Pointer /></el-icon>
          </template>
        </el-empty>
      </div>
    </div>

    <!-- 影响分析弹窗 -->
    <el-dialog
      v-model="impactDialogVisible"
      :title="impactData?.packageInfo?.name + ' - 影响分析'"
      width="600px"
    >
      <div v-if="impactData" class="impact-analysis">
        <div class="impact-summary">
          <div class="impact-card">
            <div class="impact-number">{{ impactData.statistics.totalSystems }}</div>
            <div class="impact-label">受影响系统</div>
          </div>
        </div>

        <div class="impact-section" v-if="Object.keys(impactData.statistics.byDomain).length > 0">
          <h4>按业务域分布</h4>
          <div class="distribution-bars">
            <div
              v-for="(count, domain) in impactData.statistics.byDomain"
              :key="domain"
              class="distribution-bar"
            >
              <span class="bar-label">{{ formatDomain(domain) }}</span>
              <el-progress :percentage="getPercentage(count, impactData.statistics.totalSystems)" :show-text="false" />
              <span class="bar-value">{{ count }}</span>
            </div>
          </div>
        </div>

        <div class="impact-section" v-if="Object.keys(impactData.statistics.byVersion).length > 0">
          <h4>按版本分布</h4>
          <div class="version-tags">
            <el-tag
              v-for="(count, version) in impactData.statistics.byVersion"
              :key="version"
              size="small"
              class="version-tag"
            >
              {{ version }}: {{ count }}个系统
            </el-tag>
          </div>
        </div>

        <div class="impact-section">
          <h4>受影响系统列表</h4>
          <el-table :data="impactData.affectedSystems" size="small" stripe>
            <el-table-column prop="systemName" label="系统名称" />
            <el-table-column prop="systemCode" label="系统代码" />
            <el-table-column prop="versionNumber" label="使用版本" width="100" />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { Refresh, FullScreen, InfoFilled, Pointer, ZoomIn, ZoomOut, FullScreen as FullScreenIcon, Close, Operation } from '@element-plus/icons-vue'
import { trackingApi, type RelationshipGraph, type SystemNode, type PackageNode, type GraphNode } from '@/api/tracking'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GraphChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, DataZoomComponent } from 'echarts/components'
import VChart from 'vue-echarts'

// Register ECharts components
use([CanvasRenderer, GraphChart, TitleComponent, TooltipComponent, LegendComponent, DataZoomComponent])

// Data
const graphData = reactive<RelationshipGraph>({
  nodes: [],
  edges: [],
  metadata: { totalSystems: 0, totalPackages: 0, totalSubscriptions: 0 }
})

const filters = reactive({
  domain: '',
  softwareType: '',
  status: ''
})

const viewMode = ref('global')
const selectedNode = ref<(SystemNode | PackageNode) | null>(null)
const nodeDetails = ref<Array<{ id: string; name: string; version?: string; status?: string }>>([])
const impactDialogVisible = ref(false)
const impactData = ref<any>(null)
const chartRef = ref<any>(null)
const isFullscreen = ref(false)
const isScattered = ref(false)

// Listen for fullscreen changes
function handleFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

// Type colors - Bright modern vibrant palette
const typeColors: Record<string, string> = {
  'DOCKER_IMAGE': '#00D9FF',  // Bright Cyan
  'HELM_CHART': '#00E676',   // Bright Mint Green
  'MAVEN': '#FF5252',        // Bright Coral Red
  'NPM': '#FFAB40',          // Bright Amber
  'PYPI': '#E040FB',         // Bright Magenta
  'GENERIC': '#40C4FF'       // Light Sky Blue
}

// System node color - Bright Violet
const systemColor = '#7C4DFF'

// Filter nodes and edges based on view mode
const filteredNodes = computed(() => {
  if (viewMode.value === 'system') {
    // Only show system nodes
    return graphData.nodes.filter(node => node.type === 'system')
  } else if (viewMode.value === 'package') {
    // Only show package nodes
    return graphData.nodes.filter(node => node.type === 'package')
  }
  // Global: show all
  return graphData.nodes
})

const filteredEdges = computed(() => {
  const nodeIds = new Set(filteredNodes.value.map(n => n.id))
  return graphData.edges.filter(edge =>
    nodeIds.has(edge.source as string) && nodeIds.has(edge.target as string)
  )
})

// Chart option
const chartOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: (params: any) => {
      if (params.dataType === 'node') {
        const data = params.data
        let html = `<div style="font-weight: bold; margin-bottom: 4px">${data.name}</div>`
        if (data.type === 'system') {
          html += `<div>代码: ${data.systemCode}</div>`
          html += `<div>域: ${formatDomain(data.domain)}</div>`
        } else {
          html += `<div>标识: ${data.packageKey}</div>`
          html += `<div>类型: ${formatType(data.softwareType)}</div>`
        }
        return html
      } else {
        return `${params.data.source} → ${params.data.target}`
      }
    }
  },
  dataZoom: [
    {
      type: 'inside',
      start: 0,
      end: 100,
      zoomLock: false
    }
  ],
  series: [{
    type: 'graph',
    layout: viewMode.value === 'package' ? 'circular' : 'force',
    data: filteredNodes.value.map(node => ({
      ...node,
      symbol: getNodeSymbol(node, viewMode.value),
      symbolSize: getNodeSize(node, viewMode.value),
      itemStyle: {
        color: node.type === 'system' ? systemColor : typeColors[(node as PackageNode).softwareType] || '#999'
      },
      label: {
        show: true,
        position: 'bottom',
        formatter: '{b}',
        fontSize: 11,
        fontWeight: 300
      }
    })),
    links: filteredEdges.value.map(edge => ({
      ...edge,
      lineStyle: {
        color: edge.sourceType === 'INVENTORY' ? '#f59e0b' : '#e0e0e0',
        curveness: 0.1
      },
      label: {
        show: false
      }
    })),
    roam: true,
    draggable: true,
    force: {
      repulsion: isScattered.value ? 2000 : (viewMode.value === 'system' ? 400 : 200),
      edgeLength: isScattered.value ? [200, 400] : [80, 150],
      gravity: 0.1,
      layoutAnimation: true
    },
    circular: {
      rotate: 'none'
    },
    emphasis: {
      focus: 'adjacency',
      lineStyle: { width: 4 }
    }
  }]
}))

// Helper functions
function formatType(type: string): string {
  const typeMap: Record<string, string> = {
    'DOCKER_IMAGE': 'Docker镜像',
    'HELM_CHART': 'Helm Chart',
    'MAVEN': 'Maven依赖',
    'NPM': 'NPM包',
    'PYPI': 'PyPI包',
    'GENERIC': '通用文件'
  }
  return typeMap[type] || type
}

function formatDomain(domain: string): string {
  const domainMap: Record<string, string> = {
    'BUSINESS': '业务域',
    'OPERATION': '运营域',
    'RESOURCE': '资源域',
    'SERVICE': '服务域',
    'DATA': '数据域'
  }
  return domainMap[domain] || domain
}

function formatStatus(status: string): string {
  const statusMap: Record<string, string> = {
    'APPROVED': '已审批',
    'PENDING': '待审批',
    'REJECTED': '已拒绝',
    'PUBLISHED': '已发布',
    'DRAFT': '草稿',
    'OFFLINE': '已下架'
  }
  return statusMap[status] || status
}

function getStatusType(status: string): string {
  const typeMap: Record<string, string> = {
    'APPROVED': 'success',
    'PENDING': 'warning',
    'REJECTED': 'danger',
    'PUBLISHED': 'success',
    'DRAFT': 'info',
    'OFFLINE': 'info'
  }
  return typeMap[status] || 'info'
}

function getPercentage(value: number, total: number): number {
  return total > 0 ? Math.round((value / total) * 100) : 0
}

// Node styling based on view mode
function getNodeSymbol(node: any, mode: string): string {
  if (node.type === 'system') {
    return mode === 'system' ? 'diamond' : 'circle'
  } else {
    return mode === 'package' ? 'roundRect' : 'rect'
  }
}

function getNodeSize(node: any, mode: string): number | [number, number] {
  if (node.type === 'system') {
    return mode === 'system' ? 60 : 50
  } else {
    return mode === 'package' ? [70, 45] : [55, 35]
  }
}

// Load data
async function loadGraph() {
  try {
    const data = await trackingApi.getRelationshipGraph({
      domain: filters.domain || undefined,
      softwareType: filters.softwareType || undefined,
      status: filters.status || undefined
    })
    Object.assign(graphData, data)
  } catch (e) {
    console.error('Failed to load graph:', e)
  }
}

async function loadSystemDependencies() {
  if (!selectedNode.value || selectedNode.value.type !== 'system') return

  try {
    const data = await trackingApi.getSystemDependencies(selectedNode.value.systemId)
    nodeDetails.value = data.packages.map(p => ({
      id: `package:${p.packageId}`,
      name: p.packageName,
      version: p.versionNumber,
      status: p.status
    }))
  } catch (e) {
    console.error('Failed to load system dependencies:', e)
  }
}

async function loadPackageImpact() {
  if (!selectedNode.value || selectedNode.value.type !== 'package') return

  try {
    const data = await trackingApi.getPackageImpact(selectedNode.value.packageId)
    impactData.value = data
    impactDialogVisible.value = true
  } catch (e) {
    console.error('Failed to load package impact:', e)
  }
}

// Chart interactions
function handleChartClick(params: any) {
  if (params.dataType === 'node') {
    selectedNode.value = params.data as SystemNode | PackageNode
    nodeDetails.value = []

    // Auto load related data
    if (selectedNode.value.type === 'system') {
      loadSystemDependencies()
    }
  }
}

function handleChartDblClick(params: any) {
  if (params.dataType === 'node') {
    focusNode(params.data.id)
  }
}

function handleChartMouseUp(params: any) {
  // Handle mouse up to capture click events on graph nodes
  // This is a fallback in case @click doesn't work
  if (params && params.data && params.data.type) {
    selectedNode.value = params.data as SystemNode | PackageNode
    nodeDetails.value = []
    if (params.data.type === 'system') {
      loadSystemDependencies()
    }
  }
}

function focusNode(nodeId: string) {
  // Highlight the node in chart
  const chart = chartRef.value?.chart
  if (chart) {
    chart.dispatchAction({
      type: 'focusNodeAdjacency',
      seriesIndex: 0,
      dataIndex: graphData.nodes.findIndex(n => n.id === nodeId)
    })
  }
}

function resetZoom() {
  const chart = chartRef.value?.chart
  if (chart) {
    chart.dispatchAction({ type: 'restore' })
  }
}

function zoomIn() {
  const chart = chartRef.value?.chart
  if (chart) {
    chart.dispatchAction({ type: 'zoom', scale: 1.2 })
  }
}

function zoomOut() {
  const chart = chartRef.value?.chart
  if (chart) {
    chart.dispatchAction({ type: 'zoom', scale: 0.8 })
  }
}

function toggleFullscreen() {
  const elem = document.querySelector('.graph-container') as HTMLElement
  if (!elem) return

  if (!document.fullscreenElement) {
    elem.requestFullscreen?.()
  } else {
    document.exitFullscreen?.()
  }
}

function refreshGraph() {
  loadGraph()
}

function toggleScatter() {
  isScattered.value = !isScattered.value
  // Trigger chart update by toggling viewMode briefly
  const currentMode = viewMode.value
  viewMode.value = ''
  nextTick(() => {
    viewMode.value = currentMode
  })
}

function resetFilters() {
  filters.domain = ''
  filters.softwareType = ''
  filters.status = ''
  loadGraph()
}

onMounted(() => {
  loadGraph()
  document.addEventListener('fullscreenchange', handleFullscreenChange)

  // Bind ECharts native click event via chart.on()
  nextTick(() => {
    const chartInstance = chartRef.value?.chart
    if (chartInstance) {
      // Get the actual ECharts instance
      const echarts = chartInstance._rawValue || chartInstance

      // Use ECharts' on() to bind click event to graph nodes
      if (echarts.on) {
        echarts.on('click', (params: any) => {
          if (params.dataType === 'node' && params.data) {
            selectedNode.value = params.data as SystemNode | PackageNode
            nodeDetails.value = []
            if (params.data.type === 'system') {
              loadSystemDependencies()
            }
          }
        })
      }
    }
  })
})
</script>

<style scoped lang="scss">
.tracking-page {
  .page-header {
    margin-bottom: var(--space-lg);

    .page-title {
      font-size: var(--font-size-2xl);
      font-weight: var(--font-weight-bold);
      margin: 0;
    }

    .page-subtitle {
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
      margin: var(--space-xs) 0 0;
    }
  }

  .tracking-container {
    display: grid;
    grid-template-columns: 260px 1fr 280px;
    gap: var(--space-md);
    height: calc(100vh - 200px);
    min-height: 600px;
  }

  .filter-panel {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-md);
    overflow-y: auto;

    .panel-section {
      margin-bottom: var(--space-lg);

      &:last-child {
        margin-bottom: 0;
      }
    }

    .panel-title {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
      margin: 0 0 var(--space-sm);
      padding-bottom: var(--space-xs);
      border-bottom: 1px solid var(--color-border-light);
    }

    .filter-group {
      margin-bottom: var(--space-sm);

      label {
        display: block;
        font-size: var(--font-size-xs);
        color: var(--color-text-secondary);
        margin-bottom: var(--space-xs);
      }

      .el-select {
        width: 100%;
      }
    }

    .legend-list {
      .legend-item {
        display: flex;
        align-items: center;
        gap: var(--space-sm);
        margin-bottom: var(--space-sm);

        .legend-symbol {
          width: 24px;
          height: 24px;
          border-radius: 6px;
          box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);

          &.system {
            border-radius: 50%;
            border: 3px solid #fff;
            box-shadow: 0 2px 8px rgba(103, 58, 183, 0.4);
          }

          &.package {
            border-radius: 6px;
            border: 2px solid rgba(255,255,255,0.8);
          }
        }

        .legend-label {
          font-size: var(--font-size-xs);
          color: var(--color-text-secondary);
          font-weight: 300;
        }
      }
    }

    .stats-list {
      .stats-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: var(--space-xs) 0;
        border-bottom: 1px solid var(--color-border-light);

        &:last-child {
          border-bottom: none;
        }

        .stats-label {
          font-size: var(--font-size-xs);
          color: var(--color-text-secondary);
        }

        .stats-value {
          font-size: var(--font-size-sm);
          font-weight: var(--font-weight-semibold);
          color: var(--color-primary);
        }
      }
    }
  }

  .graph-container {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .graph-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--space-sm) var(--space-md);
      border-bottom: 1px solid var(--color-border-light);
    }

    .graph-wrapper {
      flex: 1;
      position: relative;
      overflow: hidden;
    }

    .graph-chart {
      width: 100%;
      height: 100%;
    }

    .graph-hint {
      padding: var(--space-sm) var(--space-md);
      border-top: 1px solid var(--color-border-light);
      text-align: center;
    }

    // Fullscreen detail overlay
    .detail-overlay {
      position: absolute;
      right: 20px;
      top: 60px;
      width: 300px;
      max-height: calc(100vh - 120px);
      background: var(--color-bg-card);
      border-radius: var(--radius-xl);
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
      overflow: hidden;
      z-index: 10001;
      display: flex;
      flex-direction: column;
      animation: slideIn 0.3s ease;

      @keyframes slideIn {
        from {
          opacity: 0;
          transform: translateX(20px);
        }
        to {
          opacity: 1;
          transform: translateX(0);
        }
      }

      .overlay-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: var(--space-md) var(--space-lg);
        border-bottom: 1px solid var(--color-border-light);

        .overlay-title {
          font-size: var(--font-size-sm);
          font-weight: var(--font-weight-semibold);
          color: var(--color-text-primary);
        }
      }

      .overlay-content {
        padding: var(--space-md) var(--space-lg);
        overflow-y: auto;
        flex: 1;

        .detail-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: var(--space-xs) 0;
          border-bottom: 1px solid var(--color-border-light);

          &:last-child {
            border-bottom: none;
          }

          label {
            font-size: var(--font-size-xs);
            color: var(--color-text-secondary);
          }

          span {
            font-size: var(--font-size-xs);
            color: var(--color-text-primary);
            font-weight: var(--font-weight-medium);
          }
        }
      }

      .overlay-actions {
        padding: var(--space-md) var(--space-lg);
        border-top: 1px solid var(--color-border-light);
      }

      .overlay-section {
        padding: var(--space-md) var(--space-lg);
        border-top: 1px solid var(--color-border-light);

        .overlay-section-title {
          font-size: var(--font-size-xs);
          color: var(--color-text-secondary);
          margin-bottom: var(--space-sm);
        }

        .overlay-relation-list {
          max-height: 200px;
          overflow-y: auto;

          .overlay-relation-item {
            display: flex;
            align-items: center;
            gap: var(--space-sm);
            padding: var(--space-xs) 0;

            .relation-name {
              font-size: var(--font-size-xs);
              color: var(--color-text-primary);
            }
          }
        }
      }
    }
  }

  .detail-panel {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-md);
    overflow-y: auto;

    &.empty {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .panel-section {
      margin-bottom: var(--space-lg);

      &:last-child {
        margin-bottom: 0;
      }
    }

    .panel-title {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
      margin: 0 0 var(--space-sm);
      padding-bottom: var(--space-xs);
      border-bottom: 1px solid var(--color-border-light);
    }

    .detail-content {
      .detail-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: var(--space-xs) 0;
        border-bottom: 1px solid var(--color-border-light);

        &:last-child {
          border-bottom: none;
        }

        label {
          font-size: var(--font-size-xs);
          color: var(--color-text-secondary);
        }

        span {
          font-size: var(--font-size-xs);
          color: var(--color-text-primary);
        }
      }
    }

    .relation-list {
      .relation-item {
        padding: var(--space-sm);
        background: var(--color-bg-secondary);
        border-radius: var(--radius-md);
        margin-bottom: var(--space-sm);
        cursor: pointer;
        transition: background 0.2s;

        &:hover {
          background: var(--color-bg-hover);
        }

        .relation-name {
          font-size: var(--font-size-sm);
          font-weight: var(--font-weight-medium);
          color: var(--color-text-primary);
          margin-bottom: var(--space-xs);
        }

        .relation-meta {
          display: flex;
          gap: var(--space-xs);
        }
      }
    }
  }

  .impact-analysis {
    .impact-summary {
      display: flex;
      justify-content: center;
      margin-bottom: var(--space-lg);

      .impact-card {
        text-align: center;
        padding: var(--space-lg) var(--space-xl);
        background: var(--color-bg-secondary);
        border-radius: var(--radius-lg);

        .impact-number {
          font-size: var(--font-size-4xl);
          font-weight: var(--font-weight-bold);
          color: var(--color-primary);
        }

        .impact-label {
          font-size: var(--font-size-sm);
          color: var(--color-text-secondary);
          margin-top: var(--space-xs);
        }
      }
    }

    .impact-section {
      margin-bottom: var(--space-lg);

      h4 {
        font-size: var(--font-size-sm);
        font-weight: var(--font-weight-semibold);
        color: var(--color-text-primary);
        margin: 0 0 var(--space-sm);
      }

      .distribution-bars {
        .distribution-bar {
          display: flex;
          align-items: center;
          gap: var(--space-sm);
          margin-bottom: var(--space-sm);

          .bar-label {
            width: 80px;
            font-size: var(--font-size-xs);
            color: var(--color-text-secondary);
          }

          .el-progress {
            flex: 1;
          }

          .bar-value {
            width: 30px;
            text-align: right;
            font-size: var(--font-size-xs);
            color: var(--color-text-primary);
          }
        }
      }

      .version-tags {
        display: flex;
        flex-wrap: wrap;
        gap: var(--space-xs);

        .version-tag {
          margin-right: 0;
        }
      }
    }
  }
}

@media (max-width: 1200px) {
  .tracking-page {
    .tracking-container {
      grid-template-columns: 1fr;
      grid-template-rows: auto 1fr auto;

      .filter-panel {
        display: flex;
        gap: var(--space-md);
        overflow-x: auto;

        .panel-section {
          flex-shrink: 0;
          min-width: 200px;
          margin-bottom: 0;
        }
      }
    }
  }
}
</style>
