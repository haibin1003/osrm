<template>
  <div class="role-management">
    <!-- 搜索栏 -->
    <div class="stripe-card search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="角色名称">
          <el-input v-model="searchForm.roleName" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="searchForm.roleCode" placeholder="请输入角色编码" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作栏 -->
    <div class="stripe-card table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">角色列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增角色
          </el-button>
        </div>
      </template>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="roleList"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column type="index" :index="(index) => index + 1 + (pagination.page - 1) * pagination.size" width="50" align="center" />
        <el-table-column prop="roleCode" label="角色编码" min-width="150" />
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column label="权限数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ row.permissionCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" min-width="150">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button type="primary" link @click="handlePermission(row)">
              <el-icon><Key /></el-icon>权限配置
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="formData.roleCode"
            placeholder="请输入角色编码，如：ROLE_ADMIN"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置对话框 -->
    <el-dialog
      v-model="permissionDialogVisible"
      title="权限配置"
      width="600px"
      destroy-on-close
    >
      <el-input
        v-model="permissionFilter"
        placeholder="搜索权限"
        clearable
        style="margin-bottom: 16px;"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-tree
        ref="permissionTreeRef"
        :data="permissionTreeData"
        show-checkbox
        node-key="id"
        :default-checked-keys="selectedPermissions"
        :props="{ label: 'permissionName', children: 'children' }"
        :filter-node-method="filterPermissionNode"
      >
        <template #default="{ node, data }">
          <span>
            {{ data.permissionName }}
            <el-tag v-if="data.permissionCode" size="small" type="info" style="margin-left: 8px;">
              {{ data.permissionCode }}
            </el-tag>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermissionSubmit" :loading="permissionSubmitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { roleApi } from '@/api/role'
import { permissionApi } from '@/api/permission'
import { Search, Plus, Edit, Delete, Key } from '@element-plus/icons-vue'

// 搜索表单
const searchForm = reactive({
  roleName: '',
  roleCode: ''
})

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 表格数据
const loading = ref(false)
const roleList = ref<any[]>([])

// 方法
const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.roleName = ''
  searchForm.roleCode = ''
  handleSearch()
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await roleApi.getList({
      page: pagination.page - 1,
      size: pagination.size,
      roleName: searchForm.roleName || undefined,
      roleCode: searchForm.roleCode || undefined
    })
    roleList.value = res.content || []
    pagination.total = res.totalElements || 0
  } catch (error: any) {
    console.error('加载角色列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  loadData()
}

const handlePageChange = (val: number) => {
  pagination.page = val
  loadData()
}

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const formData = reactive({
  id: undefined as number | undefined,
  roleCode: '',
  roleName: '',
  description: ''
})

const formRules: FormRules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^ROLE_[A-Z_]+$/, message: '格式为 ROLE_XXX，全大写', trigger: 'blur' }
  ],
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ]
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增角色'
  Object.assign(formData, {
    id: undefined,
    roleCode: '',
    roleName: '',
    description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  Object.assign(formData, {
    id: row.id,
    roleCode: row.roleCode,
    roleName: row.roleName,
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
          await roleApi.update(formData.id!, {
            roleName: formData.roleName,
            description: formData.description
          })
          ElMessage.success('更新成功')
        } else {
          await roleApi.create({
            roleCode: formData.roleCode,
            roleName: formData.roleName,
            description: formData.description
          })
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
  ElMessageBox.confirm(
    `确定要删除角色 "${row.roleName}" 吗？`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await roleApi.delete(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  })
}

// 权限配置对话框
const permissionDialogVisible = ref(false)
const permissionFilter = ref('')
const permissionTreeRef = ref<any>()
const currentRole = ref<any>(null)
const selectedPermissions = ref<number[]>([])
const permissionSubmitLoading = ref(false)
const permissionTreeData = ref<any[]>([])

// 加载权限树
const loadPermissionTree = async () => {
  try {
    const res: any = await permissionApi.getTree()
    permissionTreeData.value = res || []
  } catch (error) {
    console.error('加载权限树失败', error)
  }
}

const handlePermission = async (row: any) => {
  currentRole.value = row
  selectedPermissions.value = []
  permissionDialogVisible.value = true
  // 加载角色当前权限
  try {
    const res: any = await roleApi.getPermissions(row.id)
    selectedPermissions.value = res || []
  } catch (error) {
    console.error('加载角色权限失败', error)
  }
}

const filterPermissionNode = (value: string, data: any) => {
  if (!value) return true
  return data.permissionName.includes(value) || data.permissionCode?.includes(value)
}

watch(permissionFilter, (val) => {
  permissionTreeRef.value?.filter(val)
})

const handlePermissionSubmit = async () => {
  const checkedKeys = permissionTreeRef.value?.getCheckedKeys() || []
  const halfCheckedKeys = permissionTreeRef.value?.getHalfCheckedKeys() || []
  const allKeys = [...checkedKeys, ...halfCheckedKeys]

  permissionSubmitLoading.value = true
  try {
    await roleApi.configurePermissions(currentRole.value.id, allKeys)
    ElMessage.success('权限配置成功')
    permissionDialogVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '配置失败')
  } finally {
    permissionSubmitLoading.value = false
  }
}

// 日期格式化
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
  loadPermissionTree()
})
</script>

<style scoped lang="scss">
.role-management {
  padding: 24px;
  background: #f6f9fc;
  min-height: 100vh;

  // Stripe top gradient bar
  &::before {
    content: '';
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(135deg, #635bff, #a259ff);
    z-index: 1000;
  }

  // Override element-plus card styles
  :deep(.el-card) {
    border-radius: 10px;
    border: none;
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.04), 0 0 0 1px rgba(0, 0, 0, 0.03);
  }

  .stripe-card {
    background: #fff;
    border-radius: 10px;
    padding: 20px;
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.04), 0 0 0 1px rgba(0, 0, 0, 0.03);
  }

  .search-card {
    margin-bottom: 16px;

    :deep(.el-card__body) {
      padding-bottom: 0;
    }

    // Search form styles
    :deep(.el-form-item__label) {
      font-weight: 300;
      color: #525f7f;
    }

    :deep(.el-input__inner) {
      font-weight: 300;
    }
  }

  .table-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title {
        font-size: 16px;
        font-weight: 300;
        color: #32325d;
      }
    }

    :deep(.el-table) {
      font-weight: 300;
      color: #525f7f;

      th {
        font-weight: 400;
        color: #32325d;
        background: #f6f9fc;
      }
    }

    .status-badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 300;

      &.status-active {
        background: rgba(99, 91, 255, 0.1);
        color: #635bff;
      }

      &.status-inactive {
        background: rgba(108, 122, 136, 0.1);
        color: #6c7a89;
      }
    }
  }

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
