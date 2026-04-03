<template>
  <div class="storage-detail-page">
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h1 class="page-title">存储后端详情</h1>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="loading-wrapper">
      <el-skeleton :rows="10" animated />
    </div>

    <template v-else-if="backend">
      <!-- Basic Info Card -->
      <el-card class="detail-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <div class="header-tags">
              <el-tag v-if="backend.isDefault" type="success">默认</el-tag>
              <el-tag :type="backend.enabled ? 'success' : 'info'">
                {{ backend.enabled ? '启用' : '停用' }}
              </el-tag>
            </div>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="后端名称">{{ backend.backendName }}</el-descriptions-item>
          <el-descriptions-item label="后端编码">{{ backend.backendCode }}</el-descriptions-item>
          <el-descriptions-item label="后端类型">
            <el-tag :type="getTypeTagType(backend.backendType)">
              {{ getTypeLabel(backend.backendType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="健康状态">
            <el-tag :type="getHealthStatusType(backend.healthStatus)">
              <el-icon v-if="backend.healthStatus === 'ONLINE'"><CircleCheck /></el-icon>
              <el-icon v-else-if="backend.healthStatus === 'OFFLINE' || backend.healthStatus === 'ERROR'"><CircleClose /></el-icon>
              <el-icon v-else><QuestionFilled /></el-icon>
              {{ getHealthStatusText(backend.healthStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="服务端点">{{ backend.endpoint }}</el-descriptions-item>
          <el-descriptions-item label="Access Key">{{ backend.accessKey || '-' }}</el-descriptions-item>
          <el-descriptions-item label="命名空间">{{ backend.namespace || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后检查">{{ formatDateTime(backend.lastHealthCheck) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Connection Info Card -->
      <el-card class="detail-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>连接信息</span>
            <el-button type="primary" size="small" :loading="checking" @click="handleHealthCheck">
              <el-icon><Refresh /></el-icon>
              健康检查
            </el-button>
          </div>
        </template>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="服务端点">
            <el-link :href="backend.endpoint" target="_blank" type="primary">
              {{ backend.endpoint }}
              <el-icon><Link /></el-icon>
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="访问密钥">{{ maskSecret(backend.accessKey) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getHealthStatusType(backend.healthStatus)">
              {{ getHealthStatusText(backend.healthStatus) }}
            </el-tag>
            <span v-if="checkResult" class="check-result">
              (响应时间: {{ checkResult.responseTimeMs }}ms)
            </span>
          </el-descriptions-item>
          <el-descriptions-item v-if="backend.errorMessage" label="错误信息">
            <span class="error-text">{{ backend.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Description Card -->
      <el-card class="detail-card" shadow="never">
        <template #header>
          <span>备注</span>
        </template>
        <p class="description">{{ backend.description || '暂无备注' }}</p>
      </el-card>

      <!-- Meta Info -->
      <el-card class="detail-card" shadow="never">
        <template #header>
          <span>元信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="创建时间">{{ formatDateTime(backend.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(backend.updatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ backend.createdBy || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </template>

    <el-empty v-else description="存储后端不存在或已被删除" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Edit, CircleCheck, CircleClose, QuestionFilled, Refresh, Link } from '@element-plus/icons-vue';
import { storageApi } from '@/api/storage';
import type { StorageBackend, HealthStatus } from '@/types/storage';

const router = useRouter();
const route = useRoute();

const loading = ref(false);
const checking = ref(false);
const backend = ref<StorageBackend | null>(null);
const checkResult = ref<{ responseTimeMs: number } | null>(null);

const backendId = Number(route.params.id);

const fetchDetail = async () => {
  loading.value = true;
  try {
    const res = await storageApi.getDetail(backendId);
    backend.value = res;
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '获取详情失败');
  } finally {
    loading.value = false;
  }
};

const handleEdit = () => {
  router.push(`/system/storage/${backendId}/edit`);
};

const handleHealthCheck = async () => {
  checking.value = true;
  try {
    const res = await storageApi.healthCheck(backendId);
    if (backend.value) {
      backend.value.healthStatus = res.healthStatus as HealthStatus;
      backend.value.lastHealthCheck = res.lastHealthCheck;
      backend.value.errorMessage = res.errorMessage;
    }
    checkResult.value = { responseTimeMs: res.responseTimeMs };
    ElMessage.success(`健康检查完成 (响应时间: ${res.responseTimeMs}ms)`);
  } catch (error: any) {
    ElMessage.error('健康检查失败');
  } finally {
    checking.value = false;
  }
};

const getTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    HARBOR: 'primary',
    NEXUS: 'success',
    NAS: 'warning'
  };
  return map[type] || 'info';
};

const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    HARBOR: 'Harbor',
    NEXUS: 'Nexus',
    NAS: 'NAS'
  };
  return map[type] || type;
};

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

const formatDateTime = (date: string | undefined) => {
  if (!date) return '-';
  return new Date(date).toLocaleString('zh-CN');
};

const maskSecret = (secret: string | undefined) => {
  if (!secret) return '-';
  if (secret.length <= 4) return '****';
  return secret.substring(0, 2) + '****' + secret.substring(secret.length - 2);
};

onMounted(() => {
  fetchDetail();
});
</script>

<style scoped lang="scss">
.storage-detail-page {
  padding: var(--space-lg);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);

  .header-left {
    display: flex;
    align-items: center;
    gap: var(--space-md);

    .page-title {
      font-size: var(--font-size-xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
      margin: 0;
    }
  }
}

.loading-wrapper {
  padding: var(--space-4xl);
}

.detail-card {
  margin-bottom: var(--space-lg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-tags {
      display: flex;
      gap: var(--space-sm);
    }
  }
}

.check-result {
  margin-left: var(--space-sm);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
}

.error-text {
  color: var(--color-danger);
}

.description {
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0;
}
</style>
