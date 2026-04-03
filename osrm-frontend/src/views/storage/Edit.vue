<template>
  <div class="storage-edit-page">
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h1 class="page-title">编辑存储后端</h1>
      </div>
    </div>

    <div v-if="loading" class="loading-wrapper">
      <el-skeleton :rows="10" animated />
    </div>

    <el-card v-else-if="formData" class="form-card" shadow="never">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        class="storage-form"
      >
        <el-form-item label="后端编码">
          <el-input :model-value="formData.backendCode" disabled />
          <div class="form-tip">编码创建后不可修改</div>
        </el-form-item>

        <el-form-item label="后端名称" prop="backendName">
          <el-input
            v-model="formData.backendName"
            placeholder="请输入后端名称"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="后端类型">
          <el-tag :type="getTypeTagType(formData.backendType)">
            {{ getTypeLabel(formData.backendType) }}
          </el-tag>
          <div class="form-tip">类型创建后不可修改</div>
        </el-form-item>

        <el-form-item label="服务端点" prop="endpoint">
          <el-input
            v-model="formData.endpoint"
            :placeholder="endpointPlaceholder"
          />
        </el-form-item>

        <el-form-item :label="namespaceLabel" prop="namespace">
          <el-input
            v-model="formData.namespace"
            :placeholder="namespacePlaceholder"
          />
        </el-form-item>

        <!-- Harbor 配置 -->
        <template v-if="formData.backendType === 'HARBOR'">
          <el-form-item label="访问协议">
            <el-radio-group v-model="harborConfig.protocol">
              <el-radio-button label="HTTPS">HTTPS</el-radio-button>
              <el-radio-button label="HTTP">HTTP</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="API 版本">
            <el-input v-model="harborConfig.apiVersion" placeholder="v2.0" />
          </el-form-item>
        </template>

        <!-- Nexus 配置 -->
        <template v-if="formData.backendType === 'NEXUS'">
          <el-form-item label="访问协议">
            <el-radio-group v-model="nexusConfig.protocol">
              <el-radio-button label="HTTPS">HTTPS</el-radio-button>
              <el-radio-button label="HTTP">HTTP</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="Maven 仓库">
            <el-input v-model="nexusConfig.mavenRepo" placeholder="maven-releases" />
          </el-form-item>
          <el-form-item label="NPM 仓库">
            <el-input v-model="nexusConfig.npmRepo" placeholder="npm-releases" />
          </el-form-item>
          <el-form-item label="PyPI 仓库">
            <el-input v-model="nexusConfig.pypiRepo" placeholder="pypi-releases" />
          </el-form-item>
        </template>

        <el-form-item label="Access Key" prop="accessKey">
          <el-input
            v-model="formData.accessKey"
            placeholder="访问用户名（可选）"
          />
        </el-form-item>

        <el-form-item label="Secret Key" prop="secretKey">
          <el-input
            v-model="formData.secretKey"
            type="password"
            placeholder="留空则不修改"
            show-password
          />
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch
            v-model="formData.isDefault"
            active-text="是"
            inactive-text="否"
          />
        </el-form-item>

        <el-form-item label="启用">
          <el-switch
            v-model="formData.enabled"
            active-text="是"
            inactive-text="否"
          />
        </el-form-item>

        <el-form-item label="备注" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            保存
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-empty v-else description="存储后端不存在或已被删除" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { storageApi } from '@/api/storage';
import type { BackendType, HarborConfig, NexusConfig } from '@/types/storage';

interface EditFormData {
  backendCode: string;
  backendName: string;
  backendType: BackendType;
  endpoint: string;
  namespace: string;
  accessKey: string;
  secretKey: string;
  isDefault: boolean;
  enabled: boolean;
  description: string;
}

const router = useRouter();
const route = useRoute();
const formRef = ref<FormInstance>();
const loading = ref(true);
const submitting = ref(false);

const backendId = Number(route.params.id);

const formData = ref<EditFormData | null>(null);

// Harbor config
const harborConfig = reactive<HarborConfig>({
  protocol: 'HTTPS',
  apiVersion: 'v2.0'
});

// Nexus config
const nexusConfig = reactive<NexusConfig>({
  protocol: 'HTTPS',
  mavenRepo: '',
  npmRepo: '',
  pypiRepo: ''
});

const fetchDetail = async () => {
  loading.value = true;
  try {
    const data = await storageApi.getDetail(backendId);
    formData.value = {
      backendCode: data.backendCode,
      backendName: data.backendName,
      backendType: data.backendType,
      endpoint: data.endpoint,
      namespace: data.namespace || '',
      accessKey: data.accessKey || '',
      secretKey: '',
      isDefault: data.isDefault,
      enabled: data.enabled,
      description: data.description || ''
    };

    // Populate config
    if (data.config) {
      if (data.backendType === 'HARBOR') {
        const cfg = data.config as HarborConfig;
        harborConfig.protocol = cfg.protocol || 'HTTPS';
        harborConfig.apiVersion = cfg.apiVersion || 'v2.0';
      } else if (data.backendType === 'NEXUS') {
        const cfg = data.config as NexusConfig;
        nexusConfig.protocol = cfg.protocol || 'HTTPS';
        nexusConfig.mavenRepo = cfg.mavenRepo || '';
        nexusConfig.npmRepo = cfg.npmRepo || '';
        nexusConfig.pypiRepo = cfg.pypiRepo || '';
      }
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '获取详情失败');
  } finally {
    loading.value = false;
  }
};

const formRules: FormRules = {
  backendName: [
    { required: true, message: '请输入后端名称', trigger: 'blur' },
    { min: 2, max: 64, message: '长度在 2 到 64 个字符', trigger: 'blur' }
  ],
  endpoint: [
    { required: true, message: '请输入服务端点', trigger: 'blur' }
  ]
};

const endpointPlaceholder = computed(() => {
  if (!formData.value) return '';
  const map: Record<string, string> = {
    HARBOR: 'https://harbor.example.com',
    NEXUS: 'https://nexus.example.com',
    NAS: '/mnt/storage/osrm'
  };
  return map[formData.value.backendType] || '';
});

const namespaceLabel = computed(() => {
  if (!formData.value) return '命名空间';
  const map: Record<string, string> = {
    HARBOR: 'Harbor 项目',
    NEXUS: '仓库名称',
    NAS: '子目录'
  };
  return map[formData.value.backendType] || '命名空间';
});

const namespacePlaceholder = computed(() => {
  if (!formData.value) return '';
  const map: Record<string, string> = {
    HARBOR: 'library',
    NEXUS: 'maven-releases',
    NAS: 'packages'
  };
  return map[formData.value.backendType] || '';
});

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

const handleSubmit = async () => {
  if (!formRef.value || !formData.value) return;

  try {
    await formRef.value.validate();
    submitting.value = true;

    const payload: Record<string, unknown> = {
      backendName: formData.value.backendName,
      endpoint: formData.value.endpoint,
      namespace: formData.value.namespace,
      accessKey: formData.value.accessKey,
      isDefault: formData.value.isDefault,
      enabled: formData.value.enabled,
      description: formData.value.description
    };

    if (formData.value.secretKey) {
      payload.secretKey = formData.value.secretKey;
    }

    // Add config based on type
    if (formData.value.backendType === 'HARBOR') {
      payload.config = { ...harborConfig };
    } else if (formData.value.backendType === 'NEXUS') {
      payload.config = { ...nexusConfig };
    }

    await storageApi.update(backendId, payload);
    ElMessage.success('存储后端更新成功');
    router.push('/system/storage');
  } catch (error: any) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message);
    } else if (error !== 'validation') {
      ElMessage.error('更新失败，请检查输入');
    }
  } finally {
    submitting.value = false;
  }
};

const handleCancel = () => {
  router.back();
};

onMounted(() => {
  fetchDetail();
});
</script>

<style scoped lang="scss">
.storage-edit-page {
  padding: var(--space-lg);
}

.page-header {
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

.form-card {
  max-width: 800px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);

  :deep(.el-card__body) {
    padding: var(--space-xl);
  }
}

.storage-form {
  .form-tip {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
    margin-top: var(--space-xs);
  }
}
</style>
