<template>
  <div class="storage-create-page">
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h1 class="page-title">新增存储后端</h1>
      </div>
    </div>

    <el-card class="form-card" shadow="never">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        class="storage-form"
      >
        <el-form-item label="后端名称" prop="backendName">
          <el-input
            v-model="formData.backendName"
            placeholder="请输入后端名称，如：生产 Harbor"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="后端类型" prop="backendType">
          <el-radio-group v-model="formData.backendType">
            <el-radio-button label="HARBOR">
              <el-icon><Box /></el-icon>
              Harbor
            </el-radio-button>
            <el-radio-button label="NEXUS">
              <el-icon><Collection /></el-icon>
              Nexus
            </el-radio-button>
            <el-radio-button label="NAS">
              <el-icon><Folder /></el-icon>
              NAS
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="服务端点" prop="endpoint">
          <el-input
            v-model="formData.endpoint"
            :placeholder="endpointPlaceholder"
          />
          <div class="form-tip">
            {{ endpointTip }}
          </div>
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
            placeholder="访问密码或 Token（可选）"
            show-password
          />
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch
            v-model="formData.isDefault"
            active-text="是"
            inactive-text="否"
          />
          <div class="form-tip">
            设为默认后，新软件包将默认使用此存储后端
          </div>
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
            提交
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { ArrowLeft, Box, Collection, Folder } from '@element-plus/icons-vue';
import { storageApi } from '@/api/storage';
import type { StorageBackendFormData, HarborConfig, NexusConfig } from '@/types/storage';

const router = useRouter();
const formRef = ref<FormInstance>();
const submitting = ref(false);

// Form data
const formData = reactive<StorageBackendFormData>({
  backendName: '',
  backendType: 'HARBOR',
  endpoint: '',
  namespace: '',
  accessKey: '',
  secretKey: '',
  isDefault: false,
  description: ''
});

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

// Form validation rules
const formRules: FormRules = {
  backendName: [
    { required: true, message: '请输入后端名称', trigger: 'blur' },
    { min: 2, max: 64, message: '长度在 2 到 64 个字符', trigger: 'blur' }
  ],
  backendType: [
    { required: true, message: '请选择后端类型', trigger: 'change' }
  ],
  endpoint: [
    { required: true, message: '请输入服务端点', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (formData.backendType === 'NAS') {
          // NAS 只需要路径
          callback();
        } else {
          // Harbor/Nexus 需要 URL
          const urlPattern = /^https?:\/\/.+/;
          if (!urlPattern.test(value)) {
            callback(new Error('请输入有效的 URL，以 http:// 或 https:// 开头'));
          } else {
            callback();
          }
        }
      },
      trigger: 'blur'
    }
  ]
};

// Computed placeholders
const endpointPlaceholder = computed(() => {
  const map: Record<string, string> = {
    HARBOR: 'https://harbor.example.com',
    NEXUS: 'https://nexus.example.com',
    NAS: '/mnt/storage/osrm 或 \\nas-server\share'
  };
  return map[formData.backendType] || '';
});

const endpointTip = computed(() => {
  const map: Record<string, string> = {
    HARBOR: 'Harbor 服务的完整 URL 地址',
    NEXUS: 'Nexus 服务的完整 URL 地址',
    NAS: 'NAS 挂载路径或本地存储路径'
  };
  return map[formData.backendType] || '';
});

const namespaceLabel = computed(() => {
  const map: Record<string, string> = {
    HARBOR: 'Harbor 项目',
    NEXUS: '仓库名称',
    NAS: '子目录'
  };
  return map[formData.backendType] || '命名空间';
});

const namespacePlaceholder = computed(() => {
  const map: Record<string, string> = {
    HARBOR: 'library',
    NEXUS: 'maven-releases',
    NAS: 'packages'
  };
  return map[formData.backendType] || '';
});

// Handlers
const handleSubmit = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();
    submitting.value = true;

    const payload = { ...formData } as StorageBackendFormData & { config?: unknown };

    // Add config based on type
    if (formData.backendType === 'HARBOR') {
      payload.config = { ...harborConfig };
    } else if (formData.backendType === 'NEXUS') {
      payload.config = { ...nexusConfig };
    }

    await storageApi.create(payload);
    ElMessage.success('存储后端创建成功');
    router.push('/system/storage');
  } catch (error: any) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message);
    } else if (error !== 'validation') {
      ElMessage.error('创建失败，请检查输入');
    }
  } finally {
    submitting.value = false;
  }
};

const handleCancel = () => {
  router.back();
};
</script>

<style scoped lang="scss">
.storage-create-page {
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
  .el-radio-button {
    :deep(.el-radio-button__inner) {
      display: flex;
      align-items: center;
      gap: var(--space-xs);
      padding: var(--space-sm) var(--space-lg);
    }
  }

  .form-tip {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
    margin-top: var(--space-xs);
  }
}
</style>
