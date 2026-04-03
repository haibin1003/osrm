<template>
  <div class="apply-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">申请订购</h1>
        <p class="page-subtitle">选择软件和版本，关联业务系统后提交申请</p>
      </div>
    </div>

    <el-card shadow="never" class="form-card">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="软件包" prop="packageId">
          <el-select v-model="formData.packageId" placeholder="请选择软件包" filterable style="width: 100%"
            @change="onPackageChange" :loading="loadingPackages">
            <el-option v-for="p in publishedPackages" :key="p.id"
              :label="p.packageName" :value="p.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="版本" prop="versionId">
          <el-select v-model="formData.versionId" placeholder="请选择版本" style="width: 100%"
            :disabled="!formData.packageId" :loading="loadingVersions">
            <el-option v-for="v in versions" :key="v.id"
              :label="v.versionNo" :value="v.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="业务系统" prop="businessSystemId">
          <el-select v-model="formData.businessSystemId" placeholder="请选择业务系统" filterable style="width: 100%"
            :loading="loadingSystems">
            <el-option v-for="s in businessSystems" :key="s.id"
              :label="`${s.systemName} (${s.systemCode})`" :value="s.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="使用场景" prop="useScene">
          <el-input v-model="formData.useScene" type="textarea" :rows="3"
            placeholder="请描述使用场景，如：用于订单系统的消息队列服务" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { softwareApi } from '@/api/software'
import { businessApi } from '@/api/business'
import { subscriptionApi } from '@/api/subscription'
import type { SoftwarePackage, SoftwareVersion } from '@/types/software'
import type { BusinessSystem } from '@/types/business'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const loadingPackages = ref(true)
const loadingVersions = ref(false)
const loadingSystems = ref(true)

const publishedPackages = ref<SoftwarePackage[]>([])
const versions = ref<SoftwareVersion[]>([])
const businessSystems = ref<BusinessSystem[]>([])

const formData = reactive({
  packageId: null as number | null,
  versionId: null as number | null,
  businessSystemId: null as number | null,
  useScene: ''
})

const formRules: FormRules = {
  packageId: [{ required: true, message: '请选择软件包', trigger: 'change' }],
  versionId: [{ required: true, message: '请选择版本', trigger: 'change' }],
  businessSystemId: [{ required: true, message: '请选择业务系统', trigger: 'change' }],
  useScene: [{ required: true, message: '请填写使用场景', trigger: 'blur' }]
}

const onPackageChange = async (packageId: number) => {
  formData.versionId = null
  loadingVersions.value = true
  try {
    versions.value = await softwareApi.getVersions(packageId)
  } catch { versions.value = [] }
  finally { loadingVersions.value = false }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    await subscriptionApi.apply({
      packageId: formData.packageId!,
      versionId: formData.versionId!,
      businessSystemId: formData.businessSystemId!,
      useScene: formData.useScene
    })
    ElMessage.success('订购申请已提交')
    router.push('/subscription/my')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally { submitting.value = false }
}

onMounted(async () => {
  // 如果有预选参数
  const prePackageId = route.query.packageId ? Number(route.query.packageId) : null
  const preVersionId = route.query.versionId ? Number(route.query.versionId) : null

  try {
    const pkgRes = await softwareApi.list({ status: 'PUBLISHED', page: 1, size: 100 })
    publishedPackages.value = pkgRes.content
  } catch { /* ignore - no published packages */ }
  finally { loadingPackages.value = false }

  // 业务系统需要 business-system:read 权限，分开调用避免整体失败
  try {
    const sysRes = await businessApi.list({ enabled: true, page: 1, size: 100 })
    businessSystems.value = sysRes.content
  } catch { /* ignore - user may not have permission */ }
  finally { loadingSystems.value = false }

  if (prePackageId) {
    formData.packageId = prePackageId
    await onPackageChange(prePackageId)
    if (preVersionId) formData.versionId = preVersionId
  }
})
</script>

<style scoped lang="scss">
.apply-page {
  .page-header { margin-bottom: var(--space-2xl);
    .page-title { font-size: var(--font-size-4xl); font-weight: var(--font-weight-bold); margin: 0; }
    .page-subtitle { font-size: var(--font-size-md); color: var(--color-text-secondary); margin: var(--space-xs) 0 0; }
  }
  .form-card { max-width: 700px; }
}
</style>
