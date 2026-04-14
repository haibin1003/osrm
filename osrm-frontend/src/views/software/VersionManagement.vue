<template>
  <div class="version-management">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="软件包">
          <el-select v-model="searchForm.packageId" placeholder="选择软件包" clearable style="width: 200px">
            <el-option label="Node.js" value="1" />
            <el-option label="MySQL" value="2" />
            <el-option label="Redis" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="searchForm.version" placeholder="请输入版本号" clearable />
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
          <span class="title">版本管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增版本
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="versionList" stripe border style="width: 100%">
        <el-table-column type="index" width="50" align="center" />
        <el-table-column prop="packageName" label="软件包" min-width="150" />
        <el-table-column prop="version" label="版本号" width="120" />
        <el-table-column prop="releaseDate" label="发布日期" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'stable' ? 'success' : 'warning'">
              {{ row.status === 'stable' ? '稳定版' : '测试版' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载次数" width="100" align="center" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button type="primary" link @click="handleDownload(row)">
              <el-icon><Download /></el-icon>下载
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Download } from '@element-plus/icons-vue'

const searchForm = reactive({
  packageId: '',
  version: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const loading = ref(false)
const versionList = ref<any[]>([])

const loadData = async () => {
  loading.value = true
  try {
    // TODO: 调用API获取数据
    versionList.value = []
    pagination.total = 0
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
  searchForm.packageId = ''
  searchForm.version = ''
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
  ElMessage.info('新增版本功能开发中')
}

const handleEdit = (row: any) => {
  ElMessage.info('编辑功能开发中')
}

const handleDownload = (row: any) => {
  ElMessage.success(`开始下载 ${row.packageName} ${row.version}`)
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除此版本吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.version-management {
  background: #f6f9fc;
  min-height: calc(100vh - 120px);
  padding: var(--space-xl);

  .search-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: 10px;
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
  }

  .table-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: 10px;
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

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title {
        font-size: var(--font-size-lg);
        font-weight: var(--font-weight-light);
      }
    }
  }

  .pagination-wrapper {
    margin-top: var(--space-lg);
    display: flex;
    justify-content: flex-end;
    padding: var(--space-md) var(--space-lg);
    border-top: 1px solid var(--color-border-light);
  }
}
</style>
