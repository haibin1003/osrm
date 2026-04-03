# REQ-600 软件门户开发总结

**功能ID**: REQ-600
**完成日期**: 2026-03-21
**开发阶段**: Phase 1–7（完整）

---

## 一、实现清单

### 后端实现

| 组件 | 文件路径 | 说明 |
|------|----------|------|
| 门户控制器 | `interfaces/rest/PortalController.java` | 软件列表（PUBLISHED）、软件详情、统计数据 |
| 门户 API | `api/portal.ts`（前端） | 封装门户相关接口调用 |

### 前端实现

| 文件 | 说明 |
|------|------|
| `views/portal/SoftwarePortal.vue` | 门户主页：卡片式软件列表、搜索、类型过滤、统计栏 |
| `views/portal/SoftwareDetail.vue` | 软件详情：基本信息、版本列表、获取命令、订购入口 |

---

## 二、核心功能

### 软件门户首页

- 卡片式展示已发布软件包（`.pkg-card`）
- 顶部统计栏：总发布数、各类型数量
- 关键词搜索（`input[placeholder="搜索软件包..."]`）
- 类型过滤（RadioButton：全部/Docker/Helm/Maven/NPM/PyPI/通用）
- 分页（12/24/48 每页）

### 软件详情页

- 展示包名、类型、状态、描述
- 版本列表（含版本号、发布说明、发布时间）
- 根据软件类型展示获取命令（docker pull / helm pull / maven / npm / pip）
- 订购此软件按钮（跳转申请订购页）

---

## 三、REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/portal/packages` | 已发布软件列表（支持关键词/类型过滤分页） |
| GET | `/api/v1/portal/packages/{id}` | 软件详情 |
| GET | `/api/v1/portal/stats` | 门户统计数据（PortalStats） |

### PortalStats 字段
```
publishedCount, dockerCount, helmCount, mavenCount, npmCount, pypiCount, genericCount
```

---

## 四、遗留问题

- 无重大遗留问题
- 详情页获取命令中 `<registry>`、`<version>` 为占位符，实际应由后端注入真实地址
