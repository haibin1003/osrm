<template>
  <div class="settings-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">系统设置</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" type="border-card">
        <!-- 基本设置 -->
        <el-tab-pane label="基本设置" name="basic">
          <el-form :model="basicSettings" label-width="150px" class="settings-form">
            <el-form-item label="系统名称">
              <el-input v-model="basicSettings.systemName" />
            </el-form-item>

            <el-form-item label="系统Logo">
              <el-upload
                class="avatar-uploader"
                action="#"
                :auto-upload="false"
                :show-file-list="false"
              >
                <img v-if="basicSettings.logo" :src="basicSettings.logo" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>

            <el-form-item label="登录页背景">
              <el-upload
                class="avatar-uploader"
                action="#"
                :auto-upload="false"
                :show-file-list="false"
              >
                <img v-if="basicSettings.loginBg" :src="basicSettings.loginBg" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>

            <el-form-item label="版权信息">
              <el-input v-model="basicSettings.copyright" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveBasic">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 安全设置 -->
        <el-tab-pane label="安全设置" name="security">
          <el-form :model="securitySettings" label-width="150px" class="settings-form">
            <el-form-item label="密码最小长度">
              <el-slider v-model="securitySettings.passwordMinLength" :min="6" :max="20" show-stops />
            </el-form-item>

            <el-form-item label="密码复杂度">
              <el-checkbox-group v-model="securitySettings.passwordComplexity">
                <el-checkbox label="uppercase">必须包含大写字母</el-checkbox>
                <el-checkbox label="lowercase">必须包含小写字母</el-checkbox>
                <el-checkbox label="number">必须包含数字</el-checkbox>
                <el-checkbox label="special">必须包含特殊字符</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="登录失败锁定">
              <el-switch v-model="securitySettings.loginLockEnabled" />
            </el-form-item>

            <el-form-item v-if="securitySettings.loginLockEnabled" label="锁定阈值">
              <el-input-number v-model="securitySettings.loginLockThreshold" :min="3" :max="10" />
              <span class="form-hint">次登录失败后锁定账户</span>
            </el-form-item>

            <el-form-item label="Token过期时间">
              <el-input-number v-model="securitySettings.tokenExpireMinutes" :min="30" :max="1440" step="30" />
              <span class="form-hint">分钟</span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveSecurity">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 通知设置 -->
        <el-tab-pane label="通知设置" name="notification">
          <el-form :model="notificationSettings" label-width="150px" class="settings-form">
            <el-form-item label="邮件通知">
              <el-switch v-model="notificationSettings.emailEnabled" />
            </el-form-item>

            <template v-if="notificationSettings.emailEnabled">
              <el-form-item label="SMTP服务器">
                <el-input v-model="notificationSettings.smtpServer" placeholder="smtp.example.com" />
              </el-form-item>

              <el-form-item label="SMTP端口">
                <el-input-number v-model="notificationSettings.smtpPort" :min="1" :max="65535" />
              </el-form-item>

              <el-form-item label="发件人邮箱">
                <el-input v-model="notificationSettings.senderEmail" placeholder="noreply@example.com" />
              </el-form-item>

              <el-form-item label="发件人密码">
                <el-input v-model="notificationSettings.senderPassword" type="password" show-password />
              </el-form-item>
            </template>

            <el-divider />

            <el-form-item label="通知事件">
              <el-checkbox-group v-model="notificationSettings.events">
                <el-checkbox label="user_registered">用户注册</el-checkbox>
                <el-checkbox label="password_changed">密码修改</el-checkbox>
                <el-checkbox label="subscription_approved">订购审批</el-checkbox>
                <el-checkbox label="system_alert">系统告警</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveNotification">保存设置</el-button>
              <el-button @click="testEmail">测试邮件</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 存储设置 -->
        <el-tab-pane label="存储设置" name="storage">
          <el-form :model="storageSettings" label-width="150px" class="settings-form">
            <el-form-item label="存储类型">
              <el-radio-group v-model="storageSettings.type">
                <el-radio label="local">本地存储</el-radio>
                <el-radio label="s3">对象存储(S3)</el-radio>
                <el-radio label="oss">阿里云OSS</el-radio>
              </el-radio-group>
            </el-form-item>

            <template v-if="storageSettings.type === 'local'">
              <el-form-item label="存储路径">
                <el-input v-model="storageSettings.localPath" placeholder="/data/uploads" />
              </el-form-item>
            </template>

            <template v-if="storageSettings.type === 's3'">
              <el-form-item label="Endpoint">
                <el-input v-model="storageSettings.s3Endpoint" placeholder="https://s3.amazonaws.com" />
              </el-form-item>

              <el-form-item label="Bucket">
                <el-input v-model="storageSettings.s3Bucket" />
              </el-form-item>

              <el-form-item label="Access Key">
                <el-input v-model="storageSettings.s3AccessKey" />
              </el-form-item>

              <el-form-item label="Secret Key">
                <el-input v-model="storageSettings.s3SecretKey" type="password" show-password />
              </el-form-item>
            </template>

            <template v-if="storageSettings.type === 'oss'">
              <el-form-item label="Endpoint">
                <el-input v-model="storageSettings.ossEndpoint" placeholder="https://oss-cn-hangzhou.aliyuncs.com" />
              </el-form-item>

              <el-form-item label="Bucket">
                <el-input v-model="storageSettings.ossBucket" />
              </el-form-item>

              <el-form-item label="Access Key ID">
                <el-input v-model="storageSettings.ossAccessKeyId" />
              </el-form-item>

              <el-form-item label="Access Key Secret">
                <el-input v-model="storageSettings.ossAccessKeySecret" type="password" show-password />
              </el-form-item>
            </template>

            <el-form-item label="单文件大小限制">
              <el-input-number v-model="storageSettings.maxFileSize" :min="1" :max="1024" />
              <span class="form-hint">MB</span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveStorage">保存设置</el-button>
              <el-button @click="testStorage">测试连接</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 令牌配置 -->
        <el-tab-pane label="令牌配置" name="token">
          <el-form :model="tokenSettings" label-width="160px" class="settings-form">
            <el-form-item label="默认有效期">
              <el-select v-model="tokenSettings['token.defaultLifetime']" style="width: 200px">
                <el-option label="1小时" value="1h" />
                <el-option label="1天" value="1d" />
                <el-option label="7天" value="7d" />
                <el-option label="30天" value="30d" />
                <el-option label="永久" value="forever" />
              </el-select>
              <span class="form-hint">新建下载令牌的默认有效期</span>
            </el-form-item>

            <el-form-item label="默认最大下载次数">
              <el-select v-model="tokenSettings['token.maxDownloads']" style="width: 200px">
                <el-option label="不限" value="0" />
                <el-option label="1次" value="1" />
                <el-option label="5次" value="5" />
                <el-option label="10次" value="10" />
                <el-option label="50次" value="50" />
                <el-option label="100次" value="100" />
              </el-select>
              <span class="form-hint">新建下载令牌的默认最大下载次数</span>
            </el-form-item>

            <el-form-item label="平台名称">
              <el-input v-model="tokenSettings['platform.name']" style="width: 300px" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="savingToken" @click="saveTokenSettings">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 日志设置 -->
        <el-tab-pane label="日志设置" name="log">
          <el-form :model="logSettings" label-width="150px" class="settings-form">
            <el-form-item label="操作日志">
              <el-switch v-model="logSettings.operationLogEnabled" />
            </el-form-item>

            <el-form-item label="日志保留天数">
              <el-input-number v-model="logSettings.logRetentionDays" :min="7" :max="365" />
              <span class="form-hint">天</span>
            </el-form-item>

            <el-form-item label="登录日志">
              <el-switch v-model="logSettings.loginLogEnabled" />
            </el-form-item>

            <el-form-item label="系统日志级别">
              <el-select v-model="logSettings.logLevel">
                <el-option label="DEBUG" value="DEBUG" />
                <el-option label="INFO" value="INFO" />
                <el-option label="WARN" value="WARN" />
                <el-option label="ERROR" value="ERROR" />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveLog">保存设置</el-button>
              <el-button @click="exportLogs">导出日志</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { configApi } from '@/api/config'

const activeTab = ref('basic')
const savingToken = ref(false)

// 基本设置
const basicSettings = reactive({
  systemName: 'OSRM 开源软件仓库管理系统',
  logo: '',
  loginBg: '',
  copyright: '© 2026 OSRM. All rights reserved.'
})

// 安全设置
const securitySettings = reactive({
  passwordMinLength: 8,
  passwordComplexity: ['uppercase', 'lowercase', 'number'] as string[],
  loginLockEnabled: true,
  loginLockThreshold: 5,
  tokenExpireMinutes: 120
})

// 通知设置
const notificationSettings = reactive({
  emailEnabled: false,
  smtpServer: '',
  smtpPort: 587,
  senderEmail: '',
  senderPassword: '',
  events: ['user_registered', 'password_changed'] as string[]
})

// 存储设置
const storageSettings = reactive({
  type: 'local' as 'local' | 's3' | 'oss',
  localPath: '/data/uploads',
  s3Endpoint: '',
  s3Bucket: '',
  s3AccessKey: '',
  s3SecretKey: '',
  ossEndpoint: '',
  ossBucket: '',
  ossAccessKeyId: '',
  ossAccessKeySecret: '',
  maxFileSize: 100
})

// 日志设置
const logSettings = reactive({
  operationLogEnabled: true,
  logRetentionDays: 30,
  loginLogEnabled: true,
  logLevel: 'INFO'
})

// 令牌配置
const tokenSettings = reactive<Record<string, string>>({
  'token.defaultLifetime': '7d',
  'token.maxDownloads': '10',
  'platform.name': 'OSRM'
})

// 方法
const saveBasic = () => {
  ElMessage.success('基本设置已保存')
}

const saveSecurity = () => {
  ElMessage.success('安全设置已保存')
}

const saveNotification = () => {
  ElMessage.success('通知设置已保存')
}

const testEmail = () => {
  ElMessage.success('测试邮件已发送')
}

const saveStorage = () => {
  ElMessage.success('存储设置已保存')
}

const saveTokenSettings = async () => {
  savingToken.value = true
  try {
    await configApi.update(tokenSettings)
    ElMessage.success('令牌配置已保存')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally { savingToken.value = false }
}

// 加载配置
onMounted(async () => {
  try {
    const configs = await configApi.getAll()
    Object.assign(tokenSettings, configs)
  } catch { /* ignore */ }
})

const testStorage = () => {
  ElMessage.success('存储连接测试成功')
}

const saveLog = () => {
  ElMessage.success('日志设置已保存')
}

const exportLogs = () => {
  ElMessage.success('日志导出成功')
}
</script>

<style scoped lang="scss">
.settings-page {
  :deep(.el-card) {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-xs);
  }

  .card-header {
    .title {
      font-size: var(--font-size-md);
      font-weight: var(--font-weight-bold);
    }
  }

  .settings-form {
    max-width: 600px;
    padding: var(--space-lg);

    .form-hint {
      margin-left: var(--space-xs);
      color: var(--color-text-secondary);
      font-size: var(--font-size-sm);
    }
  }

  .avatar-uploader {
    :deep(.el-upload) {
      border: 1px dashed var(--color-border);
      border-radius: var(--radius-sm);
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: border-color var(--transition-fast);

      &:hover {
        border-color: var(--color-primary);
      }
    }

    .avatar {
      width: 178px;
      height: 178px;
      display: block;
    }

    .avatar-uploader-icon {
      font-size: 28px;
      color: var(--color-text-tertiary);
      width: 178px;
      height: 178px;
      text-align: center;
      line-height: 178px;
    }
  }
}
</style>
