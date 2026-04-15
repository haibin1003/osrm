<template>
  <div class="packages-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">软件包管理</h1>
        <p class="page-subtitle">管理企业开源软件包</p>
      </div>
      <el-button v-if="canCreate" type="primary" @click="showCreateDialog"><el-icon><Plus /></el-icon>新增软件包</el-button>
    </div>

    <!-- 搜索 (Stripe style card) -->
    <div class="search-card stripe-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="包名/包标识" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable>
            <el-option label="Docker 镜像" value="DOCKER_IMAGE" />
            <el-option label="Helm Chart" value="HELM_CHART" />
            <el-option label="Maven" value="MAVEN" />
            <el-option label="NPM" value="NPM" />
            <el-option label="PyPI" value="PYPI" />
            <el-option label="通用文件" value="GENERIC" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="草稿" value="DRAFT" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下架" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-card stripe-card">
      <el-table v-loading="loading" :data="tableData">
        <el-table-column prop="packageName" label="包名" min-width="150" />
        <el-table-column prop="packageKey" label="包标识" min-width="150" />
        <el-table-column prop="softwareTypeName" label="类型" width="110">
          <template #default="{ row }"><el-tag size="small">{{ row.softwareTypeName }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="currentVersion" label="当前版本" width="110">
          <template #default="{ row }"><span>{{ row.currentVersion || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="versionCount" label="版本数" width="80" align="center" />
        <el-table-column prop="statusName" label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="statusClass(row.status)">{{ row.statusName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT' && canUpdate" type="primary" link @click="showVersionDialog(row)">版本管理</el-button>
            <el-button v-if="row.status === 'DRAFT' && canUpdate" type="primary" link @click="showEditDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT' && canApprove" type="success" link @click="handleSubmit(row)">提交审核</el-button>
            <el-button v-if="row.status === 'PENDING' && canApprove" type="success" link @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 'PENDING' && canApprove" type="warning" link @click="handleReject(row)">驳回</el-button>
            <el-button v-if="row.status === 'PUBLISHED' && canUpdate" type="warning" link @click="handleOffline(row)">下架</el-button>
            <el-button v-if="row.status === 'OFFLINE' && canUpdate" type="success" link @click="handleRepublish(row)">重新上架</el-button>
            <el-button type="primary" link @click="$router.push(`/portal/software/${row.id}`)">详情</el-button>
            <el-popconfirm v-if="row.status === 'DRAFT' && canDelete" title="确定删除？" @confirm="handleDelete(row)">
              <template #reference><el-button type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
          :total="pagination.total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
          @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <!-- 新增软件包弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新增软件包" width="550px" @close="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="包名" prop="packageName">
          <el-input v-model="createForm.packageName" placeholder="如：my-app（中文名称）" />
        </el-form-item>
        <el-form-item label="包标识" prop="packageKey">
          <el-input v-model="createForm.packageKey" placeholder="如：my-app（英文唯一标识）" />
          <div class="form-tip">用于系统唯一标识，只允许字母、数字、连字符</div>
        </el-form-item>
        <el-form-item label="类型" prop="softwareType">
          <el-select v-model="createForm.softwareType" placeholder="请选择" style="width: 100%">
            <el-option label="Docker 镜像" value="DOCKER_IMAGE" />
            <el-option label="Helm Chart" value="HELM_CHART" />
            <el-option label="Maven 依赖" value="MAVEN" />
            <el-option label="NPM 包" value="NPM" />
            <el-option label="PyPI 包" value="PYPI" />
            <el-option label="通用文件" value="GENERIC" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 版本管理弹窗 -->
    <el-dialog v-model="versionDialogVisible" :title="`版本管理 - ${currentPackageName}`" width="700px">
      <div style="margin-bottom: var(--space-md);">
        <el-button type="primary" size="small" @click="showAddVersion = !showAddVersion">
          {{ showAddVersion ? '收起' : '添加版本' }}
        </el-button>
      </div>
      <el-form v-if="showAddVersion" ref="versionFormRef" :model="versionForm" :rules="versionRules" label-width="100px" class="version-form">
        <el-form-item label="版本号" prop="versionNo">
          <el-input v-model="versionForm.versionNo" placeholder="如：1.0.0" style="width: 200px" />
        </el-form-item>
        <el-form-item label="存储后端" prop="storageBackendId">
          <el-select v-model="versionForm.storageBackendId" placeholder="请选择存储后端" style="width: 200px">
            <el-option v-for="b in storageBackends" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布说明">
          <el-input v-model="versionForm.releaseNotes" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" :loading="addingVersion" @click="handleAddVersion">确认添加</el-button>
          <el-button size="small" @click="showAddVersion = false">取消</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="versionList" size="small">
        <el-table-column prop="versionNo" label="版本号" width="120" />
        <el-table-column prop="statusName" label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isLatest" label="最新" width="60" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isLatest" type="success" size="small">✓</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="releaseNotes" label="发布说明" min-width="150" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="文件大小" width="100">
          <template #default="{ row }">{{ row.fileSize ? formatSize(row.fileSize) : '-' }}</template>
        </el-table-column>
        <el-table-column label="制品路径" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.storagePath" class="artifact-path">{{ row.storagePath }}</span>
            <span v-else class="text-muted">未上传</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" type="success" link size="small" @click="handlePublishVersion(row)">发布</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" type="warning" link size="small" @click="handleOfflineVersion(row)">下线</el-button>
            <el-upload :show-file-list="false" :before-upload="(file: File) => handleUploadArtifact(row, file)"
              accept=".tar,.jar,.war,.pom,.tgz,.gz,.zip,.whl,.gem">
              <el-button type="primary" link size="small">上传制品</el-button>
            </el-upload>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 制品上传结果弹窗 -->
    <el-dialog v-model="uploadResultVisible" title="上传成功" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="文件路径">{{ uploadResult.filePath }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ uploadResult.fileSize ? formatSize(uploadResult.fileSize) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="MD5">{{ uploadResult.md5Hash || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="uploadResult.downloadCommand" label="下载/使用命令">
          <div style="display: flex; align-items: center; gap: 8px;">
            <code class="code-block">{{ uploadResult.downloadCommand }}</code>
            <el-button size="small" type="primary" link @click="copyText(uploadResult.downloadCommand)">复制</el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="uploadResultVisible = false">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑软件包弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑软件包" width="550px" @close="resetEditForm">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="包名" prop="packageName">
          <el-input v-model="editForm.packageName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 下架确认弹窗 -->
    <el-dialog v-model="offlineDialogVisible" title="下架确认" width="400px">
      <el-form>
        <el-form-item label="下架原因">
          <el-input v-model="offlineReason" type="textarea" :rows="3" placeholder="请输入下架原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offlineDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmOffline">确认下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { softwareApi } from '@/api/software'
import { artifactApi } from '@/api/artifact'
import { storageApi } from '@/api/storage'
import { useAuthStore } from '@/stores/modules/auth'
import type { SoftwarePackage, SoftwareVersion, PackageForm, VersionForm } from '@/types/software'

const authStore = useAuthStore()
const route = useRoute()

// 权限检查
const canCreate = computed(() => authStore.hasPermission('package:create'))
const canUpdate = computed(() => authStore.hasPermission('package:update'))
const canApprove = computed(() => authStore.hasPermission('package:approve'))
const canDelete = computed(() => authStore.hasPermission('package:delete'))
const canManageStorage = computed(() => authStore.hasPermission('storage:read'))

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<SoftwarePackage[]>([])
const searchForm = reactive({ keyword: '', type: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

// 存储后端列表
const storageBackends = ref<{ id: number; name: string }[]>([])

// 新增软件包
const createDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<PackageForm>({ packageName: '', packageKey: '', softwareType: 'DOCKER_IMAGE', description: '' })
const createRules: FormRules = {
  packageName: [{ required: true, message: '请输入包名', trigger: 'blur' }],
  packageKey: [{ required: true, message: '请输入包标识', trigger: 'blur' }, { pattern: /^[a-zA-Z0-9._-]+$/, message: '只允许字母、数字、连字符、点', trigger: 'blur' }],
  softwareType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

// 版本管理
const versionDialogVisible = ref(false)
const showAddVersion = ref(false)
const addingVersion = ref(false)
const currentPackageId = ref<number | null>(null)
const currentPackageName = ref('')
const versionList = ref<SoftwareVersion[]>([])
const versionFormRef = ref<FormInstance>()
const versionForm = reactive<VersionForm>({ versionNo: '', storageBackendId: null, releaseNotes: '' })
const versionRules: FormRules = {
  versionNo: [{ required: true, message: '请输入版本号', trigger: 'blur' }, { pattern: /^\d+\.\d+\.\d+/, message: '格式如：1.0.0', trigger: 'blur' }],
  storageBackendId: [{ required: true, message: '请选择存储后端', trigger: 'change' }]
}

// 编辑软件包
const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref<FormInstance>()
const editTargetId = ref<number | null>(null)
const editForm = reactive({ packageName: '', description: '' })
const editRules: FormRules = {
  packageName: [{ required: true, message: '请输入包名', trigger: 'blur' }]
}

const showEditDialog = (row: SoftwarePackage) => {
  editTargetId.value = row.id
  editForm.packageName = row.packageName
  editForm.description = row.description || ''
  editDialogVisible.value = true
}

const resetEditForm = () => { editFormRef.value?.resetFields() }

const handleEdit = async () => {
  if (!editFormRef.value || !editTargetId.value) return
  await editFormRef.value.validate()
  editSubmitting.value = true
  try {
    await softwareApi.update(editTargetId.value, editForm)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '更新失败') }
  finally { editSubmitting.value = false }
}

// 下架
const offlineDialogVisible = ref(false)
const offlineReason = ref('')
const offlineTarget = ref<SoftwarePackage | null>(null)

const statusClass = (s: string) => ({ DRAFT: '', PENDING: 'warning', PUBLISHED: 'success', OFFLINE: 'danger', ARCHIVED: '' }[s] || '')

const loadData = async () => {
  loading.value = true
  try {
    const res = await softwareApi.list({ keyword: searchForm.keyword || undefined, type: searchForm.type || undefined, status: searchForm.status || undefined, page: pagination.page, size: pagination.size })
    tableData.value = res.content
    pagination.total = res.totalElements
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '加载失败') }
  finally { loading.value = false }
}

const handleSearch = () => { pagination.page = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.type = ''; searchForm.status = ''; handleSearch() }

const showCreateDialog = () => { createDialogVisible.value = true }
const resetCreateForm = () => { createFormRef.value?.resetFields(); Object.assign(createForm, { packageName: '', packageKey: '', softwareType: 'DOCKER_IMAGE', description: '' }) }

const handleCreate = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate()
  submitting.value = true
  try {
    await softwareApi.create(createForm)
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '创建失败') }
  finally { submitting.value = false }
}

const handleSubmit = async (row: SoftwarePackage) => {
  try { await softwareApi.submit(row.id); ElMessage.success('已提交审核'); loadData() }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '提交失败') }
}

const handleApprove = async (row: SoftwarePackage) => {
  try { await softwareApi.approve(row.id); ElMessage.success('审批通过'); loadData() }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '审批失败') }
}

const handleReject = async (row: SoftwarePackage) => {
  try { await softwareApi.reject(row.id); ElMessage.success('已驳回'); loadData() }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '驳回失败') }
}

const handleOffline = (row: SoftwarePackage) => {
  offlineTarget.value = row
  offlineReason.value = ''
  offlineDialogVisible.value = true
}

const confirmOffline = async () => {
  if (!offlineTarget.value) return
  try {
    await softwareApi.offline(offlineTarget.value.id, offlineReason.value || undefined)
    ElMessage.success('已下架')
    offlineDialogVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '下架失败') }
}

const handleRepublish = async (row: SoftwarePackage) => {
  try { await softwareApi.republish(row.id); ElMessage.success('已重新上架'); loadData() }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '上架失败') }
}

const handleDelete = async (row: SoftwarePackage) => {
  try { await softwareApi.delete(row.id); ElMessage.success('删除成功'); loadData() }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '删除失败') }
}

const showVersionDialog = async (row: SoftwarePackage) => {
  currentPackageId.value = row.id
  currentPackageName.value = row.packageName
  showAddVersion.value = false
  versionList.value = await softwareApi.getVersions(row.id)
  versionDialogVisible.value = true
}

const handleAddVersion = async () => {
  if (!versionFormRef.value || !currentPackageId.value) return
  await versionFormRef.value.validate()
  addingVersion.value = true
  try {
    await softwareApi.createVersion(currentPackageId.value, versionForm)
    ElMessage.success('版本添加成功')
    versionList.value = await softwareApi.getVersions(currentPackageId.value)
    versionForm.versionNo = ''; versionForm.storageBackendId = null; versionForm.releaseNotes = ''
    showAddVersion.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '添加失败') }
  finally { addingVersion.value = false }
}

// 制品上传结果
const uploadResultVisible = ref(false)
const uploadResult = reactive({ filePath: '', fileSize: 0, md5Hash: '', downloadCommand: '' })

const copyText = async (text: string) => {
  try { await navigator.clipboard.writeText(text); ElMessage.success('已复制') }
  catch { ElMessage.error('复制失败') }
}

const handlePublishVersion = async (version: SoftwareVersion) => {
  if (!currentPackageId.value) return
  try {
    await softwareApi.publishVersion(currentPackageId.value, version.id)
    ElMessage.success('版本已发布')
    versionList.value = await softwareApi.getVersions(currentPackageId.value)
    loadData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '发布失败') }
}

const handleOfflineVersion = async (version: SoftwareVersion) => {
  if (!currentPackageId.value) return
  try {
    await softwareApi.offlineVersion(currentPackageId.value, version.id)
    ElMessage.success('版本已下线')
    versionList.value = await softwareApi.getVersions(currentPackageId.value)
    loadData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '下线失败') }
}

const handleUploadArtifact = async (version: SoftwareVersion, file: File) => {
  if (!currentPackageId.value) return false
  try {
    ElMessage.info('正在上传，请稍候...')
    const result = await artifactApi.upload(currentPackageId.value, version.id, file)
    uploadResult.filePath = result.filePath || ''
    uploadResult.fileSize = result.fileSize || 0
    uploadResult.md5Hash = result.md5Hash || ''
    uploadResult.downloadCommand = result.downloadCommand || ''
    uploadResultVisible.value = true
    versionList.value = await softwareApi.getVersions(currentPackageId.value)
    loadData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '上传失败') }
  return false
}

const formatSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

onMounted(async () => {
  loadData()
  // 检查是否有预操作参数
  if (route.query.action === 'create' && canCreate.value) {
    showCreateDialog()
  }
  // 只有有存储权限的用户才加载存储后端列表
  if (canManageStorage.value) {
    try {
      const res = await storageApi.getList({ page: 1, size: 100 })
      storageBackends.value = res.content.map((b: any) => ({ id: b.id, name: b.backendName }))
    } catch { /* ignore */ }
  }
})
</script>

<style scoped lang="scss">
.packages-page {
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

  .search-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    overflow: hidden;
    position: relative;
    margin-bottom: var(--space-lg);
    padding: var(--space-lg);

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(135deg, #635bff, #a259ff);
    }

    :deep(.el-form-item) {
      margin-bottom: 0;
    }

    :deep(.el-select) {
      width: 140px;
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

  .artifact-path {
    font-family: var(--font-mono);
    font-size: 12px;
    color: var(--color-text-secondary);
  }

  .version-form {
    background: var(--color-bg-page);
    border-radius: var(--radius-md);
    padding: var(--space-md);
    margin-bottom: var(--space-md);
  }

  .code-block {
    flex: 1;
    word-break: break-all;
    font-size: 12px;
    background: var(--color-bg-page);
    padding: var(--space-xs) var(--space-sm);
    border-radius: var(--radius-sm);
    font-family: var(--font-mono);
  }
}
</style>
