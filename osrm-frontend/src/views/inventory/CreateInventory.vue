<template>
  <div class="inventory-create">
    <div class="form-card content-card">
      <div class="form-header">
        <h2 class="form-title">存量软件登记</h2>
        <p class="form-subtitle">选择一个系统，一次可登记多个开源软件</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="inventory-form">
        <!-- 系统选择 -->
        <el-form-item label="所属系统" prop="businessSystemId">
          <el-select
            v-model="form.businessSystemId"
            placeholder="请选择业务系统"
            style="width: 100%"
            filterable
            clearable
            @change="onSystemChange"
          >
            <el-option
              v-for="sys in allSystems"
              :key="sys.id"
              :label="sys.systemName"
              :value="sys.id"
            >
              <span>{{ sys.systemName }}</span>
              <span style="float: right; color: #909399; font-size: 12px">{{ sys.systemCode }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item v-if="appList.length > 0" label="所属应用">
          <el-select
            v-model="form.businessSystemApplicationId"
            placeholder="选择应用（选填）"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="app in appList"
              :key="app.id"
              :label="app.applicationName"
              :value="app.id"
            />
          </el-select>
        </el-form-item>

        <!-- 软件列表 -->
        <div class="software-section">
          <div class="section-label">
            <span>软件列表</span>
            <el-button type="primary" link size="small" @click="addEntry">
              <el-icon><Plus /></el-icon> 添加软件
            </el-button>
          </div>

          <div
            v-for="(entry, index) in form.softwareEntries"
            :key="index"
            class="software-card"
          >
            <div class="card-header">
              <span class="card-index">{{ index + 1 }}</span>
              <el-button
                v-if="form.softwareEntries.length > 1"
                type="danger"
                link
                size="small"
                @click="removeEntry(index)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>

            <div class="card-body">
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item
                    :prop="`softwareEntries.${index}.packageName`"
                    :rules="[{ required: true, message: '请输入软件名称', trigger: 'blur' }]"
                    label="软件名称"
                  >
                    <el-autocomplete
                      v-model="entry.packageName"
                      :fetch-suggestions="(query: string, cb: any) => searchPackages(query, cb)"
                      placeholder="搜索已有软件或输入新名称"
                      style="width: 100%"
                      clearable
                      @select="(pkg: any) => onPackageSelect(pkg, index)"
                    >
                      <template #default="{ item }">
                        <div>{{ item.value }}</div>
                      </template>
                    </el-autocomplete>
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="版本号">
                    <el-input v-model="entry.versionNo" placeholder="如 8.0.32" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="软件类型">
                    <el-select v-model="entry.softwareType" placeholder="选择" style="width: 100%" clearable>
                      <el-option label="Docker镜像" value="DOCKER_IMAGE" />
                      <el-option label="Maven包" value="MAVEN" />
                      <el-option label="NPM包" value="NPM" />
                      <el-option label="其他" value="GENERIC" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="12">
                <el-col :span="8">
                  <el-form-item label="部署环境">
                    <el-select v-model="entry.deployEnvironment" placeholder="选择" style="width: 100%" clearable>
                      <el-option label="生产环境" value="PRODUCTION" />
                      <el-option label="测试环境" value="TESTING" />
                      <el-option label="开发环境" value="DEVELOPMENT" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="服务器数量">
                    <el-input-number v-model="entry.serverCount" :min="1" :max="9999" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="使用场景">
                    <el-input v-model="entry.usageScenario" placeholder="简述用途" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="备注">
                <el-input v-model="entry.remarks" placeholder="其他说明" />
              </el-form-item>
            </div>
          </div>
        </div>

        <el-form-item>
          <el-button type="primary" @click="submit" :loading="submitting">
            提交登记
          </el-button>
          <el-button @click="reset">重置</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { inventoryApi, type SoftwareEntry, type BatchCreateRequest } from '@/api/inventory'
import { softwareApi, type SoftwarePackage } from '@/api/software'
import { getSystemCatalogList, getSystemApplications, type SystemCatalogItem, type ApplicationCatalogItem } from '@/api/catalog'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const allSystems = ref<SystemCatalogItem[]>([])
const appList = ref<ApplicationCatalogItem[]>([])
const packageList = ref<SoftwarePackage[]>([])

const emptyEntry = (): SoftwareEntry => ({
  packageName: '',
  versionNo: '',
  softwareType: '',
  deployEnvironment: '',
  serverCount: 1,
  usageScenario: '',
  remarks: ''
})

const form = reactive<BatchCreateRequest>({
  businessSystemId: undefined as unknown as number,
  businessSystemApplicationId: undefined,
  softwareEntries: [emptyEntry()]
})

const rules: FormRules = {
  businessSystemId: [{ required: true, message: '请选择系统', trigger: 'change' }]
}

const loadAllSystems = async () => {
  try {
    const res: any = await getSystemCatalogList({ enabled: true, page: 1, size: 200 })
    allSystems.value = res.content || []
  } catch (error) {
    console.error('加载系统目录失败', error)
  }
}

const loadAllPackages = async () => {
  try {
    const res = await softwareApi.list({ page: 1, size: 1000 })
    packageList.value = res.content || []
  } catch (error) {
    console.error('加载软件包失败', error)
  }
}

const onSystemChange = async (systemId: number | undefined) => {
  form.businessSystemApplicationId = undefined
  appList.value = []
  if (systemId) {
    try {
      const res: any = await getSystemApplications(systemId)
      appList.value = res || []
    } catch (error) {
      console.error('加载应用列表失败', error)
    }
  }
}

const searchPackages = (query: string, cb: (results: any[]) => void) => {
  if (!query) {
    cb(packageList.value.map(p => ({ value: p.packageName, id: p.id })))
    return
  }
  const results = packageList.value
    .filter(p => p.packageName.toLowerCase().includes(query.toLowerCase()))
    .map(p => ({ value: p.packageName, id: p.id }))
  cb(results)
}

const onPackageSelect = (pkg: any, index: number) => {
  const found = packageList.value.find(p => p.id === pkg.id)
  if (found) {
    form.softwareEntries[index].packageId = found.id
  }
}

const addEntry = () => {
  form.softwareEntries.push(emptyEntry())
}

const removeEntry = (index: number) => {
  form.softwareEntries.splice(index, 1)
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await inventoryApi.batchCreate({
        businessSystemId: form.businessSystemId,
        businessSystemApplicationId: form.businessSystemApplicationId || undefined,
        softwareEntries: form.softwareEntries.map(e => ({
          packageId: e.packageId,
          packageName: e.packageName,
          versionNo: e.versionNo || undefined,
          softwareType: e.softwareType || undefined,
          deployEnvironment: e.deployEnvironment || undefined,
          serverCount: e.serverCount,
          usageScenario: e.usageScenario || undefined,
          remarks: e.remarks || undefined
        }))
      })
      ElMessage.success(`成功登记 ${form.softwareEntries.length} 个软件，等待审批`)
      router.push('/inventory/my')
    } catch (error) {
      console.error('登记失败', error)
      ElMessage.error('登记失败，请重试')
    } finally {
      submitting.value = false
    }
  })
}

const reset = () => {
  formRef.value?.resetFields()
  form.businessSystemId = undefined as unknown as number
  form.businessSystemApplicationId = undefined
  form.softwareEntries = [emptyEntry()]
  appList.value = []
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadAllSystems()
  loadAllPackages()
})
</script>

<style scoped lang="scss">
.inventory-create {
  padding: var(--space-xl) 0;
  max-width: 900px;
  margin: 0 auto;
}

.form-card {
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
    background: var(--color-primary);
  }
}

.form-header {
  padding: var(--space-xl) var(--space-2xl);
  border-bottom: 1px solid var(--color-border-light);

  .form-title {
    font-size: var(--font-size-xl);
    font-weight: var(--font-weight-medium);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-xs);
  }

  .form-subtitle {
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    margin: 0;
  }
}

.inventory-form {
  padding: var(--space-xl) var(--space-2xl);
}

.software-section {
  margin: var(--space-lg) 0;
}

.section-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-md);
  padding-bottom: var(--space-sm);
  border-bottom: 1px solid var(--color-border-light);
}

.software-card {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  margin-bottom: var(--space-md);

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--space-sm);
  }

  .card-index {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: var(--color-primary);
    color: #fff;
    font-size: 12px;
    font-weight: 600;
  }

  .card-body {
    :deep(.el-form-item) {
      margin-bottom: var(--space-md);
    }

    :deep(.el-form-item:last-child) {
      margin-bottom: 0;
    }

    :deep(.el-autocomplete),
    :deep(.el-select) {
      width: 100%;
    }
  }
}
</style>
