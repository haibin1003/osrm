<template>
  <div class="landing-page">
    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-content">
        <div class="hero-logo">
          <el-icon :size="48"><Box /></el-icon>
        </div>
        <h1 class="hero-title">OSRM</h1>
        <p class="hero-subtitle">开源软件仓库管理系统</p>
        <p class="hero-description">
          统一管理企业级开源软件资产，实现软件录入、版本管理、安全审计、订购审批一站式解决方案
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="goToLogin">
            <el-icon><User /></el-icon>
            立即登录
          </el-button>
          <el-button size="large" @click="goToPortal">
            <el-icon><View /></el-icon>
            浏览软件
          </el-button>
        </div>
      </div>
    </section>

    <!-- Stats Section -->
    <section class="stats-section">
      <div class="container">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalPackages }}</div>
            <div class="stat-label">软件包总数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.publishedCount }}</div>
            <div class="stat-label">已发布</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalSubscriptions }}</div>
            <div class="stat-label">订购次数</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Popular Software Section -->
    <section class="popular-section">
      <div class="container">
        <h2 class="section-title">热门软件</h2>
        <div class="software-grid">
          <div
            v-for="pkg in popularSoftware"
            :key="pkg.id"
            class="software-card card-hover-lift"
            @click="goToDetail(pkg.id)"
          >
            <div class="software-header">
              <div class="software-icon" :class="`type-${pkg.softwareType?.toLowerCase()}`">
                {{ pkg.packageName?.charAt(0).toUpperCase() }}
              </div>
              <div class="software-type">{{ pkg.softwareTypeName }}</div>
            </div>
            <h3 class="software-name">{{ pkg.packageName }}</h3>
            <p class="software-desc">{{ pkg.description }}</p>
            <div class="software-meta">
              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ pkg.viewCount || 0 }}
              </span>
              <span class="meta-item">
                <el-icon><Download /></el-icon>
                {{ pkg.downloadCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="features-section">
      <div class="container">
        <h2 class="section-title">平台特性</h2>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon">
              <el-icon :size="32"><Collection /></el-icon>
            </div>
            <h3>统一管理</h3>
            <p>集中管理企业所有开源软件资产，包括 Docker、Maven、NPM 等多种格式</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">
              <el-icon :size="32"><Files /></el-icon>
            </div>
            <h3>多格式支持</h3>
            <p>支持 Docker 镜像、Helm Chart、Maven、NPM、PyPI 等多种软件包格式</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">
              <el-icon :size="32"><Lock /></el-icon>
            </div>
            <h3>合规审计</h3>
            <p>记录软件使用场景，追踪业务系统依赖，满足企业合规审计要求</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">
              <el-icon :size="32"><Lock /></el-icon>
            </div>
            <h3>权限管控</h3>
            <p>基于 RBAC 的权限管理，支持系统管理员、软件管理员、开发人员、访客等多角色</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <h3>版本追踪</h3>
            <p>完整的版本管理，支持版本发布、下线、回滚，确保软件版本可追溯</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">
              <el-icon :size="32"><CircleCheck /></el-icon>
            </div>
            <h3>订购审批</h3>
            <p>软件订购申请-审批流程，控制软件使用范围，确保合规使用</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="landing-footer">
      <div class="container">
        <p>© 2026 OSRM - 开源软件仓库管理系统</p>
        <el-button type="primary" link @click="goToLogin">进入系统</el-button>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { portalApi } from '@/api/portal'
import type { SoftwarePackage } from '@/types/software'
import {
  Box, User, View, Download, Collection, Files, Lock, Document, CircleCheck
} from '@element-plus/icons-vue'

const router = useRouter()

const stats = ref({
  totalPackages: 0,
  publishedCount: 0,
  pendingCount: 0,
  draftCount: 0,
  totalSubscriptions: 0
})

const popularSoftware = ref<SoftwarePackage[]>([])

onMounted(async () => {
  try {
    const statsRes = await portalApi.getStatsOverview()
    stats.value = statsRes || {
      totalPackages: 0,
      publishedCount: 0,
      pendingCount: 0,
      draftCount: 0,
      totalSubscriptions: 0
    }
  } catch (e) {
    console.error('Failed to load stats:', e)
  }

  try {
    const popularRes = await portalApi.getPopular()
    popularSoftware.value = (popularRes || []).slice(0, 6)
  } catch (e) {
    console.error('Failed to load popular software:', e)
  }
})

const goToLogin = () => router.push('/login')
const goToPortal = () => router.push('/browse')
const goToDetail = (id: number) => router.push(`/browse/software/${id}`)
</script>

<style scoped lang="scss">
.landing-page {
  min-height: 100vh;
  background: var(--color-bg-page);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-xl);
}

// Hero Section
.hero {
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #312e81 100%);
  color: white;
  padding: 32px 0 24px;
  text-align: center;
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 var(--space-xl);
}

.hero-logo {
  width: 48px;
  height: 48px;
  background: var(--color-primary);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-md);

  :deep(.el-icon) {
    font-size: 24px;
  }
}

.hero-title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  margin: 0 0 var(--space-xs);
  letter-spacing: -0.3px;
}

.hero-subtitle {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
  margin: 0 0 var(--space-xs);
  opacity: 0.9;
}

.hero-description {
  font-size: var(--font-size-sm);
  margin: 0 0 var(--space-lg);
  opacity: 0.8;
  line-height: 1.4;
  max-width: 480px;
  margin-left: auto;
  margin-right: auto;
}

.hero-actions {
  display: flex;
  gap: var(--space-lg);
  justify-content: center;

  .el-button {
    min-width: 140px;
  }
}

// Stats Section
.stats-section {
  padding: var(--space-4xl) 0;
  background: var(--color-bg-card);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-2xl);
}

.stat-card {
  text-align: center;
  padding: var(--space-2xl);
  background: var(--color-bg-page);
  border-radius: var(--radius-xl);

  .stat-value {
    font-size: var(--font-size-4xl);
    font-weight: var(--font-weight-bold);
    color: var(--color-primary);
    margin-bottom: var(--space-sm);
  }

  .stat-label {
    font-size: var(--font-size-md);
    color: var(--color-text-secondary);
  }
}

// Popular Section
.popular-section {
  padding: var(--space-4xl) 0;
}

.section-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  text-align: center;
  margin-bottom: var(--space-3xl);
}

.software-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-xl);
}

.software-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  cursor: pointer;
  border: 1px solid var(--color-border);

  &:hover {
    border-color: var(--color-primary-muted);
  }
}

.software-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
}

.software-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: white;

  &.type-docker_image { background: #3b82f6; }
  &.type-helm_chart { background: #0ea5e9; }
  &.type-maven { background: #c2410c; }
  &.type-npm { background: #dc2626; }
  &.type-pypi { background: #16a34a; }
  &.type-generic { background: #6b7280; }
}

.software-type {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  background: var(--color-bg-page);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-sm);
}

.software-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-sm);
}

.software-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-lg);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.software-meta {
  display: flex;
  gap: var(--space-lg);
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);

  .meta-item {
    display: flex;
    align-items: center;
    gap: var(--space-xs);
  }
}

// Features Section
.features-section {
  padding: var(--space-4xl) 0;
  background: var(--color-bg-card);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-xl);
}

.feature-card {
  background: var(--color-bg-page);
  border-radius: var(--radius-xl);
  padding: var(--space-2xl);
  text-align: center;

  .feature-icon {
    width: 64px;
    height: 64px;
    background: var(--color-primary-light);
    border-radius: var(--radius-lg);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto var(--space-lg);
    color: var(--color-primary);
  }

  h3 {
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-sm);
  }

  p {
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    margin: 0;
    line-height: 1.6;
  }
}

// Footer
.landing-footer {
  background: #0f172a;
  color: white;
  padding: var(--space-2xl) 0;

  .container {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  p {
    margin: 0;
    opacity: 0.8;
  }
}

@media (max-width: 768px) {
  .hero {
    padding: 60px 0;
  }

  .hero-title {
    font-size: var(--font-size-3xl);
  }

  .hero-actions {
    flex-direction: column;
    align-items: center;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .landing-footer .container {
    flex-direction: column;
    gap: var(--space-md);
    text-align: center;
  }
}
</style>
