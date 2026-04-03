<template>
  <div class="approval-history">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="软件名称">
          <el-input v-model="searchForm.softwareName" placeholder="请输入软件名称" clearable />
        </el-form-item>
        <el-form-item label="审批结果">
          <el-select v-model="searchForm.result" placeholder="全部" clearable>
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
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
          <span class="title">审批历史</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="historyList" stripe border style="width: 100%">
        <el-table-column type="index" width="50" align="center" />
        <el-table-column prop="applicant" label="申请人" min-width="120" />
        <el-table-column prop="softwareName" label="软件名称" min-width="150" />
        <el-table-column prop="businessSystem" label="业务系统" min-width="150" />
        <el-table-column prop="approver" label="审批人" width="120" />
        <el-table-column prop="approvalTime" label="审批时间" min-width="150" />
        <el-table-column prop="result" label="审批结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result === 'approved' ? 'success' : 'danger'">
              {{ row.result === 'approved' ? '已通过' : '已拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="审批意见" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              <el-icon><View /></el-icon>查看
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
import { ElMessage } from 'element-plus'
import { Search, View } from '@element-plus/icons-vue'

const searchForm = reactive({
  softwareName: '',
  result: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const loading = ref(false)
const historyList = ref<any[]>([])

const loadData = async () => {
  loading.value = true
  try {
    // TODO: 调用API获取数据
    historyList.value = [
      {
        id: 1,
        applicant: '张三',
        softwareName: 'Docker',
        businessSystem: '容器平台',
        approver: '管理员',
        approvalTime: '2026-03-17 16:30',
        result: 'approved',
        comment: '符合使用要求，同意申请'
      },
      {
        id: 2,
        applicant: '李四',
        softwareName: 'MongoDB',
        businessSystem: '数据平台',
        approver: '管理员',
        approvalTime: '2026-03-16 11:20',
        result: 'rejected',
        comment: '已有同类软件，建议使用现有资源'
      }
    ]
    pagination.total = historyList.value.length
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
  searchForm.softwareName = ''
  searchForm.result = ''
  searchForm.dateRange = []
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

const handleView = (row: any) => {
  ElMessage.info('查看审批详情')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.approval-history {
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
