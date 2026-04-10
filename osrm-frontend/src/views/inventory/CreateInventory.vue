<template>
  <div class="inventory-create">
    <el-card class="create-card">
      <template #header>
        <div class="card-header">
          <h2>存量软件登记</h2>
          <el-text type="info">登记存量系统已使用的开源软件</el-text>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        class="inventory-form"
      >
        <el-form-item label="软件名称" prop="packageName">
          <el-input
            v-model="form.packageName"
            placeholder="请输入软件名称，如：MySQL"
            clearable
          />
        </el-form-item>

        <el-form-item label="关联软件包">
          <el-select
            v-model="form.packageId"
            clearable
            placeholder="选择已入库软件包（可选）"
            style="width: 100%"
          >
            <el-option
              v-for="pkg in packageList"
              :key="pkg.id"
              :label="pkg.packageName"
              :value="pkg.id"
            />
          </el-select>
          <el-text type="info" size="small">如软件已在系统中，可选择关联</el-text>
        </el-form-item>

        <el-form-item label="版本号">
          <el-input
            v-model="form.versionNo"
            placeholder="如：8.0.32"
            clearable
          />
        </el-form-item>

        <el-form-item label="软件类型">
          <el-select
            v-model="form.softwareType"
            placeholder="选择类型"
            style="width: 100%"
          >
            <el-option label="Docker镜像" value="DOCKER_IMAGE" />
            <el-option label="Maven包" value="MAVEN" />
            <el-option label="NPM包" value="NPM" />
            <el-option label="其他" value="GENERIC" />
          </el-select>
        </el-form-item>

        <el-form-item label="业务系统">
          <el-select
            v-model="form.businessSystemId"
            placeholder="选择业务系统"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="system in systemList"
              :key="system.id"
              :label="system.systemName"
              :value="system.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="部署环境">
          <el-select
            v-model="form.deployEnvironment"
            placeholder="选择环境"
            style="width: 100%"
          >
            <el-option label="生产环境" value="PRODUCTION" />
            <el-option label="测试环境" value="TESTING" />
            <el-option label="开发环境" value="DEVELOPMENT" />
          </el-select>
        </el-form-item>

        <el-form-item label="服务器数量">
          <el-input-number v-model="form.serverCount" :min="1" :max="9999" />
        </el-form-item>

        <el-form-item label="使用场景">
          <el-input
            v-model="form.usageScenario"
            type="textarea"
            :rows="3"
            placeholder="描述该软件的使用场景、用途等"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.remarks"
            type="textarea"
            :rows="2"
            placeholder="其他需要说明的信息"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submit" :loading="submitting">
            提交登记
          </el-button>
          <el-button @click="reset">重置</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { inventoryApi, type InventoryForm } from '@/api/inventory';
import { softwareApi, type SoftwarePackage } from '@/api/software';
import { businessApi, type BusinessSystem } from '@/api/business';

const router = useRouter();
const formRef = ref<FormInstance>();
const submitting = ref(false);
const packageList = ref<SoftwarePackage[]>([]);
const systemList = ref<BusinessSystem[]>([]);

const form = reactive<InventoryForm>({
  packageName: '',
  packageId: undefined,
  versionNo: '',
  softwareType: '',
  responsiblePerson: '',
  businessSystemId: undefined,
  deployEnvironment: '',
  serverCount: 1,
  usageScenario: '',
  remarks: ''
});

const rules: FormRules = {
  packageName: [
    { required: true, message: '请输入软件名称', trigger: 'blur' },
    { max: 128, message: '长度不能超过128个字符', trigger: 'blur' }
  ]
};

const loadPackages = async () => {
  try {
    const res = await softwareApi.list({ page: 1, size: 1000 });
    packageList.value = res.content || [];
  } catch (error) {
    console.error('加载软件包失败', error);
  }
};

const loadSystems = async () => {
  try {
    const res = await businessApi.list({ page: 1, size: 1000 });
    systemList.value = res.content || [];
  } catch (error) {
    console.error('加载业务系统失败', error);
  }
};

const submit = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        await inventoryApi.create(form);
        ElMessage.success('登记成功，等待审批');
        router.push('/inventory/my');
      } catch (error) {
        console.error('登记失败', error);
        ElMessage.error('登记失败，请重试');
      } finally {
        submitting.value = false;
      }
    }
  });
};

const reset = () => {
  formRef.value?.resetFields();
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  loadPackages();
  loadSystems();
});
</script>

<style scoped>
.inventory-create {
  padding: 20px;
}

.create-card {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-header h2 {
  margin: 0;
}

.inventory-form {
  padding: 20px 0;
}
</style>
