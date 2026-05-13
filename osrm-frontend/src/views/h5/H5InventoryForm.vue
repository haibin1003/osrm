<template>
  <div class="h5-form">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
      <!-- 系统信息（先选系统和应用，带出责任人信息） -->
      <div class="section">
        <div class="section-title">系统信息</div>
        <div class="section-tip">
          请先选择所属系统和应用，系统将自动关联责任人信息
        </div>
        <el-form-item label="所属系统" prop="systemCatalogId">
          <el-select
            v-model="form.systemCatalogId"
            placeholder="请选择系统（可输入搜索）"
            style="width: 100%"
            filterable
            clearable
            @change="onSystemChange"
          >
            <el-option
              v-for="sys in allSystems"
              :key="sys.id"
              :label="`${sys.systemName} (${sys.systemCode})`"
              :value="sys.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="showApplicationSelect" label="所属应用" prop="applicationCatalogId">
          <el-select
            v-model="form.applicationCatalogId"
            placeholder="请选择应用"
            style="width: 100%"
            @change="onApplicationChange"
          >
            <el-option
              v-for="app in applicationList"
              :key="app.id"
              :label="app.applicationName"
              :value="app.id"
            />
          </el-select>
        </el-form-item>
      </div>

      <!-- 责任人信息（系统选择后自动带出，可手动修改） -->
      <div class="section">
        <div class="section-title">责任人信息</div>
        <div class="section-tip">
          以下信息由所选应用自动带出，也可手动修改
        </div>
        <el-form-item label="姓名" prop="responsiblePerson">
          <el-input v-model="form.responsiblePerson" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入11位手机号"
            maxlength="11"
            @focus="onPhoneFocus"
            @blur="onPhoneBlur"
          />
          <div v-if="phoneOriginal" class="phone-hint">已脱敏显示：{{ maskedPhone }}</div>
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="form.department" placeholder="请输入部门（选填）" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
          <div class="email-tip">为方便开通开源软件管理平台的账号，请输入邮箱信息</div>
        </el-form-item>
      </div>

      <!-- 软件列表 -->
      <div class="section">
        <div class="section-title">软件信息</div>
        <div class="section-tip">
          填写当前系统下正在使用的开源软件，可添加多个
        </div>
        <div
          v-for="(entry, index) in form.softwareEntries"
          :key="index"
          class="software-card"
        >
          <div class="card-head">
            <span class="card-num">{{ index + 1 }}</span>
            <el-button
              v-if="form.softwareEntries.length > 1"
              link
              type="danger"
              size="small"
              @click="removeEntry(index)"
            >删除</el-button>
          </div>
          <el-form-item
            :prop="`softwareEntries.${index}.packageName`"
            :rules="[{ required: true, message: '请输入软件名称', trigger: 'blur' }]"
            :label="`${index + 1}. 软件名称`"
          >
            <el-input v-model="entry.packageName" placeholder="请输入软件名称" />
          </el-form-item>
          <el-row :gutter="8">
            <el-col :span="12">
              <el-form-item label="版本号">
                <el-input v-model="entry.versionNo" placeholder="如 8.0.32" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="软件类型">
                <el-select v-model="entry.softwareType" placeholder="请选择" style="width: 100%">
                  <el-option label="Docker镜像" value="DOCKER_IMAGE" />
                  <el-option label="Maven包" value="MAVEN" />
                  <el-option label="NPM包" value="NPM" />
                  <el-option label="其他" value="GENERIC" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="8">
            <el-col :span="12">
              <el-form-item label="部署环境">
                <el-select v-model="entry.deployEnvironment" placeholder="请选择" style="width: 100%">
                  <el-option label="生产环境" value="PRODUCTION" />
                  <el-option label="测试环境" value="TESTING" />
                  <el-option label="开发环境" value="DEVELOPMENT" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="服务器数量">
                <el-input-number v-model="entry.serverCount" :min="1" :max="9999" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="使用场景">
            <el-input v-model="entry.usageScenario" placeholder="描述使用场景（选填）" />
          </el-form-item>
        </div>
        <el-button type="primary" plain size="small" style="width: 100%; margin-top: 8px" @click="addEntry">
          + 添加软件
        </el-button>
      </div>

      <!-- 验证码 -->
      <div class="section">
        <div class="section-title">安全验证</div>
        <el-form-item label="图形验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="输入验证码" maxlength="4" style="flex: 1" />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              class="captcha-img"
              alt="验证码"
              @click="refreshCaptcha"
            />
            <el-button v-else text @click="refreshCaptcha">获取验证码</el-button>
          </div>
        </el-form-item>
      </div>

      <el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="submitting" @click="onSubmit">
          提交盘点（{{ form.softwareEntries.length }} 个软件）
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getCaptcha,
  searchSystems,
  getApplications,
  getSuggestion,
  submitInventory,
  type SystemCatalogItem,
  type ApplicationCatalogItem,
  type H5SoftwareEntry
} from '@/api/h5'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()

if (!route.query.token) {
  router.replace({ name: 'H5LinkExpired' })
}

const emptyEntry = (): H5SoftwareEntry => ({
  packageName: '',
  versionNo: '',
  softwareType: '',
  deployEnvironment: '',
  serverCount: 1,
  usageScenario: ''
})

const form = reactive({
  responsiblePerson: '',
  phone: '',
  department: '',
  email: '',
  systemCatalogId: null as number | null,
  applicationCatalogId: null as number | null,
  softwareEntries: [emptyEntry()] as H5SoftwareEntry[],
  captchaKey: '',
  captchaCode: ''
})

const allSystems = ref<SystemCatalogItem[]>([])
const showApplicationSelect = ref(false)
const applicationList = ref<ApplicationCatalogItem[]>([])
const phoneOriginal = ref('')
const captchaImage = ref('')
const submitting = ref(false)

const maskedPhone = computed(() => {
  const p = phoneOriginal.value || form.phone
  if (!p || p.length < 7) return p
  return p.substring(0, 2) + '***' + p.substring(p.length - 2)
})

const rules: FormRules = {
  responsiblePerson: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      validator: (_rule, _value, callback) => {
        const actual = phoneOriginal.value || form.phone
        if (!actual || !/^1[3-9]\d{9}$/.test(actual)) {
          callback(new Error('手机号格式不正确'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  systemCatalogId: [{ required: true, message: '请选择系统', trigger: 'change' }],
  applicationCatalogId: [{ required: true, message: '请选择应用', trigger: 'change' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

function onSystemChange(sysId: number | null) {
  if (!sysId) {
    showApplicationSelect.value = false
    applicationList.value = []
    form.applicationCatalogId = null
    form.responsiblePerson = ''
    form.phone = ''
    form.department = ''
    phoneOriginal.value = ''
    return
  }

  showApplicationSelect.value = true
  form.applicationCatalogId = null
  form.responsiblePerson = ''
  form.phone = ''
  form.department = ''
  phoneOriginal.value = ''
  applicationList.value = []
  getApplications(sysId).then((res: any) => {
    applicationList.value = res || []
  })
}

function onApplicationChange(appId: number | null) {
  if (!appId) {
    form.responsiblePerson = ''
    form.phone = ''
    phoneOriginal.value = ''
    form.department = ''
    return
  }
  const app = applicationList.value.find(a => a.id === appId)
  if (app) {
    if (app.responsiblePerson) form.responsiblePerson = app.responsiblePerson
    if (app.responsiblePhone) {
      phoneOriginal.value = app.responsiblePhone
      form.phone = maskedPhone.value
    }
  }
  // 系统级部门建议兜底
  if (form.systemCatalogId) {
    getSuggestion(form.systemCatalogId).then((res: any) => {
      if (res && !form.department && res.responsibleDept) {
        form.department = res.responsibleDept
      }
    })
  }
}

function onPhoneFocus() {
  if (phoneOriginal.value) {
    form.phone = phoneOriginal.value
  }
}

function onPhoneBlur() {
  const full = form.phone
  if (full && /^1[3-9]\d{9}$/.test(full)) {
    phoneOriginal.value = full
    form.phone = maskedPhone.value
  }
}

function addEntry() {
  form.softwareEntries.push(emptyEntry())
}

function removeEntry(index: number) {
  form.softwareEntries.splice(index, 1)
}

function refreshCaptcha() {
  getCaptcha().then((res: any) => {
    form.captchaKey = res.key
    captchaImage.value = res.image
  }).catch((err: Error) => {
    ElMessage.error(err.message || '验证码获取失败')
  })
}

function onSubmit() {
  if (!formRef.value) return
  formRef.value.validate((valid) => {
    if (!valid) return
    if (!form.captchaKey) {
      ElMessage.error('请先获取验证码')
      return
    }
    submitting.value = true
    submitInventory({
      responsiblePerson: form.responsiblePerson,
      phone: phoneOriginal.value || form.phone,
      department: form.department || undefined,
      email: form.email || undefined,
      systemCatalogId: form.systemCatalogId!,
      applicationCatalogId: form.applicationCatalogId!,
      softwareEntries: form.softwareEntries.map(e => ({
        packageName: e.packageName,
        versionNo: e.versionNo || undefined,
        softwareType: e.softwareType || undefined,
        deployEnvironment: e.deployEnvironment || undefined,
        serverCount: e.serverCount,
        usageScenario: e.usageScenario || undefined,
        remarks: e.remarks || undefined
      })),
      captchaKey: form.captchaKey,
      captchaCode: form.captchaCode
    }).then((res: any) => {
      router.push({
        name: 'H5SubmitSuccess',
        query: {
          count: res.count,
          username: res.username,
          initialPassword: res.initialPassword || ''
        }
      })
    }).catch((err: Error) => {
      ElMessage.error(err.message || '提交失败')
      refreshCaptcha()
    }).finally(() => {
      submitting.value = false
    })
  })
}

onMounted(() => {
  searchSystems('', 1, 200).then((res: any) => {
    allSystems.value = res.content || []
  })
})

refreshCaptcha()
</script>

<style scoped lang="scss">
.h5-form {
  padding-bottom: 32px;
}

.section {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.section-tip {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
  padding-left: 11px;
}

.software-card {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;

  .card-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }

  .card-num {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: #409eff;
    color: #fff;
    font-size: 12px;
    font-weight: 600;
  }

  :deep(.el-form-item) {
    margin-bottom: 10px;
  }

  :deep(.el-form-item__label) {
    font-size: 12px;
  }
}

.phone-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.email-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.captcha-img {
  width: 100px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
}
</style>
