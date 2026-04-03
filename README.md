# OSRM - 开源软件仓库管理系统

[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0-3178C6)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **OSRM**（Open Source Repository Manager）是一个企业级开源软件仓库管理平台，实现开源软件的全生命周期管理（录入-审核-发布-订购-使用）。

---

## 🎯 项目简介

企业内部开源软件管理存在以下痛点：
- 开源软件分散存储，缺乏统一管理
- 开发人员重复下载，无法追踪使用情况
- 安全合规难以把控，缺乏审计手段
- 不知道哪些业务系统使用了哪些开源组件

**OSRM** 构建统一的开源软件管理平台，实现：
- ✅ 统一纳管 Harbor、Nexus、NAS 等存储后端
- ✅ 开源软件全生命周期管理
- ✅ 使用情况的追踪与统计
- ✅ 安全合规的基础支撑

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        展示层 (Presentation)                  │
│         Vue 3 + Element Plus + TypeScript + Vite             │
├─────────────────────────────────────────────────────────────┤
│                         网关层 (Gateway)                      │
│              Nginx + 负载均衡 + 限流/熔断                     │
├─────────────────────────────────────────────────────────────┤
│                        应用层 (Application)                   │
│         Spring Boot + Spring Security + JWT                   │
├─────────────────────────────────────────────────────────────┤
│                         领域层 (Domain)                       │
│         DDD + 领域服务 + 领域事件                             │
├─────────────────────────────────────────────────────────────┤
│                      基础设施层 (Infrastructure)              │
│    PostgreSQL + Redis + MinIO + Harbor + Nexus               │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 项目结构

```
osrm/
├── osrm-backend/           # 后端服务（Spring Boot）
│   ├── osrm-common/        # 公共模块
│   ├── osrm-domain/        # 领域层
│   ├── osrm-application/   # 应用层
│   ├── osrm-infrastructure/# 基础设施层
│   ├── osrm-interfaces/    # 接口层
│   └── osrm-starter/       # 启动模块
│
├── osrm-frontend/          # 前端应用（Vue 3）
│   ├── src/
│   │   ├── api/           # API 接口
│   │   ├── components/    # 组件
│   │   ├── views/         # 页面
│   │   ├── stores/        # Pinia 状态管理
│   │   └── utils/         # 工具函数
│   └── package.json
│
├── osrm-mcp/               # MCP 服务层
│   ├── packages/
│   │   ├── shared/        # 共享模块
│   │   ├── portal/        # 公开服务
│   │   ├── api/           # API 服务
│   │   └── gateway/       # 网关
│   └── package.json
│
├── osrm-deploy/            # 部署配置
│   ├── docker/            # Docker 配置
│   └── kubernetes/        # K8s 配置
│
└── docs/                   # 项目文档
    ├── requirements/      # 需求文档
    ├── design/            # 设计文档
    ├── standards/         # 规范文档
    └── summaries/         # 功能总结
```

---

## 🚀 快速开始

### 环境要求

- **Java**: 21+
- **Node.js**: 20+
- **PostgreSQL**: 15+
- **Redis**: 7+
- **Maven**: 3.9+

### 后端启动

```bash
cd osrm-backend
mvn clean install
mvn spring-boot:run -pl osrm-starter
```

### 前端启动

```bash
cd osrm-frontend
npm install
npm run dev
```

### MCP 服务启动

```bash
cd osrm-mcp
npm install
npm run build
PORT=3000 OSRM_BASE_URL=http://localhost:8080/api/v1 \
  node packages/gateway/dist/index.js
```

---

## 📋 核心功能

### 1. 存储后端管理
- 纳管 Harbor、Nexus、NAS 等存储后端
- 自动健康检测（每5分钟）
- 连接测试与配置管理

### 2. 开源软件管理
- 软件录入（Docker/Maven/NPM/PyPI/文件）
- 版本管理与生命周期控制
- 附件管理（文档、安全报告）

### 3. 软件审核发布
- 审批工作流（草稿 → 待审核 → 已发布）
- 版本发布与下架管理
- 审批历史记录

### 4. 软件门户
- 公开浏览与搜索
- 软件详情与版本信息
- 热门软件推荐

### 5. 订购与下载
- 订购申请与审批
- 下载令牌管理（有效期/次数控制）
- 多种获取方式（docker pull/maven/npm/pip）

### 6. 使用追踪与统计
- 使用登记与追踪
- 统计报表（软件排行、业务系统分布）
- 运营看板与可视化

### 7. MCP 服务层
- 28个工具函数（Portal 6个 + API 22个）
- Streamable HTTP 协议支持
- 无侵入式后端集成

---

## 🛡️ 安全特性

- **认证**: JWT Token + Session 管理
- **传输**: HTTPS 加密传输
- **权限**: RBAC 权限模型
- **审计**: 完整的操作日志记录
- **密码策略**: 8位以上、字母+数字、90天过期

---

## 📚 文档导航

### 需求文档
- [完整产品需求](docs/requirements/osrm-requirements.md) - OSRM 整体产品需求

### 设计文档
- [系统架构设计](docs/design/architecture/system-architecture.md)
- [部署架构设计](docs/design/architecture/deployment-architecture.md)
- [技术架构设计](docs/design/architecture/technical-architecture.md)

### 开发规范
- [开发流程规范](docs/standards/development-process.md)
- [API 设计规范](docs/standards/api-design.md)
- [数据库规范](docs/standards/database.md)
- [前端开发规范](docs/standards/frontend.md)

### 功能总结
- [MCP 服务总结](docs/summaries/mcp-service-summary.md)

---

## 🤝 参与贡献

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📄 许可证

本项目采用 [MIT](LICENSE) 许可证开源。

---

## 💬 联系方式

如有问题或建议，欢迎通过以下方式联系：

- 项目 Issues: [GitHub Issues](https://github.com/haibin1003/osrm/issues)
- 邮箱: {your-email}

---

<p align="center">Made with ❤️ by OSRM Team</p>
