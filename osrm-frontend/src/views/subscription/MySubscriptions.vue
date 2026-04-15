<template>
  <div class="my-subs-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">我的订购</h1>
        <p class="page-subtitle">查看已订购的软件包</p>
      </div>
      <el-button type="primary" @click="openApplyDialog"><el-icon><Plus /></el-icon>申请订购</el-button>
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

    <!-- 下载令牌弹窗 -->
    <el-dialog v-model="tokenDialogVisible" title="下载令牌" width="500px">
      <div v-if="tokenData">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="令牌"><code class="token-code">{{ tokenData.token }}</code></el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ tokenData.expireAt }}</el-descriptions-item>
          <el-descriptions-item label="下载次数">{{ tokenData.usedCount }} / {{ tokenData.maxDownloads }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 申请订购弹窗 -->
    <el-dialog v-model="applyDialogVisible" title="申请订购" width="550px" @close="resetApplyForm">
      <el-form ref="applyFormRef" :model="applyFormData" :rules="applyFormRules" label-width="100px">
        <el-form-item label="软件包" prop="packageId">
          <el-select v-model="applyFormData.packageId" placeholder="请选择软件包" filterable style="width: 100%"
            @change="onPackageChange" :loading="loadingPackages">
            <el-option v-for="p in publishedPackages" :key="p.id"
              :label="p.packageName" :value="p.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="版本" prop="versionId">
          <el-select v-model="applyFormData.versionId" placeholder="请选择版本" style="width: 100%"
            :disabled="!applyFormData.packageId" :loading="loadingVersions">
            <el-option v-for="v in versions" :key="v.id"
              :label="v.versionNo" :value="v.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="业务系统" prop="businessSystemId">
          <el-select v-model="applyFormData.businessSystemId" placeholder="请选择业务系统" filterable style="width: 100%"
            :loading="loadingSystems">
            <el-option v-for="s in businessSystems" :key="s.id"
              :label="`${s.systemName} (${s.systemCode})`" :value="s.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="使用场景" prop="useScene">
          <el-input v-model="applyFormData.useScene" type="textarea" :rows="3"
            placeholder="请描述使用场景，如：用于订单系统的消息队列服务" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleApplySubmit">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { softwareApi } from '@/api/software'
import { businessApi } from '@/api/business'
import { subscriptionApi } from '@/api/subscription'
import type { Subscription, DownloadTokenDTO } from '@/api/subscription'
import type { SoftwarePackage, SoftwareVersion } from '@/types/software'
import type { BusinessSystem } from '@/types/business'

const route = useRoute()

const loading = ref(false)
const tableData = ref<Subscription[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const tokenDialogVisible = ref(false)
const tokenData = ref<DownloadTokenDTO | null>(null)

// Apply dialog refs
const applyDialogVisible = ref(false)
const applyFormRef = ref<FormInstance>()
const submitting = ref(false)
const loadingPackages = ref(true)
const loadingVersions = ref(false)
const loadingSystems = ref(true)

const publishedPackages = ref<SoftwarePackage[]>([])
const versions = ref<SoftwareVersion[]>([])
const businessSystems = ref<BusinessSystem[]>([])

const applyFormData = reactive({
  packageId: null as number | null,
  versionId: null as number | null,
  businessSystemId: null as number | null,
  useScene: ''
})

const applyFormRules: FormRules = {
  packageId: [{ required: true, message: '请选择软件包', trigger: 'change' }],
  versionId: [{ required: true, message: '请选择版本', trigger: 'change' }],
  businessSystemId: [{ required: true, message: '请选择业务系统', trigger: 'change' }],
  useScene: [{ required: true, message: '请填写使用场景', trigger: 'blur' }]
}

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

const openApplyDialog = () => {
  applyDialogVisible.value = true
  // Check for pre-selected package from query params
  const prePackageId = route.query.packageId ? Number(route.query.packageId) : null
  const preVersionId = route.query.versionId ? Number(route.query.versionId) : null
  if (prePackageId) {
    applyFormData.packageId = prePackageId
    onPackageChange(prePackageId)
    if (preVersionId) {
      setTimeout(() => { applyFormData.versionId = preVersionId }, 100)
    }
  }
}

const resetApplyForm = () => {
  applyFormRef.value?.resetFields()
  applyFormData.packageId = null
  applyFormData.versionId = null
  applyFormData.businessSystemId = null
  applyFormData.useScene = ''
  versions.value = []
}

const onPackageChange = async (packageId: number) => {
  applyFormData.versionId = null
  loadingVersions.value = true
  try {
    versions.value = await softwareApi.getVersions(packageId)
  } catch { versions.value = [] }
  finally { loadingVersions.value = false }
}

const handleApplySubmit = async () => {
  if (!applyFormRef.value) return
  await applyFormRef.value.validate()
  submitting.value = true
  try {
    await subscriptionApi.apply({
      packageId: applyFormData.packageId!,
      versionId: applyFormData.versionId!,
      businessSystemId: applyFormData.businessSystemId!,
      useScene: applyFormData.useScene
    })
    ElMessage.success('订购申请已提交')
    applyDialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally { submitting.value = false }
}

// Load apply dialog dropdown data
const loadApplyDialogData = async () => {
  try {
    const pkgRes = await softwareApi.list({ status: 'PUBLISHED', page: 1, size: 100 })
    publishedPackages.value = pkgRes.content
  } catch { /* ignore - no published packages */ }
  finally { loadingPackages.value = false }

  try {
    const sysRes = await businessApi.list({ enabled: true, page: 1, size: 100 })
    businessSystems.value = sysRes.content
  } catch { /* ignore - user may not have permission */ }
  finally { loadingSystems.value = false }
}

onMounted(async () => {
  loadData()
  // Check if we should open apply dialog from query params
  if (route.query.apply === 'true') {
    await loadApplyDialogData()
    openApplyDialog()
  }
})
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
