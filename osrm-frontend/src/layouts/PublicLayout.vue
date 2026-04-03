<template>
  <div class="public-layout">
    <header class="public-header">
      <div class="header-inner">
        <div class="brand" @click="router.push('/landing')">
          <div class="brand-icon"><el-icon :size="18"><Box /></el-icon></div>
          <span class="brand-name">OSRM</span>
        </div>
        <div class="header-actions">
          <template v-if="authStore.isAuthenticated">
            <el-button type="primary" link @click="router.push('/home')">进入系统</el-button>
            <el-button type="primary" size="small" @click="router.push('/home')">控制台</el-button>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="router.push('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </header>
    <main class="public-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Box } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/modules/auth'

const router = useRouter()
const authStore = useAuthStore()
</script>

<style scoped lang="scss">
.public-layout {
  min-height: 100vh;
  background: var(--color-bg-page);
  display: flex;
  flex-direction: column;
}

.public-header {
  height: 52px;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 100%;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;

  .brand-icon {
    width: 32px;
    height: 32px;
    background: var(--color-primary);
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
  }

  .brand-name {
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-bold);
    color: var(--color-text-primary);
    letter-spacing: -0.3px;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.public-content {
  flex: 1;
  width: 100%;
}
</style>
