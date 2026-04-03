<template>
  <div class="my-subs-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">我的订购</h1>
        <p class="page-subtitle">查看已订购的软件包</p>
      </div>
      <el-button type="primary" @click="$router.push('/subscription/apply')"><el-icon><Plus /></el-icon>申请订购</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="packageName" label="软件包" min-width="150" />
        <el-table-column prop="versionNumber" label="版本" width="100" />
        <el-table-column prop="systemName" label="业务系统" width="150" />
        <el-table-column prop="useScene" label="使用场景" min-width="150" show-overflow-tooltip />
        <el-table-column prop="statusName" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'APPROVED'" type="primary" link @click="handleGetToken(row)">下载令牌</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20]" layout="total, sizes, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="tokenDialogVisible" title="下载令牌" width="500px">
      <div v-if="tokenData">
        <p><strong>令牌：</strong><code>{{ tokenData.token }}</code></p>
        <p><strong>过期时间：</strong>{{ tokenData.expireAt }}</p>
        <p><strong>下载次数：</strong>{{ tokenData.usedCount }} / {{ tokenData.maxDownloads }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { subscriptionApi } from '@/api/subscription'
import type { Subscription, DownloadTokenDTO } from '@/api/subscription'

const loading = ref(false)
const tableData = ref<Subscription[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const tokenDialogVisible = ref(false)
const tokenData = ref<DownloadTokenDTO | null>(null)

const statusType = (s: string) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', REVOKED: 'info' }[s] || 'info')

const loadData = async () => {
  loading.value = true
  try {
    const res = await subscriptionApi.mySubscriptions(page.value, size.value)
    tableData.value = res.content; total.value = res.totalElements
  } catch { /* ignore */ }
  finally { loading.value = false }
}

const handleGetToken = async (row: Subscription) => {
  try {
    tokenData.value = await subscriptionApi.getToken(row.id)
    tokenDialogVisible.value = true
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '获取令牌失败') }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.my-subs-page {
  .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: var(--space-2xl);
    .page-title { font-size: var(--font-size-4xl); font-weight: var(--font-weight-bold); margin: 0; }
    .page-subtitle { font-size: var(--font-size-md); color: var(--color-text-secondary); margin: var(--space-xs) 0 0; }
  }
  .pagination-wrapper { margin-top: var(--space-lg); display: flex; justify-content: flex-end; }
}
</style>
