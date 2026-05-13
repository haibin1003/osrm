<template>
  <div class="fullscreen-graph-page">
    <canvas ref="particleCanvas" class="particle-layer"></canvas>
    <div class="scan-line"></div>

    <div class="fg-header">
      <div class="fg-title">系统与软件关联图</div>
      <div class="fg-stats">
        <div class="fg-stat">
          <span class="fg-stat-value">{{ graphData.metadata?.totalSystems || 0 }}</span>
          <span class="fg-stat-label">系统总数</span>
        </div>
        <div class="fg-stat">
          <span class="fg-stat-value">{{ graphData.metadata?.totalPackages || 0 }}</span>
          <span class="fg-stat-label">软件总数</span>
        </div>
      </div>
    </div>

    <div class="fg-graph-wrap">
      <v-chart ref="graphChartRef" class="fg-chart" :option="graphChartOption" autoresize @click="handleGraphClick" />

      <div class="fg-controls">
        <el-switch
          v-model="graphShowAll"
          size="small"
          active-text="全部"
          inactive-text="关联"
          @change="loadGraph"
          style="--el-switch-on-color: #00B4FF; --el-switch-off-color: #4a5568"
        />
      </div>

      <!-- 节点详情浮动面板 -->
      <div class="node-detail-float" v-if="selectedGraphNode">
        <div class="detail-header">
          <span>{{ selectedGraphNode.name }}</span>
          <el-button type="info" link size="small" @click="selectedGraphNode = null" style="color: #8899aa;">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="detail-body">
          <div class="detail-row">
            <span class="detail-label">节点类型</span>
            <span class="detail-value">{{ selectedGraphNode.type === 'system' ? '业务系统' : '软件包' }}</span>
          </div>
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
          <div class="detail-row" v-if="selectedGraphNode.status">
            <span class="detail-label">状态</span>
            <span class="detail-value">{{ selectedGraphNode.status }}</span>
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

    <el-button class="fg-back-btn" type="primary" @click="$router.push('/home')">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ArrowLeft, Close } from '@element-plus/icons-vue'
import { trackingApi } from '@/api/tracking'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GraphChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, GraphChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const graphShowAll = ref(false)
const selectedGraphNode = ref<any>(null)
const nodeDetailPackages = ref<any[]>([])
const graphChartRef = ref<any>(null)

const graphData = ref<{
  nodes: any[]
  edges: any[]
  metadata: { totalSystems: number; totalPackages: number; totalSubscriptions: number }
}>({
  nodes: [],
  edges: [],
  metadata: { totalSystems: 0, totalPackages: 0, totalSubscriptions: 0 }
})

// 关联图配色 - 高饱和荧光色
const GRAPH_TYPE_COLORS: Record<string, string> = {
  'DOCKER_IMAGE': '#22D3EE',
  'HELM_CHART': '#34D399',
  'MAVEN': '#FBBF24',
  'NPM': '#F87171',
  'PYPI': '#A78BFA',
  'GENERIC': '#CBD5E1'
}

async function loadGraph() {
  try {
    const data = await trackingApi.getRelationshipGraph({ showAll: graphShowAll.value || undefined })
    graphData.value = data
  } catch (e) {
    console.error('Failed to load graph:', e)
  }
}

function formatType(type: string): string {
  const typeMap: Record<string, string> = {
    'DOCKER_IMAGE': 'Docker',
    'HELM_CHART': 'Helm',
    'MAVEN': 'Maven',
    'NPM': 'NPM',
    'PYPI': 'PyPI',
    'GENERIC': '通用',
    'MIDDLEWARE': '中间件'
  }
  return typeMap[type] || type
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

const graphChartOption = computed(() => {
  const isShowAll = graphShowAll.value
  const nodeCount = graphData.value.nodes.length
  const baseSize = isShowAll ? 28 : 42
  const pkgSize = isShowAll ? 20 : 32
  const labelFontSize = isShowAll ? 8 : 11
  const showLabel = !isShowAll || nodeCount < 80

  const nodes = graphData.value.nodes.map((node: any) => ({
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

  const edges = graphData.value.edges.map((edge: any) => ({
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

// 粒子系统
const particleCanvas = ref<HTMLCanvasElement>()
let particleAnimId = 0
let particleCleanup: (() => void) | null = null

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

onMounted(() => {
  loadGraph()
  setTimeout(() => {
    particleCleanup = initParticles() || null
  }, 100)
})

// 监听图表实例，绑定点击事件
watch(() => graphChartRef.value?.chart, (chart) => {
  if (chart) {
    chart.off('click')
    chart.on('click', (params: any) => {
      handleGraphClick(params)
    })
  }
}, { immediate: true })

onUnmounted(() => {
  if (particleCleanup) { particleCleanup(); particleCleanup = null }
})
</script>

<style scoped lang="scss">
.fullscreen-graph-page {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 9999;
  background: linear-gradient(180deg, #0B1A2F 0%, #132744 100%);
  padding: 12px 16px;
  overflow: hidden;
  color: #ffffff;
  display: flex;
  flex-direction: column;

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(0, 180, 255, 0.6), transparent);
    z-index: 10;
    pointer-events: none;
  }

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

  .fg-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    position: relative;
    height: 48px;
    flex-shrink: 0;
    padding: 0 8px;
    z-index: 5;

    .fg-title {
      font-size: 22px;
      font-weight: 800;
      color: #ffffff;
      letter-spacing: 3px;
      text-shadow: 0 0 20px rgba(0, 180, 255, 0.6), 0 0 40px rgba(0, 180, 255, 0.2);
    }

    .fg-stats {
      display: flex;
      gap: 32px;

      .fg-stat {
        text-align: center;
        .fg-stat-value {
          font-size: 24px;
          font-weight: 800;
          color: #00F0FF;
          text-shadow: 0 0 12px rgba(0, 240, 255, 0.5);
        }
        .fg-stat-label {
          font-size: 12px;
          color: #B0E0FF;
          margin-top: 2px;
        }
      }
    }
  }

  .fg-graph-wrap {
    flex: 1;
    position: relative;
    min-height: 0;
    background: rgba(0, 60, 120, 0.08);
    border: 1px solid rgba(0, 180, 255, 0.12);
    border-radius: 10px;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0;
      height: 2px;
      background: linear-gradient(90deg, transparent, #00B4FF, transparent);
      opacity: 0.5;
      z-index: 5;
    }

    .fg-chart {
      width: 100%;
      height: 100%;
    }

    .fg-controls {
      position: absolute;
      top: 10px;
      right: 14px;
      z-index: 10;
      background: rgba(11, 26, 47, 0.8);
      border: 1px solid rgba(0, 180, 255, 0.15);
      border-radius: 6px;
      padding: 6px 12px;
      backdrop-filter: blur(4px);
    }
  }

  .node-detail-float {
    position: absolute;
    top: 50px;
    right: 12px;
    width: 260px;
    background: rgba(11, 26, 47, 0.95);
    border: 1px solid rgba(0, 180, 255, 0.25);
    border-radius: 8px;
    padding: 14px;
    z-index: 100;
    backdrop-filter: blur(8px);
    max-height: calc(100% - 70px);
    overflow-y: auto;

    &::before {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0;
      height: 2px;
      background: linear-gradient(90deg, transparent, #00B4FF, transparent);
    }

    .detail-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
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

  .fg-back-btn {
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
}

@keyframes scanMove {
  0% { top: -2px; }
  100% { top: 100%; }
}
</style>
