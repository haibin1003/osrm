# 存储后端管理前端设计

## 页面结构

```
src/views/storage/
├── Index.vue           # 存储后端列表页
├── Create.vue          # 新增存储后端页
├── Detail.vue          # 存储后端详情页
└── components/
    ├── StorageCard.vue     # 存储后端卡片组件
    ├── BackendForm.vue     # 存储后端表单组件
    └── HealthStatus.vue    # 健康状态显示组件
```

## 组件设计

### 1. StorageCard 组件

**Props**:
| 属性 | 类型 | 说明 |
|------|------|------|
| backend | StorageBackend | 存储后端数据 |

**Events**:
| 事件 | 说明 |
|------|------|
| view | 查看详情 |
| edit | 编辑 |
| delete | 删除 |
| setDefault | 设为默认 |
| healthCheck | 健康检查 |

**UI设计**:
- 卡片布局，一行3-4个卡片
- 顶部显示类型图标（Harbor/Nexus/NAS）
- 显示后端名称和编码
- 状态标签（启用/停用）
- 健康状态指示器（绿/红/灰）
- 默认标记（如果是默认）
- 操作按钮组

### 2. BackendForm 组件

**Props**:
| 属性 | 类型 | 说明 |
|------|------|------|
| mode | 'create' \| 'edit' | 模式 |
| initialData | Partial<StorageBackend> | 初始数据（编辑模式） |

**Events**:
| 事件 | 参数 | 说明 |
|------|------|------|
| submit | StorageBackendFormData | 表单提交 |
| cancel | - | 取消 |

**表单字段动态显示**:
| 字段 | Harbor | Nexus | NAS |
|------|--------|-------|-----|
| 服务端点 | URL | URL | 路径 |
| 命名空间 | 项目名 | 仓库名 | - |
| Access Key | 用户名 | 用户名 | - |
| Secret Key | 密码 | 密码 | - |

### 3. 列表页面布局

**筛选区**:
- 搜索框（按名称）
- 类型筛选下拉框
- 状态筛选下拉框
- 新增按钮

**内容区**:
- 卡片网格布局
- 空状态提示

## 类型定义

```typescript
// src/types/storage.ts

export type BackendType = 'HARBOR' | 'NEXUS' | 'NAS';

export type HealthStatus = 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN';

export interface StorageBackend {
  id: number;
  backendCode: string;
  backendName: string;
  backendType: BackendType;
  endpoint: string;
  namespace?: string;
  accessKey?: string;
  secretKey?: string;
  isDefault: boolean;
  enabled: boolean;
  healthStatus: HealthStatus;
  lastHealthCheck?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface StorageBackendFormData {
  backendName: string;
  backendType: BackendType;
  endpoint: string;
  namespace?: string;
  accessKey?: string;
  secretKey?: string;
  isDefault: boolean;
  description?: string;
}

export interface BackendQueryParams {
  keyword?: string;
  type?: BackendType;
  enabled?: boolean;
  page?: number;
  size?: number;
}
```

## API 调用封装

```typescript
// src/api/storage.ts

import request from './request';
import type { StorageBackend, StorageBackendFormData, BackendQueryParams } from '@/types/storage';

export const storageApi = {
  // 获取列表
  getList(params: BackendQueryParams) {
    return request.get<PageResult<StorageBackend>>('/v1/storage/backends', { params });
  },

  // 获取详情
  getDetail(id: number) {
    return request.get<StorageBackend>(`/v1/storage/backends/${id}`);
  },

  // 创建
  create(data: StorageBackendFormData) {
    return request.post<StorageBackend>('/v1/storage/backends', data);
  },

  // 更新
  update(id: number, data: Partial<StorageBackendFormData>) {
    return request.put<StorageBackend>(`/v1/storage/backends/${id}`, data);
  },

  // 删除
  delete(id: number) {
    return request.delete(`/v1/storage/backends/${id}`);
  },

  // 健康检查
  healthCheck(id: number) {
    return request.post<{ status: string; message: string }>(`/v1/storage/backends/${id}/health-check`);
  },

  // 设为默认
  setDefault(id: number) {
    return request.put(`/v1/storage/backends/${id}/default`);
  },

  // 启用/停用
  setEnabled(id: number, enabled: boolean) {
    return request.put(`/v1/storage/backends/${id}/enable`, null, { params: { enabled } });
  }
};
```

## 路由配置

```typescript
// src/router/index.ts

{
  path: 'system/storage',
  name: 'StorageBackend',
  component: () => import('@/views/storage/Index.vue'),
  meta: { title: '存储配置', permission: 'storage:read' }
},
{
  path: 'system/storage/create',
  name: 'StorageBackendCreate',
  component: () => import('@/views/storage/Create.vue'),
  meta: { title: '新增存储后端', permission: 'storage:create' }
},
{
  path: 'system/storage/:id',
  name: 'StorageBackendDetail',
  component: () => import('@/views/storage/Detail.vue'),
  meta: { title: '存储后端详情', permission: 'storage:read' }
}
```
