<template>
  <div class="profile-page">
    <el-row :gutter="24">
      <!-- 左侧个人信息卡片 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="6">
        <el-card class="profile-card" shadow="never">
          <div class="profile-header">
            <el-avatar :size="100" :src="userInfo.avatar" class="profile-avatar">
              {{ userInitials }}
            </el-avatar>
            <h3 class="profile-name">{{ userInfo.realName }}</h3>
            <p class="profile-username">@{{ userInfo.username }}</p>
            <el-tag type="primary" class="profile-role">{{ userInfo.role }}</el-tag>
          </div>

          <el-divider />

          <div class="profile-info">
            <div class="info-item">
              <el-icon><Message /></el-icon>
              <span>{{ userInfo.email }}</span>
            </div>
            <div class="info-item">
              <el-icon><Phone /></el-icon>
              <span>{{ userInfo.phone || '未设置' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Timer /></el-icon>
              <span>注册时间: {{ userInfo.createTime }}</span>
            </div>
            <div class="info-item">
              <el-icon><Clock /></el-icon>
              <span>最后登录: {{ userInfo.lastLoginTime }}</span>
            </div>
          </div>

          <el-divider />

          <div class="profile-stats">
            <div class="stat-item">
              <div class="stat-value">{{ stats.softwareCount }}</div>
              <div class="stat-label">订购软件</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.approvalCount }}</div>
              <div class="stat-label">审批记录</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.loginCount }}</div>
              <div class="stat-label">登录次数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧编辑区域 -->
      <el-col :xs="24" :sm="24" :md="16" :lg="18">
        <el-card class="edit-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="title">基本信息</span>
            </div>
          </template>

          <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            label-width="100px"
            class="profile-form"
          >
            <el-form-item label="用户名">
              <el-input v-model="formData.username" disabled />
            </el-form-item>

            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入手机号" />
            </el-form-item>

            <el-form-item label="个人简介" prop="bio">
              <el-input
                v-model="formData.bio"
                type="textarea"
                :rows="4"
                placeholder="请输入个人简介"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSave">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="password-card" shadow="never" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span class="title">修改密码</span>
            </div>
          </template>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            class="password-form"
          >
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input
                v-model="passwordForm.currentPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/modules/auth'
import { profileApi, type Profile } from '@/api/profile'
import { Message, Phone, Timer, Clock } from '@element-plus/icons-vue'

const authStore = useAuthStore()

// 用户信息
const userInfo = reactive({
  username: '',
  realName: '',
  email: '',
  phone: '',
  role: '',
  avatar: '',
  createTime: '',
  lastLoginTime: '',
  roles: [] as string[]
})

const userInitials = computed(() => {
  return userInfo.realName?.charAt(0).toUpperCase() || '?'
})

// 统计数据（暂用模拟数据，后续可从后端获取）
const stats = reactive({
  softwareCount: 0,
  approvalCount: 0,
  loginCount: 0
})

// 加载用户档案
const loading = ref(false)
const loadProfile = async () => {
  loading.value = true
  try {
    const profile: Profile = await profileApi.getProfile()
    Object.assign(userInfo, {
      username: profile.username,
      realName: profile.realName,
      email: profile.email,
      phone: profile.phone,
      role: profile.roles?.[0] || '用户',
      avatar: profile.avatar,
      createTime: profile.createTime ? new Date(profile.createTime).toLocaleString('zh-CN') : '-',
      lastLoginTime: profile.lastLoginTime ? new Date(profile.lastLoginTime).toLocaleString('zh-CN') : '-',
      roles: profile.roles || []
    })
    // 同步更新表单数据
    Object.assign(formData, {
      username: profile.username,
      realName: profile.realName,
      email: profile.email,
      phone: profile.phone,
      bio: profile.bio || ''
    })
  } catch (error: any) {
    ElMessage.error(error.message || '加载用户信息失败')
  } finally {
    loading.value = false
  }
}

// 基本信息表单
const formRef = ref<FormInstance>()
const formData = reactive({
  username: userInfo.username,
  realName: userInfo.realName,
  email: userInfo.email,
  phone: userInfo.phone,
  bio: ''
})

const formRules: FormRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 密码表单
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 方法
const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await profileApi.updateProfile({
          realName: formData.realName,
          email: formData.email,
          phone: formData.phone,
          bio: formData.bio
        })
        // 更新本地显示
        Object.assign(userInfo, {
          realName: formData.realName,
          email: formData.email,
          phone: formData.phone
        })
        // 更新 auth store 中的用户信息
        if (authStore.userInfo) {
          authStore.userInfo.realName = formData.realName
          authStore.userInfo.email = formData.email
        }
        ElMessage.success('保存成功')
      } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
      }
    }
  })
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await profileApi.changePassword({
          currentPassword: passwordForm.currentPassword,
          newPassword: passwordForm.newPassword
        })
        ElMessage.success('密码修改成功')
        passwordForm.currentPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
      } catch (error: any) {
        ElMessage.error(error.message || '密码修改失败')
      }
    }
  })
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped lang="scss">
.profile-page {
  .profile-card {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-xs);

    .profile-header {
      text-align: center;
      padding: var(--space-lg) 0;

      .profile-avatar {
        background: var(--color-primary);
        color: #fff;
        font-size: 36px;
        font-weight: var(--font-weight-bold);
      }

      .profile-name {
        margin-top: var(--space-md);
        font-size: var(--font-size-xl);
        font-weight: var(--font-weight-bold);
        color: var(--color-text-primary);
      }

      .profile-username {
        margin-top: var(--space-xs);
        color: var(--color-text-secondary);
        font-size: var(--font-size-sm);
      }

      .profile-role {
        margin-top: var(--space-sm);
      }
    }

    .profile-info {
      padding: 0 var(--space-sm);

      .info-item {
        display: flex;
        align-items: center;
        gap: var(--space-sm);
        padding: var(--space-sm) 0;
        color: var(--color-text-secondary);
        font-size: var(--font-size-sm);

        .el-icon {
          color: var(--color-text-tertiary);
          font-size: var(--font-size-md);
        }
      }
    }

    .profile-stats {
      display: flex;
      justify-content: space-around;
      padding: var(--space-sm) 0;

      .stat-item {
        text-align: center;

        .stat-value {
          font-size: var(--font-size-2xl);
          font-weight: var(--font-weight-bold);
          color: var(--color-primary);
        }

        .stat-label {
          margin-top: var(--space-xs);
          font-size: var(--font-size-xs);
          color: var(--color-text-secondary);
        }
      }
    }
  }

  .edit-card,
  .password-card {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-xs);

    & + & {
      margin-top: var(--space-lg);
    }

    .card-header {
      .title {
        font-size: var(--font-size-md);
        font-weight: var(--font-weight-bold);
      }
    }

    .profile-form,
    .password-form {
      max-width: 500px;
    }
  }
}
</style>
