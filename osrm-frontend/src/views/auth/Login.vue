<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">
          <el-icon :size="22"><Box /></el-icon>
        </div>
        <h1 class="logo-text">OSRM</h1>
        <p class="logo-sub">开源软件仓库管理系统</p>
      </div>

      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            :prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            clearable
          />
        </el-form-item>

        <div class="form-options">
          <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
        </div>

        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <p class="login-footer">© 2026 OSRM</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Box } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/modules/auth'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度至少8位，包含字母和数字', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.login(loginForm)
        ElMessage.success('登录成功')
        const redirect = route.query.redirect as string
        router.push(redirect || '/')
      } catch (error: any) {
        ElMessage.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-page);
}

.login-card {
  width: 400px;
  padding: var(--space-3xl) var(--space-2xl);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.login-header {
  text-align: center;
  margin-bottom: var(--space-2xl);
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #635bff, #a259ff);
  border-radius: var(--radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: var(--space-md);
  box-shadow: 0 4px 12px rgba(99, 91, 255, 0.3);
}

.logo-text {
  font-size: 22px;
  font-weight: var(--font-weight-light);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xs);
  letter-spacing: -0.3px;
}

.logo-sub {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
  font-weight: var(--font-weight-light);
}

.login-form {
  :deep(.el-input__wrapper) {
    padding: var(--space-xs) var(--space-md);
    background: #f6f9fc;
    border-radius: var(--radius-md);
    box-shadow: 0 0 0 1px var(--color-border) inset;
    transition: all var(--transition-fast);

    &:hover {
      box-shadow: 0 0 0 1px #c5cfd9 inset;
    }

    &.is-focus {
      background: #fff;
      box-shadow: 0 0 0 3px rgba(99, 91, 255, 0.1) inset;
    }
  }

  :deep(.el-input__inner) {
    height: 40px;
    font-size: var(--font-size-md);
    font-weight: var(--font-weight-light);
  }
}

.form-options {
  margin-bottom: var(--space-xl);

  :deep(.el-checkbox__label) {
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    font-weight: var(--font-weight-normal);
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: var(--color-primary);
    border-color: var(--color-primary);
  }
}

.login-btn {
  width: 100%;
  height: 42px;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  background: linear-gradient(135deg, #635bff, #7c6fff);
  border: none;
  border-radius: var(--radius-md);
  box-shadow: 0 2px 8px rgba(99, 91, 255, 0.25);
  transition: all var(--transition-base);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(99, 91, 255, 0.35);
  }
}

.login-footer {
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: var(--font-size-xs);
  margin: var(--space-2xl) 0 0;
  font-weight: var(--font-weight-normal);
}
</style>
