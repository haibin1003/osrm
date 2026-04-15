<template>
  <div class="storage-backend-page">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">存储配置</h1>
        <p class="page-subtitle">管理 Harbor、Nexus、NAS 等存储后端</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增存储后端
      </el-button>
    </div>

    <!-- Filter Area -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="queryParams" inline>
        <el-form-item label="名称">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索后端名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryParams.type" placeholder="全部类型" clearable>
            <el-option label="Harbor" value="HARBOR" />
            <el-option label="Nexus" value="NEXUS" />
            <el-option label="NAS" value="NAS" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
            <el-option label="在线" value="ONLINE" />
            <el-option label="离线" value="OFFLINE" />
            <el-option label="错误" value="ERROR" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshRight /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Storage Backend Cards -->
    <div class="storage-grid">
      <template v-if="loading">
        <el-card v-for="i in 4" :key="i" class="storage-card skeleton" shadow="hover">
          <el-skeleton :rows="3" animated />
        </el-card>
      </template>

      <template v-else-if="backendList.length === 0">
        <el-empty description="暂无存储后端配置" class="empty-state">
          <el-button type="primary" @click="handleCreate">新增存储后端</el-button>
        </el-empty>
      </template>

      <el-card
        v-for="backend in backendList"
        :key="backend.id"
        class="storage-card"
        shadow="hover"
        :class="{ 'is-default': backend.isDefault, 'is-disabled': !backend.enabled }"
      >
        <div class="card-header">
          <div class="backend-icon" :class="`type-${backend.backendType.toLowerCase()}`">
            <el-icon :size="24">
              <Box v-if="backend.backendType === 'HARBOR'" />
              <Collection v-else-if="backend.backendType === 'NEXUS'" />
              <Folder v-else />
            </el-icon>
          </div>
          <div class="header-actions">
            <el-tag v-if="backend.isDefault" type="success" size="small">默认</el-tag>
            <el-switch
              v-model="backend.enabled"
              size="small"
              @change="(val: boolean) => handleEnableChange(backend, val)"
            />
          </div>
        </div>

        <div class="card-body">
          <h3 class="backend-name">{{ backend.backendName }}</h3>
          <p class="backend-code">{{ backend.backendCode }}</p>
          <p class="backend-endpoint" :title="backend.endpoint">{{ backend.endpoint }}</p>
        </div>

        <div class="card-footer">
          <div class="health-status">
            <el-tag
              :type="getHealthStatusType(backend.healthStatus)"
              size="small"
              effect="light"
            >
              <el-icon v-if="backend.healthStatus === 'ONLINE'"><CircleCheck /></el-icon>
              <el-icon v-else-if="backend.healthStatus === 'OFFLINE' || backend.healthStatus === 'ERROR'"><CircleClose /></el-icon>
              <el-icon v-else><QuestionFilled /></el-icon>
              {{ getHealthStatusText(backend.healthStatus) }}
            </el-tag>
            <el-button
              v-if="backend.enabled"
              link
              type="primary"
              size="small"
              :loading="healthChecking[backend.id]"
              @click="handleHealthCheck(backend)"
            >
              检查
            </el-button>
          </div>
          <div class="card-actions">
            <el-button link type="primary" @click="handleView(backend)">查看</el-button>
            <el-button link type="primary" @click="handleEdit(backend)">编辑</el-button>
            <el-dropdown trigger="click">
              <el-button link type="primary">
                <el-icon><More /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleSetDefault(backend)" :disabled="backend.isDefault">
                    设为默认
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleDelete(backend)" type="danger">
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Pagination -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[8, 12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Plus,
  Search,
  RefreshRight,
  Box,
  Collection,
  Folder,
  CircleCheck,
  CircleClose,
  QuestionFilled,
  More
} from '@element-plus/icons-vue';
import { storageApi } from '@/api/storage';
import type { StorageBackend, BackendQueryParams, HealthStatus } from '@/types/storage';

const router = useRouter();

// Loading states
const loading = ref(false);
const healthChecking = ref<Record<number, boolean>>({});

// Data
const backendList = ref<StorageBackend[]>([]);
const total = ref(0);

// Query parameters - backend uses 1-based page numbering
const queryParams = reactive<BackendQueryParams>({
  keyword: '',
  type: undefined,
  status: undefined,
  page: 1,
  size: 10
});

// Fetch data
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await storageApi.getList(queryParams);
    backendList.value = res.content;
    total.value = res.totalElements;
  } catch (error: any) {
    ElMessage.error('获取存储后端列表失败');
  } finally {
    loading.value = false;
  }
};

// Search handlers
const handleSearch = () => {
  queryParams.page = 1;
  fetchData();
};

const handleReset = () => {
  queryParams.keyword = '';
  queryParams.type = undefined;
  queryParams.status = undefined;
  queryParams.page = 1;
  fetchData();
};

const handleSizeChange = (val: number) => {
  queryParams.size = val;
  fetchData();
};

const handlePageChange = (val: number) => {
  queryParams.page = val;
  fetchData();
};

// Action handlers
const handleCreate = () => {
  router.push('/system/storage/create');
};

const handleView = (backend: StorageBackend) => {
  router.push(`/system/storage/${backend.id}`);
};

const handleEdit = (backend: StorageBackend) => {
  router.push(`/system/storage/${backend.id}/edit`);
};

const handleDelete = async (backend: StorageBackend) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除存储后端 "${backend.backendName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    await storageApi.delete(backend.id);
    ElMessage.success('删除成功');
    fetchData();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败');
    }
  }
};

const handleSetDefault = async (backend: StorageBackend) => {
  try {
    await storageApi.setDefault(backend.id);
    ElMessage.success('已设为默认存储后端');
    fetchData();
  } catch (error) {
    ElMessage.error('设置失败');
  }
};

const handleEnableChange = async (backend: StorageBackend, enabled: boolean) => {
  try {
    await storageApi.setEnabled(backend.id, enabled);
    ElMessage.success(enabled ? '已启用' : '已停用');
    backend.enabled = enabled;
  } catch (error) {
    ElMessage.error('状态更新失败');
    backend.enabled = !enabled;
  }
};

const handleHealthCheck = async (backend: StorageBackend) => {
  healthChecking.value[backend.id] = true;
  try {
    const res = await storageApi.healthCheck(backend.id);
    backend.healthStatus = res.healthStatus as HealthStatus;
    const msg = res.errorMessage || '健康检查完成';
    ElMessage.success(`${msg} (响应时间: ${res.responseTimeMs}ms)`);
  } catch (error: any) {
    backend.healthStatus = 'OFFLINE';
    ElMessage.error('健康检查失败');
  } finally {
    healthChecking.value[backend.id] = false;
  }
};

// Helpers
const getHealthStatusType = (status: HealthStatus | undefined) => {
  if (!status) return 'info';
  const map: Record<string, string> = {
    ONLINE: 'success',
    OFFLINE: 'danger',
    ERROR: 'danger',
    UNKNOWN: 'info'
  };
  return map[status] || 'info';
};

const getHealthStatusText = (status: HealthStatus | undefined) => {
  if (!status) return '未知';
  const map: Record<string, string> = {
    ONLINE: '在线',
    OFFLINE: '离线',
    ERROR: '错误',
    UNKNOWN: '未知'
  };
  return map[status] || '未知';
};

// Initialize
onMounted(() => {
  fetchData();
});
</script>

<style scoped lang="scss">
.storage-backend-page {
  padding: var(--space-lg);
  background: #f6f9fc;
  min-height: 100vh;
}

/* Stripe gradient bar */
.storage-backend-page::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(135deg, #635bff, #a259ff);
  z-index: 1000;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;

  .page-title {
    font-size: 24px;
    font-weight: 300;
    color: #1f2937;
    margin: 0 0 4px;
  }

  .page-subtitle {
    font-size: 14px;
    font-weight: 300;
    color: #6b7280;
    margin: 0;
  }
}

.filter-card {
  margin-bottom: 24px;
  border-radius: 10px;

  :deep(.el-card__body) {
    padding: 16px 20px;
  }
}

.storage-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-bottom: 24px;

  .empty-state {
    grid-column: 1 / -1;
    padding: 60px 0;
  }
}

.storage-card {
  position: relative;
  transition: all 0.3s ease;
  border-radius: 10px;
  font-weight: 300;

  &.is-default {
    border: 1px solid #67c23a;
    background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%);
  }

  &.is-disabled {
    opacity: 0.7;

    .backend-name,
    .backend-code,
    .backend-endpoint {
      color: #9ca3af;
    }
  }

  &.skeleton {
    :deep(.el-skeleton) {
      padding: 16px;
    }
  }

  :deep(.el-card__body) {
    padding: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16px;

    .backend-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;

      &.type-harbor {
        background: #dbeafe;
        color: #2563eb;
      }

      &.type-nexus {
        background: #dcfce7;
        color: #16a34a;
      }

      &.type-nas {
        background: #f3e8ff;
        color: #9333ea;
      }
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .card-body {
    margin-bottom: 16px;

    .backend-name {
      font-size: 16px;
      font-weight: 300;
      color: #1f2937;
      margin: 0 0 4px;
    }

    .backend-code {
      font-size: 12px;
      font-weight: 300;
      color: #6b7280;
      margin: 0 0 8px;
      font-family: monospace;
    }

    .backend-endpoint {
      font-size: 13px;
      font-weight: 300;
      color: #9ca3af;
      margin: 0;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 16px;
    border-top: 1px solid #e5e7eb;

    .health-status {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .card-actions {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}
</style>
