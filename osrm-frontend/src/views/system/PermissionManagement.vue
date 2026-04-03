<template>
  <div class="permission-management">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="权限名称">
          <el-input v-model="searchForm.permissionName" placeholder="请输入权限名称" clearable />
        </el-form-item>
        <el-form-item label="权限编码">
          <el-input v-model="searchForm.permissionCode" placeholder="请输入权限编码" clearable />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="searchForm.resourceType" placeholder="全部类型" clearable>
            <el-option label="菜单" value="menu" />
            <el-option label="按钮" value="button" />
            <el-option label="接口" value="api" />
            <el-option label="数据" value="data" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">权限列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增权限
          </el-button>
        </div>
      </template>

      <!-- 权限树表格 -->
      <el-table
        v-loading="loading"
        :data="permissionList"
        stripe
        border
        row-key="id"
        default-expand-all
        style="width: 100%"
      >
        <el-table-column prop="permissionName" label="权限名称" min-width="180">
          <template #default="{ row }">
            <el-icon v-if="row.icon" style="margin-right: 4px;">
              <component :is="row.icon" />
            </el-icon>
            <span>{{ row.permissionName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="permissionCode" label="权限编码" min-width="150" />
        <el-table-column prop="resourceType" label="资源类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getResourceTypeType(row.resourceType)">
              {{ getResourceTypeLabel(row.resourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.action" size="small">{{ row.action }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径/URL" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="550px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="上级权限">
          <el-tree-select
            v-model="formData.parentId"
            :data="permissionOptions"
            :props="{ label: 'permissionName', value: 'id', children: 'children' }"
            placeholder="请选择上级权限（不选则为顶级）"
            clearable
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="权限名称" prop="permissionName">
          <el-input v-model="formData.permissionName" placeholder="请输入权限名称" />
        </el-form-item>
        <el-form-item label="权限编码" prop="permissionCode">
          <el-input v-model="formData.permissionCode" placeholder="请输入权限编码，如：user:create" />
        </el-form-item>
        <el-form-item label="资源类型" prop="resourceType">
          <el-radio-group v-model="formData.resourceType">
            <el-radio label="menu">菜单</el-radio>
            <el-radio label="button">按钮</el-radio>
            <el-radio label="api">接口</el-radio>
            <el-radio label="data">数据</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="操作类型" prop="action">
          <el-select v-model="formData.action" placeholder="请选择操作类型" clearable style="width: 100%">
            <el-option label="查看" value="read" />
            <el-option label="新增" value="create" />
            <el-option label="编辑" value="update" />
            <el-option label="删除" value="delete" />
            <el-option label="导出" value="export" />
            <el-option label="导入" value="import" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径/URL" prop="path">
          <el-input v-model="formData.path" placeholder="请输入路径或URL" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="formData.icon" placeholder="请输入图标名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="请输入描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { permissionApi } from '@/api/permission'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'

// 搜索表单
const searchForm = reactive({
  permissionName: '',
  permissionCode: '',
  resourceType: ''
})

// 表格数据
const loading = ref(false)
const permissionList = ref<any[]>([])

// 权限选项（平级，用于上级权限选择）
const permissionOptions = ref<any[]>([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const formData = reactive({
  id: undefined as number | undefined,
  parentId: undefined as number | undefined,
  permissionName: '',
  permissionCode: '',
  resourceType: 'menu',
  action: '',
  path: '',
  icon: '',
  sortOrder: 0,
  description: ''
})

const formRules: FormRules = {
  permissionName: [
    { required: true, message: '请输入权限名称', trigger: 'blur' }
  ],
  permissionCode: [
    { required: true, message: '请输入权限编码', trigger: 'blur' }
  ],
  resourceType: [
    { required: true, message: '请选择资源类型', trigger: 'change' }
  ]
}

// 方法
const getResourceTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    menu: '菜单',
    button: '按钮',
    api: '接口',
    data: '数据'
  }
  return map[type] || type
}

const getResourceTypeType = (type: string) => {
  const map: Record<string, any> = {
    menu: 'primary',
    button: 'success',
    api: 'warning',
    data: 'info'
  }
  return map[type] || ''
}

const handleSearch = () => {
  loadData()
}

const handleReset = () => {
  searchForm.permissionName = ''
  searchForm.permissionCode = ''
  searchForm.resourceType = ''
  handleSearch()
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await permissionApi.getList({
      permissionName: searchForm.permissionName || undefined,
      permissionCode: searchForm.permissionCode || undefined,
      resourceType: searchForm.resourceType || undefined
    })
    permissionList.value = res || []
    // 同时更新选项数据
    permissionOptions.value = flattenPermissions(res || [])
  } catch (error: any) {
    console.error('加载权限列表失败', error)
  } finally {
    loading.value = false
  }
}

// 将树形数据转换为平级数据
const flattenPermissions = (list: any[], result: any[] = []) => {
  list.forEach(item => {
    result.push({
      id: item.id,
      permissionName: item.permissionName,
      permissionCode: item.permissionCode
    })
    if (item.children?.length) {
      flattenPermissions(item.children, result)
    }
  })
  return result
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增权限'
  Object.assign(formData, {
    id: undefined,
    parentId: undefined,
    permissionName: '',
    permissionCode: '',
    resourceType: 'menu',
    action: '',
    path: '',
    icon: '',
    sortOrder: 0,
    description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑权限'
  Object.assign(formData, {
    id: row.id,
    parentId: row.parentId,
    permissionName: row.permissionName,
    permissionCode: row.permissionCode,
    resourceType: row.resourceType,
    action: row.action,
    path: row.path,
    icon: row.icon,
    sortOrder: row.sortOrder,
    description: row.description
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          // TODO: 实现更新权限API
          ElMessage.success('更新成功')
        } else {
          await permissionApi.create(formData)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error: any) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleDelete = (row: any) => {
  if (row.children?.length > 0) {
    ElMessage.warning('请先删除子权限')
    return
  }
  ElMessageBox.confirm(
    `确定要删除权限 "${row.permissionName}" 吗？`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await permissionApi.delete(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.permission-management {
  .search-card {
    margin-bottom: var(--space-md);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-xs);

    :deep(.el-card__body) {
      padding-bottom: 0;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: var(--font-size-lg);
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
    }
  }
}
</style>
