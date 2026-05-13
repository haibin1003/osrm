<template>
  <div class="system-detail">
    <div class="page-header">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h1 class="page-title">{{ system?.systemName || '业务系统详情' }}</h1>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="2" border v-loading="loading">
          <el-descriptions-item label="系统编号">{{ system?.systemCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="系统名称">{{ system?.systemName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="别名">{{ system?.systemAlias || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ system?.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="域">
            {{ [system?.domainL1, system?.domainL2, system?.domainL3].filter(Boolean).join(' / ') || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(system?.status)">{{ statusText(system?.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="责任人">{{ system?.responsiblePerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ system?.responsiblePhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="责任部门">{{ system?.responsibleDept || '-' }}</el-descriptions-item>
          <el-descriptions-item label="厂商">{{ system?.vendor || '-' }}</el-descriptions-item>
          <el-descriptions-item label="等级">{{ system?.level || '-' }}</el-descriptions-item>
          <el-descriptions-item label="建设模式">{{ system?.buildMode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上线日期">{{ system?.onlineDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="启用状态">
            <el-tag :type="system?.enabled ? 'success' : 'info'">{{ system?.enabled ? '启用' : '停用' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ system?.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="子应用" name="apps">
        <el-table :data="applications" v-loading="appsLoading" stripe border>
          <el-table-column prop="applicationCode" label="应用编码" width="160" />
          <el-table-column prop="applicationName" label="应用名称" min-width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="responsiblePerson" label="责任人" width="120" />
          <el-table-column prop="responsiblePhone" label="联系电话" width="140" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        </el-table>
        <el-empty v-if="!appsLoading && applications.length === 0" description="暂无子应用" />
      </el-tab-pane>

      <el-tab-pane label="关联软件" name="software">
        <el-table :data="dependencies" v-loading="depsLoading" stripe border>
          <el-table-column prop="packageName" label="软件名称" min-width="200" />
          <el-table-column prop="packageKey" label="唯一标识" min-width="160" />
          <el-table-column prop="versionNumber" label="版本" width="120">
            <template #default="{ row }">
              <span>{{ row.versionNumber || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="softwareType" label="软件类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ formatSoftwareType(row.softwareType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'APPROVED' ? 'success' : 'warning'" size="small">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!depsLoading && dependencies.length === 0" description="暂无关联软件" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getSystemCatalogDetail, getSystemApplications, type SystemCatalogItem, type ApplicationCatalogItem } from '@/api/catalog'
import { trackingApi } from '@/api/tracking'

const route = useRoute()
const systemId = Number(route.params.id)

const activeTab = ref('basic')
const loading = ref(false)
const appsLoading = ref(false)
const depsLoading = ref(false)
const system = ref<SystemCatalogItem | null>(null)
const applications = ref<ApplicationCatalogItem[]>([])
const dependencies = ref<any[]>([])

function statusType(s?: string) {
  switch (s) {
    case 'IN_USE': return 'success'
    case 'OFFLINE': return 'danger'
    case 'OFFLINE_REFERENCE': return 'warning'
    case 'BUILDING': return 'info'
    default: return ''
  }
}

function statusText(s?: string) {
  switch (s) {
    case 'IN_USE': return '在用'
    case 'OFFLINE': return '已下线'
    case 'OFFLINE_REFERENCE': return '下线备查'
    case 'BUILDING': return '建设中'
    default: return s || '-'
  }
}

function formatSoftwareType(type?: string) {
  const map: Record<string, string> = {
    'DOCKER_IMAGE': 'Docker镜像',
    'HELM_CHART': 'Helm Chart',
    'MAVEN': 'Maven',
    'NPM': 'NPM',
    'PYPI': 'PyPI',
    'GENERIC': '通用文件'
  }
  return map[type || ''] || type || '-'
}

function formatStatus(status?: string) {
  const map: Record<string, string> = {
    'APPROVED': '已审批',
    'PENDING': '待审批',
    'REJECTED': '已拒绝'
  }
  return map[status || ''] || status || '-'
}

function loadSystem() {
  loading.value = true
  getSystemCatalogDetail(systemId)
    .then((res: any) => {
      system.value = res
    })
    .catch((err: Error) => {
      ElMessage.error(err.message || '加载系统信息失败')
    })
    .finally(() => {
      loading.value = false
    })
}

function loadApplications() {
  appsLoading.value = true
  getSystemApplications(systemId)
    .then((res: any) => {
      applications.value = res || []
    })
    .catch((err: Error) => {
      ElMessage.error(err.message || '加载子应用失败')
    })
    .finally(() => {
      appsLoading.value = false
    })
}

function loadDependencies() {
  depsLoading.value = true
  trackingApi.getSystemDependencies(systemId)
    .then((res: any) => {
      dependencies.value = res.packages || []
    })
    .catch((err: Error) => {
      ElMessage.error(err.message || '加载关联软件失败')
    })
    .finally(() => {
      depsLoading.value = false
    })
}

onMounted(() => {
  loadSystem()
  loadApplications()
  loadDependencies()
})
</script>

<style scoped lang="scss">
.system-detail {
  .page-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;

    .page-title {
      font-size: 20px;
      font-weight: 600;
      margin: 0;
    }
  }
}
</style>
