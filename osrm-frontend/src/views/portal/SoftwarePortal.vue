<template>
  <div class="software-portal">
    <div class="portal-header">
      <div class="header-content">
        <h1>软件门户</h1>
        <p>发现和订购企业开源软件包</p>
        <el-input v-model="keyword" placeholder="搜索软件包..." @keyup.enter="handleSearch"
          class="search-input" size="large" clearable @clear="handleSearch">
          <template #append>
            <el-button :icon="Search" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-bar" v-if="stats">
      <div class="stat-item">
        <span class="num">{{ stats.publishedCount }}</span>
        <span class="label">已发布</span>
      </div>
      <el-divider direction="vertical" />
      <div class="stat-item">
        <span class="num">{{ stats.dockerCount }}</span>
        <span class="label">Docker</span>
      </div>
      <el-divider direction="vertical" />
      <div class="stat-item">
        <span class="num">{{ stats.helmCount }}</span>
        <span class="label">Helm</span>
      </div>
      <el-divider direction="vertical" />
      <div class="stat-item">
        <span class="num">{{ stats.mavenCount }}</span>
        <span class="label">Maven</span>
      </div>
      <el-divider direction="vertical" />
      <div class="stat-item">
        <span class="num">{{ stats.npmCount + stats.pypiCount + stats.genericCount }}</span>
        <span class="label">其他</span>
      </div>
    </div>

    <!-- 类型过滤 + 视图切换 -->
    <div class="filter-bar">
      <el-radio-group v-model="selectedType" @change="handleTypeChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="DOCKER_IMAGE">Docker 镜像</el-radio-button>
        <el-radio-button value="HELM_CHART">Helm Chart</el-radio-button>
        <el-radio-button value="MAVEN">Maven</el-radio-button>
        <el-radio-button value="NPM">NPM</el-radio-button>
        <el-radio-button value="PYPI">PyPI</el-radio-button>
        <el-radio-button value="GENERIC">通用文件</el-radio-button>
      </el-radio-group>

      <div class="view-toggle">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="grid">
            <el-icon><Grid /></el-icon>
          </el-radio-button>
          <el-radio-button value="list">
            <el-icon><List /></el-icon>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 软件包列表 - 卡片视图 -->
    <div v-if="viewMode === 'grid'" v-loading="loading" class="packages-grid">
      <el-empty v-if="!loading && packages.length === 0" description="暂无软件包" />

      <div v-for="pkg in packages" :key="pkg.id" class="pkg-card card-hover-lift" @click="goDetail(pkg.id)">
        <div class="pkg-type-badge">
          <el-tag size="small" :type="getTypeColor(pkg.softwareType)">{{ pkg.softwareTypeName }}</el-tag>
        </div>
        <h3 class="pkg-name">{{ pkg.packageName }}</h3>
        <p class="pkg-desc">{{ pkg.description || '暂无描述' }}</p>
        <div class="pkg-footer">
          <span class="version" v-if="pkg.currentVersion">
            <el-icon><PriceTag /></el-icon> v{{ pkg.currentVersion }}
          </span>
          <span class="version-count">{{ pkg.versionCount }} 个版本</span>
        </div>
      </div>
    </div>

    <!-- 软件包列表 - 列表视图 -->
    <div v-else v-loading="loading" class="packages-list">
      <el-empty v-if="!loading && packages.length === 0" description="暂无软件包" />

      <el-table v-else :data="packages" @row-click="(row) => goDetail(row.id)"
        style="cursor: pointer;">
        <el-table-column prop="packageName" label="软件名称" min-width="180">
          <template #default="{ row }">
            <strong>{{ row.packageName }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="softwareTypeName" label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="getTypeColor(row.softwareType)">{{ row.softwareTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="success">已发布</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="versionCount" label="版本数" width="80" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize"
        :total="total" :page-sizes="[12, 24, 48]" layout="total, sizes, prev, pager, next"
        @size-change="loadData" @current-change="loadData" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, PriceTag, Grid, List } from '@element-plus/icons-vue'
import { portalApi } from '@/api/portal'
import type { SoftwarePackage } from '@/types/software'

const router = useRouter()
const route = useRoute()
const keyword = ref('')
const selectedType = ref('')
const viewMode = ref('grid') // 'grid' | 'list'
const loading = ref(false)
const packages = ref<SoftwarePackage[]>([])
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const stats = ref<any>(null)

const typeColorMap: Record<string, string> = {
  'DOCKER_IMAGE': 'primary',
  'HELM_CHART': 'info',
  'MAVEN': 'warning',
  'NPM': 'danger',
  'PYPI': 'success',
  'GENERIC': 'info'
}

const getTypeColor = (type: string) => typeColorMap[type] || 'info'

const loadData = async () => {
  loading.value = true
  try {
    const res = await portalApi.listSoftware({
      keyword: keyword.value || undefined,
      type: selectedType.value || undefined,
      page: page.value,
      size: pageSize.value
    })
    packages.value = res.content
    total.value = res.totalElements
  } finally { loading.value = false }
}

const loadStats = async () => {
  try {
    const res = await portalApi.getStats()
    stats.value = res
  } catch { /* ignore */ }
}

const handleSearch = () => { page.value = 1; loadData() }
const handleTypeChange = () => { page.value = 1; loadData() }
const goDetail = (id: number) => {
  const base = route.path.startsWith('/browse') ? '/browse' : '/portal'
  router.push(`${base}/software/${id}`)
}

onMounted(() => {
  loadData()
  loadStats()
})
</script>

<style scoped lang="scss">
.software-portal {
  .portal-header {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
    border-radius: var(--radius-xl);
    padding: 48px 40px;
    margin-bottom: var(--space-xl);
    .header-content {
      max-width: 600px;
      h1 { font-size: var(--font-size-3xl); font-weight: 700; color: #fff; margin: 0 0 8px; }
      p { color: rgba(255,255,255,0.7); font-size: var(--font-size-lg); margin: 0 0 24px; }
      .search-input { max-width: 480px; }
    }
  }

  .stats-bar {
    display: flex; align-items: center; gap: 16px;
    background: var(--color-bg-card); border-radius: var(--radius-lg);
    padding: 16px 24px; margin-bottom: var(--space-lg);
    overflow-x: auto;
    .stat-item { display: flex; flex-direction: column; align-items: center; gap: 4px;
      .num { font-size: var(--font-size-2xl); font-weight: 700; color: var(--color-primary); }
      .label { font-size: var(--font-size-xs); color: var(--color-text-secondary); }
    }
  }

  .filter-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-lg);

    .view-toggle {
      .el-radio-group {
        .el-radio-button__inner {
          padding: 8px 12px;
        }
      }
    }
  }

  .packages-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: var(--space-lg);
    min-height: 200px;
    .pkg-card {
      background: var(--color-bg-card); border: 1px solid var(--color-border);
      border-radius: var(--radius-xl); padding: var(--space-xl); cursor: pointer;
      .pkg-type-badge { margin-bottom: var(--space-md); }
      .pkg-name { font-size: var(--font-size-lg); font-weight: 600; margin: 0 0 var(--space-sm); color: var(--color-text-primary); }
      .pkg-desc { font-size: var(--font-size-sm); color: var(--color-text-secondary); margin: 0 0 var(--space-lg);
        overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
      .pkg-footer { display: flex; justify-content: space-between; align-items: center;
        font-size: var(--font-size-xs); color: var(--color-text-tertiary);
        .version { display: flex; align-items: center; gap: 4px; }
      }
    }
  }

  .packages-list {
    min-height: 200px;
    background: var(--color-bg-card);
    border-radius: var(--radius-xl);
    padding: var(--space-lg);
  }

  .pagination-wrapper { margin-top: var(--space-xl); display: flex; justify-content: center; }
}

@media (max-width: 768px) {
  .software-portal {
    .filter-bar {
      flex-direction: column;
      gap: var(--space-md);
      align-items: flex-start;
    }
  }
}
</style>
