<template>
  <div class="my-inventory">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>我的存量登记</h2>
          <el-button type="primary" @click="goToCreate">新增登记</el-button>
        </div>
      </template>

      <el-form :model="queryForm" inline class="query-form">
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable>
            <el-option label="待审批" value="PENDING" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="recordNo" label="登记编号" width="160" />
        <el-table-column prop="packageName" label="软件名称" />
        <el-table-column prop="versionNo" label="版本号" width="100" />
        <el-table-column prop="businessSystemName" label="业务系统" width="120" />
        <el-table-column prop="deployEnvironment" label="环境" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.deployEnvironment === 'PRODUCTION'" type="danger">生产</el-tag>
            <el-tag v-else-if="row.deployEnvironment === 'TESTING'" type="warning">测试</el-tag>
            <el-tag v-else-if="row.deployEnvironment === 'DEVELOPMENT'" type="info">开发</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="登记时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">查看</el-button>
            <el-button v-if="row.status === 'PENDING'" type="primary" link @click="editRow(row)">编辑</el-button>
            <el-button v-if="row.status === 'REJECTED'" type="danger" link @click="viewRejectReason(row)">驳回原因</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="存量登记详情" width="600px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="登记编号">{{ currentRow.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRow.status)">{{ currentRow.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="软件名称" :span="2">{{ currentRow.packageName }}</el-descriptions-item>
        <el-descriptions-item label="版本号">{{ currentRow.versionNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="软件类型">{{ currentRow.softwareType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务系统">{{ currentRow.businessSystemName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部署环境">{{ currentRow.deployEnvironment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务器数量">{{ currentRow.serverCount }}</el-descriptions-item>
        <el-descriptions-item label="使用场景" :span="2">{{ currentRow.usageScenario || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登记时间">{{ formatDate(currentRow.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="审批时间" v-if="currentRow.approvedAt">
          {{ formatDate(currentRow.approvedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="驳回原因" :span="2" v-if="currentRow.rejectReason">
          <span style="color: #f56c6c">{{ currentRow.rejectReason }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { inventoryApi, type InventoryRecord } from '@/api/inventory';

const router = useRouter();
const loading = ref(false);
const tableData = ref<InventoryRecord[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const currentRow = ref<InventoryRecord | null>(null);

const queryForm = reactive({
  status: '',
  page: 1,
  size: 10
});

const getStatusType = (status: string) => {
  switch (status) {
    case 'PENDING': return 'warning';
    case 'APPROVED': return 'success';
    case 'REJECTED': return 'danger';
    default: return 'info';
  }
};

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN');
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await inventoryApi.getMyList({
      status: queryForm.status || undefined,
      page: queryForm.page,
      size: queryForm.size
    });
    tableData.value = res.content || [];
    total.value = res.totalElements || 0;
  } catch (error) {
    console.error('加载数据失败', error);
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  queryForm.status = '';
  queryForm.page = 1;
  loadData();
};

const goToCreate = () => {
  router.push('/inventory/create');
};

const viewDetail = (row: InventoryRecord) => {
  currentRow.value = row;
  detailVisible.value = true;
};

const editRow = (row: InventoryRecord) => {
  router.push({
    path: '/inventory/edit',
    query: { id: row.id }
  });
};

const viewRejectReason = (row: InventoryRecord) => {
  currentRow.value = row;
  detailVisible.value = true;
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.my-inventory {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
}

.query-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
