# OSRM 项目代码框架设计

本文档定义 OSRM 项目的前后端代码结构、目录组织和命名规范。

---

## 文档信息

- **作者**: OSRM 架构团队
- **创建时间**: 2026-03-18
- **版本**: v1.0

---

## 1. 后端项目结构 (Spring Boot)

### 1.1 整体目录结构

```
osrm-backend/
├── pom.xml                                 # Maven 配置
├── README.md                               # 项目说明
├── Dockerfile                              # 容器化构建
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── osrm/
│   │   │           ├── OsrmApplication.java                    # 启动类
│   │   │           ├──
│   │   │           ├── common/                                 # 公共模块
│   │   │           │   ├── config/                             # 通用配置
│   │   │           │   │   ├── JacksonConfig.java
│   │   │           │   │   ├── RedisConfig.java
│   │   │           │   │   ├── WebMvcConfig.java
│   │   │           │   │   └── AsyncConfig.java
│   │   │           │   ├── constant/                           # 常量定义
│   │   │           │   │   ├── CommonConstant.java
│   │   │           │   │   ├── CacheConstant.java
│   │   │           │   │   └── SecurityConstant.java
│   │   │           │   ├── exception/                          # 异常处理
│   │   │           │   │   ├── BizException.java
│   │   │           │   │   ├── GlobalExceptionHandler.java
│   │   │           │   │   └── ErrorCode.java
│   │   │           │   ├── model/                              # 通用模型
│   │   │           │   │   ├── ApiResponse.java
│   │   │           │   │   ├── PageRequest.java
│   │   │           │   │   └── PageResult.java
│   │   │           │   ├── util/                               # 工具类
│   │   │           │   │   ├── JwtUtil.java
│   │   │           │   │   ├── SecurityUtil.java
│   │   │           │   │   └── JsonUtil.java
│   │   │           │   └── validation/                         # 校验注解
│   │   │           │
│   │   │           ├── domain/                                 # 领域层 (按模块划分)
│   │   │           │   ├── user/                               # 用户领域
│   │   │           │   │   ├── entity/                         # 领域实体
│   │   │           │   │   │   ├── User.java
│   │   │           │   │   │   ├── Role.java
│   │   │           │   │   │   └── Permission.java
│   │   │           │   │   ├── repository/                     # 仓储接口
│   │   │           │   │   │   ├── UserRepository.java
│   │   │           │   │   │   └── RoleRepository.java
│   │   │           │   │   ├── service/                        # 领域服务
│   │   │           │   │   │   ├── UserDomainService.java
│   │   │           │   │   │   └── AuthDomainService.java
│   │   │           │   │   └── event/                          # 领域事件
│   │   │           │   │       └── UserLoggedInEvent.java
│   │   │           │   │
│   │   │           │   ├── package-info/                       # 软件包领域
│   │   │           │   │   ├── entity/
│   │   │           │   │   ├── repository/
│   │   │           │   │   └── service/
│   │   │           │   │
│   │   │           │   ├── subscription/                       # 订购领域
│   │   │           │   │   ├── entity/
│   │   │           │   │   ├── repository/
│   │   │           │   │   └── service/
│   │   │           │   │
│   │   │           │   ├── storage/                            # 存储领域
│   │   │           │   ├── business/                           # 业务系统领域
│   │   │           │   └── compliance/                         # 合规领域
│   │   │           │
│   │   │           ├── application/                            # 应用层
│   │   │           │   ├── user/                               # 用户应用服务
│   │   │           │   │   ├── dto/                            # 数据传输对象
│   │   │           │   │   │   ├── request/
│   │   │           │   │   │   │   ├── LoginRequest.java
│   │   │           │   │   │   │   └── RefreshTokenRequest.java
│   │   │           │   │   │   └── response/
│   │   │           │   │   │       ├── LoginResponse.java
│   │   │           │   │   │       └── UserInfoResponse.java
│   │   │           │   │   ├── mapper/                         # DTO/Entity 转换
│   │   │           │   │   │   └── UserMapper.java
│   │   │           │   │   └── service/                        # 应用服务
│   │   │           │   │       ├── AuthAppService.java
│   │   │           │   │       └── UserAppService.java
│   │   │           │   │
│   │   │           │   ├── package-info/                       # 软件包应用服务
│   │   │           │   ├── subscription/                       # 订购应用服务
│   │   │           │   └── storage/                            # 存储应用服务
│   │   │           │
│   │   │           ├── interfaces/                             # 接口层 (Controller)
│   │   │           │   ├── rest/                               # REST API
│   │   │           │   │   ├── AuthController.java
│   │   │           │   │   ├── UserController.java
│   │   │           │   │   ├── PackageController.java
│   │   │           │   │   └── SubscriptionController.java
│   │   │           │   └── web/                                # WebSocket (预留)
│   │   │           │
│   │   │           ├── infrastructure/                         # 基础设施层
│   │   │           │   ├── config/                             # 基础设施配置
│   │   │           │   │   ├── JpaConfig.java
│   │   │           │   │   ├── SecurityConfig.java
│   │   │           │   │   └── RabbitMqConfig.java
│   │   │           │   ├── persistence/                        # 持久化实现
│   │   │           │   │   ├── jpa/
│   │   │           │   │   │   ├── UserJpaRepository.java
│   │   │           │   │   │   └── RoleJpaRepository.java
│   │   │           │   │   └── mybatis/                        # (备选)
│   │   │           │   ├── messaging/                          # 消息队列
│   │   │           │   │   ├── RabbitMqPublisher.java
│   │   │           │   │   └── EventListener.java
│   │   │           │   ├── cache/                              # 缓存实现
│   │   │           │   │   └── RedisCacheService.java
│   │   │           │   ├── security/                           # 安全实现
│   │   │           │   │   ├── JwtTokenProvider.java
│   │   │           │   │   ├── JwtAuthenticationFilter.java
│   │   │           │   │   └── UserDetailsServiceImpl.java
│   │   │           │   └── storage/                            # 存储后端适配器
│   │   │           │       ├── MinioStorageAdapter.java
│   │   │           │       ├── HarborStorageAdapter.java
│   │   │           │       └── NexusStorageAdapter.java
│   │   │           │
│   │   │           └── support/                                # 支撑模块
│   │   │               ├── audit/                              # 审计日志
│   │   │               ├── job/                                # 定时任务
│   │   │               └── event/                              # 事件处理
│   │   │
│   │   └── resources/
│   │       ├── application.yml                                 # 主配置
│   │       ├── application-dev.yml                             # 开发环境
│   │       ├── application-test.yml                            # 测试环境
│   │       ├── application-prod.yml                            # 生产环境
│   │       ├── db/
│   │       │   └── migration/                                  # Flyway 迁移脚本
│   │       │       ├── V1__init_schema.sql
│   │       │       └── V2__init_data.sql
│   │       ├── logback-spring.xml                              # 日志配置
│   │       └── banner.txt
│   │
│   └── test/                                                   # 测试代码
│       ├── java/
│       │   └── com/
│       │       └── osrm/
│       │           ├── unit/                                   # 单元测试
│       │           │   ├── domain/
│       │           │   └── application/
│       │           ├── integration/                            # 集成测试
│       │           │   ├── controller/
│       │           │   └── repository/
│       │           └── e2e/                                    # E2E 测试
│       └── resources/
│           └── application-test.yml
│
└── docs/                                                       # 项目文档
    ├── api/                                                    # API 文档
    └── development/                                            # 开发文档
```

### 1.2 包命名规范

| 层级 | 包名 | 说明 |
|------|------|------|
| 领域层 | `com.osrm.domain.{module}` | 业务实体、仓储接口、领域服务 |
| 应用层 | `com.osrm.application.{module}` | DTO、应用服务、Mapper |
| 接口层 | `com.osrm.interfaces.rest` | REST Controller |
| 基础设施 | `com.osrm.infrastructure.{type}` | 技术实现细节 |
| 公共模块 | `com.osrm.common.{type}` | 配置、常量、工具、异常 |

### 1.3 类命名规范

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 实体类 | `{Name}Entity` 或直接 `{Name}` | `User`, `SoftwarePackage` |
| 仓储接口 | `{Name}Repository` | `UserRepository` |
| 应用服务 | `{Name}AppService` | `AuthAppService` |
| 领域服务 | `{Name}DomainService` | `UserDomainService` |
| Controller | `{Name}Controller` | `AuthController` |
| DTO Request | `{Action}{Name}Request` | `LoginRequest` |
| DTO Response | `{Name}Response` | `UserInfoResponse` |
| Mapper | `{Name}Mapper` | `UserMapper` |

### 1.4 依赖关系

```
┌─────────────────────────────────────────────────────────────────────┐
│                        分层依赖关系                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   接口层 (interfaces)                                                │
│      │                                                               │
│      ▼                                                               │
│   应用层 (application) ──▶ 领域层 (domain)                           │
│      │                      │                                        │
│      └──────────────────────┘                                        │
│                 │                                                    │
│                 ▼                                                    │
│   基础设施层 (infrastructure) ──▶ 领域层 (domain)                    │
│   (实现领域层仓储接口)        (仓储接口定义)                          │
│                                                                      │
│   依赖原则：上层依赖下层，领域层不依赖其他层                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. 前端项目结构 (Vue 3)

### 2.1 整体目录结构

```
osrm-frontend/
├── package.json                          # npm 配置
├── vite.config.ts                        # Vite 配置
├── tsconfig.json                         # TypeScript 配置
├── index.html
├── README.md
├── Dockerfile
├──
├── public/                               # 静态资源
│   ├── favicon.ico
│   └── images/
│
├── src/
│   ├── main.ts                           # 入口文件
│   ├── App.vue                           # 根组件
│   ├──
│   ├── api/                              # API 接口定义
│   │   ├── request.ts                    # Axios 封装
│   │   ├── types.ts                      # API 类型定义
│   │   ├── auth.ts                       # 认证相关接口
│   │   ├── user.ts                       # 用户相关接口
│   │   ├── package.ts                    # 软件包接口
│   │   ├── subscription.ts               # 订购接口
│   │   └── storage.ts                    # 存储接口
│   │
│   ├── components/                       # 公共组件
│   │   ├── common/                       # 通用组件
│   │   │   ├── AppHeader.vue
│   │   │   ├── AppSidebar.vue
│   │   │   ├── AppFooter.vue
│   │   │   ├── PageContainer.vue
│   │   │   ├── DataTable.vue
│   │   │   ├── SearchForm.vue
│   │   │   └── Pagination.vue
│   │   ├── business/                     # 业务组件
│   │   │   ├── PackageCard.vue
│   │   │   ├── PackageDetail.vue
│   │   │   ├── SubscriptionForm.vue
│   │   │   └── StorageConfig.vue
│   │   └── icons/                        # 图标组件
│   │
│   ├── composables/                      # 组合式函数
│   │   ├── useAuth.ts                    # 认证逻辑
│   │   ├── usePermission.ts              # 权限检查
│   │   ├── useTable.ts                   # 表格逻辑
│   │   ├── useForm.ts                    # 表单逻辑
│   │   └── useMessage.ts                 # 消息提示
│   │
│   ├── directives/                       # 自定义指令
│   │   ├── permission.ts                 # 权限指令 v-permission
│   │   └── loading.ts                    # 加载指令 v-loading
│   │
│   ├── layouts/                          # 布局组件
│   │   ├── DefaultLayout.vue             # 默认布局（侧边栏+头部）
│   │   ├── AuthLayout.vue                # 认证布局（无侧边栏）
│   │   └── FullscreenLayout.vue          # 全屏布局
│   │
│   ├── router/                           # 路由配置
│   │   ├── index.ts                      # 路由入口
│   │   ├── guards.ts                     # 路由守卫
│   │   └── routes.ts                     # 路由定义
│   │       ├── auth.ts                   # 认证路由
│   │       ├── package.ts                # 软件包路由
│   │       ├── subscription.ts           # 订购路由
│   │       └── system.ts                 # 系统管理路由
│   │
│   ├── stores/                           # Pinia 状态管理
│   │   ├── index.ts                      # Store 入口
│   │   ├── modules/
│   │   │   ├── auth.ts                   # 认证状态
│   │   │   ├── user.ts                   # 用户状态
│   │   │   ├── package.ts                # 软件包状态
│   │   │   ├── subscription.ts           # 订购状态
│   │   │   ├── storage.ts                # 存储状态
│   │   │   └── app.ts                    # 应用状态（主题、语言等）
│   │   └── plugins/
│   │       └── persist.ts                # 状态持久化
│   │
│   ├── styles/                           # 样式文件
│   │   ├── variables.scss                # SCSS 变量
│   │   ├── mixins.scss                   # SCSS Mixins
│   │   ├── global.scss                   # 全局样式
│   │   ├── element-plus.scss             # Element Plus 主题覆盖
│   │   └── tailwind.css                  # Tailwind 入口
│   │
│   ├── types/                            # TypeScript 类型
│   │   ├── api.ts                        # API 相关类型
│   │   ├── auth.ts                       # 认证类型
│   │   ├── user.ts                       # 用户类型
│   │   ├── package.ts                    # 软件包类型
│   │   ├── router.ts                     # 路由类型
│   │   └── global.d.ts                   # 全局类型声明
│   │
│   ├── utils/                            # 工具函数
│   │   ├── auth.ts                       # Token 处理
│   │   ├── cache.ts                      # 本地缓存
│   │   ├── date.ts                       # 日期处理
│   │   ├── validate.ts                   # 表单校验
│   │   ├── download.ts                   # 文件下载
│   │   └── constants.ts                  # 常量定义
│   │
│   └── views/                            # 页面视图
│       ├── auth/                         # 认证页面
│       │   ├── Login.vue
│       │   └── Callback.vue
│       │
│       ├── dashboard/                    # 仪表盘
│       │   └── Index.vue
│       │
│       ├── package/                      # 软件包管理
│       │   ├── List.vue
│       │   ├── Detail.vue
│       │   ├── Create.vue
│       │   └── Edit.vue
│       │
│       ├── subscription/                 # 订购管理
│       │   ├── MySubscriptions.vue
│       │   ├── Create.vue
│       │   └── Approval.vue
│       │
│       ├── storage/                      # 存储管理
│       │   ├── ConfigList.vue
│       │   └── ConfigDetail.vue
│       │
│       ├── business/                     # 业务系统
│       │   ├── List.vue
│       │   └── Detail.vue
│       │
│       ├── portal/                       # 软件门户
│       │   ├── Index.vue
│       │   ├── Search.vue
│       │   └── Detail.vue
│       │
│       └── system/                       # 系统管理
│           ├── UserManagement.vue
│           ├── RoleManagement.vue
│           └── SystemConfig.vue
│
└── docs/                                 # 前端文档
    ├── component.md                      # 组件使用文档
    └── development.md                    # 开发规范
```

### 2.2 组件命名规范

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 页面组件 | PascalCase，语义化 | `UserManagement.vue` |
| 公共组件 | PascalCase，带前缀 | `AppHeader.vue`, `DataTable.vue` |
| 组合式函数 | camelCase，use 前缀 | `useAuth.ts`, `useTable.ts` |
| Store | camelCase | `auth.ts`, `user.ts` |
| 工具函数 | camelCase | `formatDate.ts`, `validateEmail.ts` |

### 2.3 API 封装规范

```typescript
// src/api/request.ts
import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token 过期，尝试刷新或跳转登录
    }
    return Promise.reject(error)
  }
)

export default request
```

```typescript
// src/api/auth.ts
import request from './request'
import type { LoginRequest, LoginResponse } from './types'

export const authApi = {
  login(data: LoginRequest) {
    return request.post<LoginResponse>('/api/v1/auth/login', data)
  },

  logout() {
    return request.post('/api/v1/auth/logout')
  },

  refreshToken(refreshToken: string) {
    return request.post<LoginResponse>('/api/v1/auth/refresh', { refreshToken })
  },

  getCurrentUser() {
    return request.get('/api/v1/auth/me')
  }
}
```

### 2.4 Store 设计规范

```typescript
// src/stores/modules/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, TokenPair } from '@/types/auth'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const userInfo = ref<UserInfo | null>(null)

  // Getters
  const isAuthenticated = computed(() => !!accessToken.value)
  const hasPermission = (permission: string) => {
    return userInfo.value?.permissions?.includes(permission) ?? false
  }

  // Actions
  const login = async (credentials: { username: string; password: string }) => {
    const res = await authApi.login(credentials)
    setToken(res.data)
    userInfo.value = res.data.user
    return res
  }

  const logout = async () => {
    await authApi.logout()
    clearToken()
    userInfo.value = null
  }

  const setToken = (tokenPair: TokenPair) => {
    accessToken.value = tokenPair.accessToken
    refreshToken.value = tokenPair.refreshToken
    localStorage.setItem('accessToken', tokenPair.accessToken)
    localStorage.setItem('refreshToken', tokenPair.refreshToken)
  }

  const clearToken = () => {
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isAuthenticated,
    hasPermission,
    login,
    logout
  }
})
```

---

## 3. 模块划分对应关系

| 业务模块 | 后端包路径 | 前端视图路径 |
|----------|-----------|-------------|
| 用户中心 | `com.osrm.domain.user` | `src/views/auth/` |
| 存储管理 | `com.osrm.domain.storage` | `src/views/storage/` |
| 业务系统 | `com.osrm.domain.business` | `src/views/business/` |
| 软件管理 | `com.osrm.domain.package-info` | `src/views/package/` |
| 订购管理 | `com.osrm.domain.subscription` | `src/views/subscription/` |
| 软件门户 | (共用软件包领域) | `src/views/portal/` |
| 系统管理 | (跨领域) | `src/views/system/` |

---

## 4. 开发环境配置

### 4.1 后端开发环境

```yaml
# application-dev.yml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/osrm_dev
    username: osrm
    password: osrm123

  redis:
    host: localhost
    port: 6379

  jpa:
    hibernate:
      ddl-auto: validate  # 开发环境使用 Flyway，不自动建表
    show-sql: true

logging:
  level:
    com.osrm: DEBUG
```

### 4.2 前端开发环境

```typescript
// .env.development
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=OSRM Dev
```

---

## 5. 构建与部署

### 5.1 后端构建

```bash
# 本地构建
mvn clean package -DskipTests

# Docker 构建
docker build -t osrm-backend:latest .

# 运行
docker run -p 8080:8080 osrm-backend:latest
```

### 5.2 前端构建

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build

# Docker 构建
docker build -t osrm-frontend:latest .
```

---

## 附录：相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| API 规范 | `docs/standards/api-design.md` | RESTful API 设计规范 |
| 前端规范 | `docs/standards/frontend.md` | Vue 3 开发规范 |
| 数据库规范 | `docs/standards/database.md` | 数据库设计规范 |
| 代码审查 | `docs/standards/code-review.md` | 代码审查规范 |
