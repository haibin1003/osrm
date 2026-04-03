<template>
  <div class="business-systems">
    <div class="page-header">
      <div>
        <h1 class="page-title">业务系统管理</h1>
        <p class="page-subtitle">管理企业业务系统信息</p>
      </div>
      <el-button v-if="canCreate" type="primary" @click="showDialog('add')">
        <el-icon><Plus /></el-icon>新增系统
      </el-button>
    </div>

    <!-- 搜索 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="系统编码/名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="所属域">
          <el-select v-model="searchForm.domain" placeholder="全部" clearable style="width: 140px">
            <el-option label="业务域" value="BUSINESS" />
            <el-option label="运营域" value="OPERATION" />
            <el-option label="资源域" value="RESOURCE" />
            <el-option label="服务域" value="SERVICE" />
            <el-option label="数据域" value="DATA" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.enabled" placeholder="全部" clearable style="width: 100px">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="systemCode" label="系统编码" min-width="120" />
        <el-table-column prop="systemName" label="系统名称" min-width="150" />
        <el-table-column prop="domainName" label="所属域" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.domainName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responsiblePerson" label="负责人" width="120" />
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canUpdate" type="primary" link @click="showDialog('edit', row)">编辑</el-button>
            <el-button v-if="canUpdate" :type="row.enabled ? 'warning' : 'success'" link @click="handleToggleEnabled(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
            <el-popconfirm v-if="canDelete" title="确定删除？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button type="danger" link :disabled="row.enabled">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增业务系统' : '编辑业务系统'"
      width="560px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="系统编码" prop="systemCode" v-if="dialogMode === 'add'">
          <el-input v-model="formData.systemCode" placeholder="如：order-sys" />
        </el-form-item>
        <el-form-item label="系统名称" prop="systemName">
          <el-input v-model="formData.systemName" placeholder="请输入系统名称" />
        </el-form-item>
        <el-form-item label="所属域" prop="domain">
          <el-select v-model="formData.domain" placeholder="请选择" style="width: 100%">
            <el-option label="业务域" value="BUSINESS" />
            <el-option label="运营域" value="OPERATION" />
            <el-option label="资源域" value="RESOURCE" />
            <el-option label="服务域" value="SERVICE" />
            <el-option label="数据域" value="DATA" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="responsiblePerson">
          <el-input v-model="formData.responsiblePerson" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { businessApi } from '@/api/business'
import { useAuthStore } from '@/stores/modules/auth'
import type { BusinessSystem, BusinessSystemForm, BusinessDomain } from '@/types/business'

const authStore = useAuthStore()

// 权限检查
const canCreate = computed(() => authStore.hasPermission('business-system:create'))
const canUpdate = computed(() => authStore.hasPermission('business-system:update'))
const canDelete = computed(() => authStore.hasPermission('business-system:delete'))

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<BusinessSystem[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const searchForm = reactive({ keyword: '', domain: '', enabled: null as boolean | null })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const formData = reactive<BusinessSystemForm>({ systemCode: '', systemName: '', domain: 'BUSINESS', responsiblePerson: '', description: '' })

const formRules: FormRules = {
  systemCode: [{ required: true, message: '请输入系统编码', trigger: 'blur' }, { pattern: /^[a-zA-Z0-9_-]+$/, message: '只支持字母、数字、下划线和连字符', trigger: 'blur' }],
  systemName: [{ required: true, message: '请输入系统名称', trigger: 'blur' }],
  domain: [{ required: true, message: '请选择所属域', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await businessApi.list({ keyword: searchForm.keyword || undefined, domain: searchForm.domain || undefined, enabled: searchForm.enabled, page: pagination.page, size: pagination.size })
    tableData.value = res.content
    pagination.total = res.totalElements
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.domain = ''; searchForm.enabled = null; handleSearch() }

const showDialog = (mode: 'add' | 'edit', row?: BusinessSystem) => {
  dialogMode.value = mode
  if (mode === 'edit' && row) {
    editId.value = row.id
    formData.systemName = row.systemName
    formData.domain = row.domain
    formData.responsiblePerson = row.responsiblePerson || ''
    formData.description = row.description || ''
  }
  dialogVisible.value = true
}

const resetForm = () => { formRef.value?.resetFields(); editId.value = null }

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (dialogMode.value === 'add') {
      await businessApi.create(formData)
      ElMessage.success('创建成功')
    } else if (editId.value) {
      await businessApi.update(editId.value, { systemName: formData.systemName, domain: formData.domain, responsiblePerson: formData.responsiblePerson, description: formData.description })
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleEnabled = async (row: BusinessSystem) => {
  try {
    await businessApi.setEnabled(row.id, !row.enabled)
    ElMessage.success(row.enabled ? '已停用' : '已启用')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (row: BusinessSystem) => {
  try {
    await businessApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.business-systems {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: var(--space-2xl);

    .page-title { font-size: var(--font-size-4xl); font-weight: var(--font-weight-bold); margin: 0; color: var(--color-text-primary); }
    .page-subtitle { font-size: var(--font-size-md); color: var(--color-text-secondary); margin: var(--space-xs) 0 0; }
  }

  .search-card {
    margin-bottom: var(--space-lg);
    :deep(.el-card__body) { padding-bottom: 0; }
  }

  .table-card { margin-bottom: var(--space-lg); }

  .pagination-wrapper { margin-top: var(--space-lg); display: flex; justify-content: flex-end; }
}
</style>
