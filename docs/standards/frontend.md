# Vue 前端规范

本规范定义 OSRM 项目的前端开发标准，基于 Vue 3 + TypeScript + Vite 技术栈。

---

## 文档信息

- **作者**: OSRM 开发团队
- **创建时间**: 2026-03-17
- **最后更新**: 2026-03-17
- **维护责任人**: 前端负责人

---

## 技术栈

- **框架**: Vue 3.4+
- **语言**: TypeScript 5.0+
- **构建工具**: Vite 5.0+
- **状态管理**: Pinia 2.0+
- **路由**: Vue Router 4.0+
- **UI 组件库**: Element Plus
- **HTTP 客户端**: Axios
- **测试**: Vitest + Vue Test Utils + Playwright

---

## 项目结构

```
src/
├── api/                    # API 接口定义
│   ├── package.ts
│   └── user.ts
├── assets/                 # 静态资源
│   ├── images/
│   └── styles/
├── components/             # 公共组件
│   ├── common/            # 通用组件
│   │   ├── Pagination.vue
│   │   └── SearchForm.vue
│   └── business/          # 业务组件
│       ├── PackageCard.vue
│       └── ReviewTimeline.vue
├── composables/            # 组合式函数
│   ├── useAuth.ts
│   └── usePagination.ts
├── directives/             # 自定义指令
├── layouts/                # 布局组件
│   ├── DefaultLayout.vue
│   └── BlankLayout.vue
├── models/                 # 数据模型/类型定义
│   ├── package.ts
│   └── user.ts
├── router/                 # 路由配置
│   └── index.ts
├── stores/                 # Pinia Store
│   ├── auth.ts
│   └── package.ts
├── utils/                  # 工具函数
│   ├── request.ts         # Axios 封装
│   ├── storage.ts         # 本地存储
│   └── validators.ts      # 表单验证
├── views/                  # 页面组件
│   ├── login/
│   ├── packages/
│   │   ├── index.vue
│   │   ├── create.vue
│   │   └── detail.vue
│   └── dashboard/
├── App.vue
└── main.ts
```

---

## Vue 3 规范

### 使用 Composition API

统一使用 `<script setup>` 语法：

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { Package } from '@/models/package'

// 关联需求: REQ-001
const props = defineProps<{
  packageId: string
}>()

const emit = defineEmits<{
  submit: [data: Package]
}>()

// 响应式状态
const loading = ref(false)
const packageData = ref<Package | null>(null)

// 计算属性
const displayName = computed(() => {
  return packageData.value?.name || '-'
})

// 方法
async function fetchPackageDetail() {
  loading.value = true
  try {
    const res = await getPackageDetail(props.packageId)
    packageData.value = res.data
  } finally {
    loading.value = false
  }
}

// 生命周期
onMounted(() => {
  fetchPackageDetail()
})
</script>
```

### 组件命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 组件文件名 | PascalCase | `PackageList.vue` |
| 组件名 | PascalCase | `PackageList` |
| 组合式函数 | camelCase, use 前缀 | `usePagination` |
| Props | camelCase | `packageId` |
| Events | camelCase | `onSubmit` |

### 组件结构

```vue
<template>
  <!-- 模板 -->
</template>

<script setup lang="ts">
// 1. 关联需求注释
// 2. import 语句（外部库 > 内部模块 > 类型）
// 3. 类型定义
// 4. Props/Emits 定义
// 5. 注入（inject）
// 6. 响应式状态
// 7. 计算属性
// 8. 方法
// 9. 生命周期钩子
// 10. 监听器
</script>

<style scoped lang="scss">
/* 样式 */
</style>
```

---

## TypeScript 规范

### 类型定义

统一在 `models/` 目录定义类型：

```typescript
// models/package.ts
// 关联需求: REQ-001

/** 软件包状态 */
export enum PackageStatus {
  DRAFT = 0,
  PENDING_REVIEW = 1,
  PUBLISHED = 2,
  ARCHIVED = 3
}

/** 软件包 */
export interface Package {
  id: number
  name: string
  version: string
  description?: string
  status: PackageStatus
  createdAt: string
  updatedAt: string
}

/** 创建软件包请求 */
export interface CreatePackageRequest {
  name: string
  version: string
  description?: string
}

/** 创建软件包响应 */
export interface CreatePackageResponse {
  id: number
  name: string
  status: PackageStatus
}
```

### Props 类型定义

```vue
<script setup lang="ts">
import type { Package } from '@/models/package'

interface Props {
  /** 软件包数据 */
  data: Package
  /** 是否可编辑 */
  editable?: boolean
  /** 加载状态 */
  loading?: boolean
}

withDefaults(defineProps<Props>(), {
  editable: false,
  loading: false
})
</script>
```

---

## 状态管理（Pinia）

### Store 结构

```typescript
// stores/package.ts
// 关联需求: REQ-001

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Package, PackageStatus } from '@/models/package'
import { getPackageList, createPackage } from '@/api/package'

export const usePackageStore = defineStore('package', () => {
  // State
  const packages = ref<Package[]>([])
  const loading = ref(false)
  const total = ref(0)

  // Getters
  const publishedPackages = computed(() =>
    packages.value.filter(p => p.status === PackageStatus.PUBLISHED)
  )

  // Actions
  async function fetchPackages(params: { page: number; size: number }) {
    loading.value = true
    try {
      const res = await getPackageList(params)
      packages.value = res.data.items
      total.value = res.data.pagination.total
    } finally {
      loading.value = false
    }
  }

  async function addPackage(data: CreatePackageRequest) {
    const res = await createPackage(data)
    packages.value.unshift(res.data)
    total.value++
  }

  return {
    packages,
    loading,
    total,
    publishedPackages,
    fetchPackages,
    addPackage
  }
})
```

### Store 使用

```vue
<script setup lang="ts">
import { usePackageStore } from '@/stores/package'
import { storeToRefs } from 'pinia'

const packageStore = usePackageStore()
// 使用 storeToRefs 解构保持响应性
const { packages, loading } = storeToRefs(packageStore)
// 方法直接解构
const { fetchPackages } = packageStore
</script>
```

---

## API 封装

### 请求封装

```typescript
// utils/request.ts
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    throw new Error(message)
  },
  (error) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
    }
    return Promise.reject(error)
  }
)

export default request
```

### API 定义

```typescript
// api/package.ts
// 关联需求: REQ-001

import request from '@/utils/request'
import type {
  Package,
  CreatePackageRequest,
  CreatePackageResponse,
  PackageListResponse
} from '@/models/package'

export function getPackageList(params: { page: number; size: number }) {
  return request.get<PackageListResponse>('/packages', { params })
}

export function getPackageDetail(id: string) {
  return request.get<Package>(`/packages/${id}`)
}

export function createPackage(data: CreatePackageRequest) {
  return request.post<CreatePackageResponse>('/packages', data)
}

export function updatePackage(id: string, data: Partial<CreatePackageRequest>) {
  return request.put<Package>(`/packages/${id}`, data)
}

export function deletePackage(id: string) {
  return request.delete(`/packages/${id}`)
}
```

---

## 路由规范

### 路由配置

```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'packages',
        name: 'Packages',
        component: () => import('@/views/packages/index.vue'),
        meta: { title: '软件包管理' }
      },
      {
        path: 'packages/create',
        name: 'CreatePackage',
        component: () => import('@/views/packages/create.vue'),
        meta: { title: '创建软件包' }
      },
      {
        path: 'packages/:id',
        name: 'PackageDetail',
        component: () => import('@/views/packages/detail.vue'),
        meta: { title: '软件包详情' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (!to.meta.public && !authStore.isAuthenticated) {
    next('/login')
  } else {
    next()
  }
})

export default router
```

### 路由懒加载

所有页面组件必须使用懒加载：

```typescript
component: () => import('@/views/packages/index.vue')
```

---

## 样式规范

### 使用 SCSS

```vue
<style scoped lang="scss">
.package-list {
  padding: 20px;

  &__header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20px;
  }

  &__title {
    font-size: 20px;
    font-weight: bold;
  }
}
</style>
```

### BEM 命名规范

```
.block {}           // 块
.block__element {}  // 元素
.block--modifier {} // 修饰符
```

### 样式变量

```scss
// assets/styles/variables.scss
:root {
  --primary-color: #409eff;
  --success-color: #67c23a;
  --warning-color: #e6a23c;
  --danger-color: #f56c6c;
  --text-color: #303133;
  --border-color: #dcdfe6;
}
```

---

## 变更记录

| 时间 | 变更内容 | 变更原因 | 影响范围 | 处理人 | 状态 |
|------|----------|----------|----------|--------|------|
| 2026-03-17 | 初始版本 | 建立前端规范 | 前端开发 | 前端负责人 | 已完成 |

