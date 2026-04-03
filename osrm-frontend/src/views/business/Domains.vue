<template>
  <div class="domain-management">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="域名称">
          <el-input v-model="searchForm.name" placeholder="请输入域名称" clearable />
        </el-form-item>
        <el-form-item label="域编码">
          <el-input v-model="searchForm.code" placeholder="请输入域编码" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">域管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增域
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="domainList" stripe border style="width: 100%">
        <el-table-column type="index" width="50" align="center" />
        <el-table-column prop="name" label="域名称" min-width="150" />
        <el-table-column prop="code" label="域编码" min-width="120" />
        <el-table-column prop="type" label="域类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeType(row.type)">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="systemCount" label="系统数量" width="100" align="center" />
        <el-table-column prop="owner" label="负责人" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button type="primary" link @click="handleViewSystems(row)">
              <el-icon><View /></el-icon>查看系统
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="域名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入域名称" />
        </el-form-item>
        <el-form-item label="域编码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入域编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="域类型" prop="type">
          <el-select v-model="formData.type" placeholder="选择域类型" style="width: 100%">
            <el-option label="核心域" value="core" />
            <el-option label="支撑域" value="support" />
            <el-option label="管理域" value="management" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="owner">
          <el-input v-model="formData.owner" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
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
import { Search, Plus, Edit, Delete, View } from '@element-plus/icons-vue'

const searchForm = reactive({
  name: '',
  code: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const loading = ref(false)
const domainList = ref<any[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  code: '',
  type: 'core',
  owner: '',
  description: ''
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入域名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入域编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择域类型', trigger: 'change' }],
  owner: [{ required: true, message: '请输入负责人', trigger: 'blur' }]
}

const getTypeType = (type: string) => {
  const map: Record<string, any> = {
    core: 'danger',
    support: 'success',
    management: 'warning'
  }
  return map[type] || 'info'
}

const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    core: '核心域',
    support: '支撑域',
    management: '管理域'
  }
  return map[type] || type
}

const loadData = async () => {
  loading.value = true
  try {
    // TODO: 调用API获取数据
    domainList.value = [
      {
        id: 1,
        name: '核心交易域',
        code: 'CORE_TRADING',
        type: 'core',
        description: '核心业务交易相关系统',
        systemCount: 5,
        owner: '张三'
      },
      {
        id: 2,
        name: '客户管理域',
        code: 'CUSTOMER_MGMT',
        type: 'support',
        description: '客户信息管理系统',
        systemCount: 3,
        owner: '李四'
      },
      {
        id: 3,
        name: '基础设施域',
        code: 'INFRASTRUCTURE',
        type: 'management',
        description: '基础运维管理系统',
        systemCount: 4,
        owner: '王五'
      }
    ]
    pagination.total = domainList.value.length
  } catch (error) {
    console.error('加载失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.code = ''
  handleSearch()
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  loadData()
}

const handlePageChange = (val: number) => {
  pagination.page = val
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增域'
  Object.assign(formData, {
    id: undefined,
    name: '',
    code: '',
    type: 'core',
    owner: '',
    description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑域'
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

const handleViewSystems = (row: any) => {
  ElMessage.info(`查看 ${row.name} 下的系统`)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        // TODO: 调用API
        await new Promise(resolve => setTimeout(resolve, 500))
        ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
        dialogVisible.value = false
        loadData()
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除域 "${row.name}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.domain-management {
  .search-card {
    margin-bottom: 16px;

    :deep(.el-card__body) {
      padding-bottom: 0;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
