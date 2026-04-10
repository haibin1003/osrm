<template>
  <div class="pending-inventory">
    <el-card>
      <template #header>
        <h2>存量登记审批</h2>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="recordNo" label="登记编号" width="160" />
        <el-table-column prop="packageName" label="软件名称" />
        <el-table-column prop="versionNo" label="版本号" width="100" />
        <el-table-column prop="responsiblePerson" label="负责人" width="100" />
        <el-table-column prop="businessSystemName" label="业务系统" width="120" />
        <el-table-column prop="deployEnvironment" label="环境" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.deployEnvironment === 'PRODUCTION'" type="danger">生产</el-tag>
            <el-tag v-else-if="row.deployEnvironment === 'TESTING'" type="warning">测试</el-tag>
            <el-tag v-else-if="row.deployEnvironment === 'DEVELOPMENT'" type="info">开发</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="登记时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link @click="approve(row)">批准</el-button>
            <el-button type="danger" link @click="openRejectDialog(row)">驳回</el-button>
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
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

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectVisible" title="驳回原因" width="500px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        placeholder="请输入驳回原因"
        maxlength="256"
        show-word-limit
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject" :loading="submitting">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="存量登记详情" width="600px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="登记编号">{{ currentRow.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="warning">{{ currentRow.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="软件名称" :span="2">{{ currentRow.packageName }}</el-descriptions-item>
        <el-descriptions-item label="版本号">{{ currentRow.versionNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="软件类型">{{ currentRow.softwareType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ currentRow.responsiblePerson }}</el-descriptions-item>
        <el-descriptions-item label="业务系统">{{ currentRow.businessSystemName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部署环境">{{ currentRow.deployEnvironment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务器数量">{{ currentRow.serverCount }}</el-descriptions-item>
        <el-descriptions-item label="使用场景" :span="2">{{ currentRow.usageScenario || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登记时间">{{ formatDate(currentRow.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { inventoryApi, type InventoryRecord } from '@/api/inventory';

const loading = ref(false);
const submitting = ref(false);
const tableData = ref<InventoryRecord[]>([]);
const total = ref(0);
const rejectVisible = ref(false);
const detailVisible = ref(false);
const rejectReason = ref('');
const currentRow = ref<InventoryRecord | null>(null);

const queryForm = reactive({
  page: 1,
  size: 10
});

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString('zh-CN');
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await inventoryApi.getPendingList({
      page: queryForm.page,
      size: queryForm.size
    });
    tableData.value = res.content || [];
    total.value = res.totalElements || 0;
  } catch (error) {
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

const approve = async (row: InventoryRecord) => {
  try {
    await ElMessageBox.confirm('确认批准此存量登记？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await inventoryApi.approve(row.id);
    ElMessage.success('批准成功');
    loadData();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('批准失败');
    }
  }
};

const openRejectDialog = (row: InventoryRecord) => {
  currentRow.value = row;
  rejectReason.value = '';
  rejectVisible.value = true;
};

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因');
    return;
  }
  submitting.value = true;
  try {
    await inventoryApi.reject(currentRow.value!.id, rejectReason.value);
    ElMessage.success('驳回成功');
    rejectVisible.value = false;
    loadData();
  } catch (error) {
    ElMessage.error('驳回失败');
  } finally {
    submitting.value = false;
  }
};

const viewDetail = (row: InventoryRecord) => {
  currentRow.value = row;
  detailVisible.value = true;
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.pending-inventory {
  padding: 20px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
