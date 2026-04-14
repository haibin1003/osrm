<template>
  <div class="inventory-manage">
    <div class="table-card stripe-card">
      <div class="table-header">
        <h2 class="table-title">存量管理设置</h2>
        <p class="table-subtitle">配置存量登记功能参数</p>
      </div>

      <div class="settings-section">
        <el-form label-width="200px">
          <el-form-item label="启用存量登记功能">
            <el-switch
              v-model="settings.enableInventoryFeature"
              active-text="开启"
              inactive-text="关闭"
            />
            <el-text type="info" class="form-tip">
              关闭后，开发人员将无法进行存量登记
            </el-text>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="saveSettings" :loading="saving">
              保存设置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-divider class="section-divider" />

      <div class="section-header">
        <h3 class="section-title">全部存量登记</h3>
      </div>

      <div class="query-form-wrapper">
        <el-form :model="queryForm" inline class="query-form">
          <el-form-item label="用户ID">
            <el-input-number v-model="queryForm.userId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryForm.status" placeholder="全部状态" clearable>
              <el-option label="待审批" value="PENDING" />
              <el-option label="已批准" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
          </el-form-item>
          <el-form-item label="软件名称">
            <el-input v-model="queryForm.packageName" placeholder="模糊查询" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="table-toolbar">
        <el-table :data="tableData" v-loading="loading" stripe>
          <el-table-column prop="recordNo" label="登记编号" width="160" />
          <el-table-column prop="userId" label="登记人ID" width="100" />
          <el-table-column prop="packageName" label="软件名称" />
          <el-table-column prop="versionNo" label="版本号" width="100" />
          <el-table-column prop="businessSystemName" label="业务系统" width="120" />
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
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="danger" link @click="deleteRow(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { inventoryApi, type InventoryRecord, type InventorySettings } from '@/api/inventory';

const loading = ref(false);
const saving = ref(false);
const tableData = ref<InventoryRecord[]>([]);
const total = ref(0);

const settings = reactive<InventorySettings>({
  enableInventoryFeature: false
});

const queryForm = reactive({
  userId: undefined as number | undefined,
  status: '',
  packageName: '',
  businessSystemId: undefined as number | undefined,
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
  return new Date(dateStr).toLocaleString('zh-CN');
};

const loadSettings = async () => {
  try {
    const res = await inventoryApi.getSettings();
    settings.enableInventoryFeature = res.enableInventoryFeature;
  } catch (error) {
    ElMessage.error('加载设置失败');
  }
};

const saveSettings = async () => {
  saving.value = true;
  try {
    await inventoryApi.updateSettings(settings);
    ElMessage.success('保存成功');
  } catch (error) {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await inventoryApi.getAllList({
      userId: queryForm.userId,
      status: queryForm.status || undefined,
      packageName: queryForm.packageName || undefined,
      businessSystemId: queryForm.businessSystemId,
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

const resetQuery = () => {
  queryForm.userId = undefined;
  queryForm.status = '';
  queryForm.packageName = '';
  queryForm.businessSystemId = undefined;
  queryForm.page = 1;
  loadData();
};

const deleteRow = async (row: InventoryRecord) => {
  try {
    await ElMessageBox.confirm('确认删除此存量登记记录？', '警告', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await inventoryApi.delete(row.id);
    ElMessage.success('删除成功');
    loadData();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
    }
  }
};

onMounted(() => {
  loadSettings();
  loadData();
});
</script>

<style scoped lang="scss">
.inventory-manage {
  padding: var(--space-xl) 0;
  max-width: 1280px;
  margin: 0 auto;
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

.table-header {
  padding: var(--space-xl) var(--space-2xl);
  border-bottom: 1px solid var(--color-border-light);

  .table-title {
    font-size: var(--font-size-xl);
    font-weight: var(--font-weight-medium);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-xs);
  }

  .table-subtitle {
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    margin: 0;
    font-weight: var(--font-weight-light);
  }
}

.settings-section {
  padding: var(--space-xl) var(--space-2xl);
}

.form-tip {
  margin-left: var(--space-md);
}

.section-divider {
  margin: 0 var(--space-2xl);
}

.section-header {
  padding: var(--space-xl) var(--space-2xl) var(--space-lg);

  .section-title {
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-medium);
    color: var(--color-text-primary);
    margin: 0;
  }
}

.query-form-wrapper {
  padding: 0 var(--space-2xl);
}

.table-toolbar {
  padding: var(--space-lg) var(--space-2xl);
}

.pagination-wrapper {
  padding: var(--space-lg) var(--space-2xl);
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--color-border-light);
}
</style>
