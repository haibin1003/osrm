<template>
  <div class="my-subs-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">我的订购</h1>
        <p class="page-subtitle">查看已订购的软件包</p>
      </div>
      <el-button type="primary" @click="$router.push('/subscription/apply')"><el-icon><Plus /></el-icon>申请订购</el-button>
    </div>

    <div class="table-card stripe-card">
      <el-table v-loading="loading" :data="tableData">
        <el-table-column prop="packageName" label="软件包" min-width="150" />
        <el-table-column prop="versionNumber" label="版本" width="100" />
        <el-table-column prop="systemName" label="业务系统" width="150" />
        <el-table-column prop="useScene" label="使用场景" min-width="150" show-overflow-tooltip />
        <el-table-column prop="statusName" label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="statusClass(row.status)">{{ row.statusName }}</span>
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
    </div>

    <el-dialog v-model="tokenDialogVisible" title="下载令牌" width="500px">
      <div v-if="tokenData">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="令牌"><code class="token-code">{{ tokenData.token }}</code></el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ tokenData.expireAt }}</el-descriptions-item>
          <el-descriptions-item label="下载次数">{{ tokenData.usedCount }} / {{ tokenData.maxDownloads }}</el-descriptions-item>
        </el-descriptions>
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

const statusClass = (s: string) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', REVOKED: '' }[s] || '')

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
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
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

  .table-card {
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

  .pagination-wrapper {
    margin-top: var(--space-lg);
    display: flex;
    justify-content: flex-end;
    padding: var(--space-md) var(--space-lg);
    border-top: 1px solid var(--color-border-light);
  }

  .token-code {
    font-family: var(--font-mono);
    font-size: 12px;
    background: var(--color-bg-page);
    padding: 2px 6px;
    border-radius: var(--radius-sm);
    word-break: break-all;
  }
}
</style>
