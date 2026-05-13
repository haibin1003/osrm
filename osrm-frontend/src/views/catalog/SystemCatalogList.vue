<template>
  <div class="catalog-list">
    <div class="page-header">
      <h1 class="page-title">系统目录</h1>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索系统名称/编号"
        clearable
        style="width: 260px"
        @keyup.enter="loadData"
      />
      <el-select v-model="status" placeholder="状态" clearable style="width: 140px" @change="loadData">
        <el-option label="在用" value="IN_USE" />
        <el-option label="已下线" value="OFFLINE" />
        <el-option label="下线备查" value="OFFLINE_REFERENCE" />
        <el-option label="建设中" value="BUILDING" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe border style="margin-top: 16px">
      <el-table-column prop="systemCode" label="系统编号" width="140" />
      <el-table-column prop="systemName" label="系统名称" min-width="180" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="responsiblePerson" label="责任人" width="100" />
      <el-table-column prop="responsiblePhone" label="联系电话" width="120" />
      <el-table-column prop="hasApplications" label="含应用" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.hasApplications" type="success">是</el-tag>
          <el-tag v-else type="info">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="启用" width="80">
        <template #default="{ row }">
          <el-switch
            v-model="row.enabled"
            inline-prompt
            active-text="启"
            inactive-text="停"
            @change="(val: boolean) => toggleEnabled(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @change="loadData"
    />

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="detailTitle" size="500px">
      <div v-if="detail" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="系统编号">{{ detail.systemCode }}</el-descriptions-item>
          <el-descriptions-item label="系统名称">{{ detail.systemName }}</el-descriptions-item>
          <el-descriptions-item label="别名">{{ detail.systemAlias || '-' }}</el-descriptions-item>
          <el-descriptions-item label="单位">{{ detail.unit || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="域">{{ [detail.domainL1, detail.domainL2, detail.domainL3].filter(Boolean).join(' / ') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)">{{ statusText(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="责任人">{{ detail.responsiblePerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.responsiblePhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="责任部门">{{ detail.responsibleDept || '-' }}</el-descriptions-item>
          <el-descriptions-item label="厂商">{{ detail.vendor || '-' }}</el-descriptions-item>
          <el-descriptions-item label="等级">{{ detail.level || '-' }}</el-descriptions-item>
          <el-descriptions-item label="建设模式">{{ detail.buildMode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上线日期">{{ detail.onlineDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ detail.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.hasApplications" class="app-section">
          <div class="app-title">子应用列表</div>
          <el-table :data="detail.applications" stripe border size="small">
            <el-table-column prop="applicationCode" label="应用编码" width="140" />
            <el-table-column prop="applicationName" label="应用名称" />
            <el-table-column prop="responsiblePerson" label="责任人" width="100" />
            <el-table-column prop="responsiblePhone" label="联系电话" width="120" />
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSystemCatalogList,
  setSystemCatalogEnabled,
  type SystemCatalogItem
} from '@/api/catalog'

const router = useRouter()
const keyword = ref('')
const status = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const list = ref<SystemCatalogItem[]>([])
const loading = ref(false)

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

function loadData() {
  loading.value = true
  getSystemCatalogList({
    keyword: keyword.value || undefined,
    status: status.value || undefined,
    page: page.value,
    size: size.value
  }).then((res: any) => {
    list.value = res.content || []
    total.value = res.totalElements || 0
  }).catch((err: Error) => {
    ElMessage.error(err.message || '加载失败')
  }).finally(() => {
    loading.value = false
  })
}

function toggleEnabled(row: SystemCatalogItem, val: boolean) {
  setSystemCatalogEnabled(row.id, val).then(() => {
    ElMessage.success(val ? '已启用' : '已停用')
  }).catch((err: Error) => {
    ElMessage.error(err.message || '操作失败')
    row.enabled = !val
  })
}

function showDetail(row: SystemCatalogItem) {
  router.push('/catalog/systems/' + row.id)
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.catalog-list {
  .page-header {
    margin-bottom: 20px;
  }

  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .filter-bar {
    display: flex;
    gap: 12px;
    align-items: center;
  }
}

.detail-content {
  .app-section {
    margin-top: 20px;
  }

  .app-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: 10px;
  }
}
</style>
