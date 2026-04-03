# OSRM 项目文档

本文档目录包含 OSRM（开源软件仓库管理）项目的完整开发规范和文档。

---

## 文档导航

### 快速开始

如果你是新加入的开发者，请按以下顺序阅读：

1. **[项目概览](../CLAUDE.md)** - 了解项目背景和核心概念
2. **[开发流程规范](standards/development-process.md)** - 了解五阶段开发流程
3. **[测试规范](standards/testing.md)** - 了解 TDD 流程和测试要求

### 规范文档

| 文档 | 说明 | 适用场景 |
|------|------|----------|
| [开发流程规范](standards/development-process.md) | 五阶段开发流程详解 | 所有开发工作 |
| [测试规范](standards/testing.md) | TDD流程、测试类型、覆盖率要求 | 编写代码和测试 |
| [需求变更管理](standards/requirements-change.md) | 变更记录格式、一致性检查 | 需求变更时 |
| [文档编写规范](standards/documentation.md) | 文档格式、模板 | 编写任何文档 |
| [API 设计规范](standards/api-design.md) | RESTful设计、错误码、版本控制 | 设计 API |
| [数据库规范](standards/database.md) | 命名规范、迁移管理 | 数据库设计 |
| [Vue 前端规范](standards/frontend.md) | Vue3、Composition API、Pinia | 前端开发 |
| [代码审查规范](standards/code-review.md) | 审查流程、审查清单 | 代码审查 |

### 需求文档

- [完整产品需求](requirements/osrm-requirements.md) - OSRM 整体产品需求
- [功能需求目录](requirements/features/) - 按功能拆分的需求文档

### 设计文档

| 目录 | 说明 |
|------|------|
| [架构设计](design/architecture/) | 系统架构、组件设计 |
| [数据库设计](design/database/) | 数据模型、表结构设计 |
| [API 设计](design/api/) | 接口定义、请求/响应规范 |

### 功能总结

- [功能总结目录](summaries/) - 各功能的实现总结

---

## 开发流程速查

### 五阶段流程

```
Phase 1: 需求设计
    ↓ 输出: /docs/requirements/features/{feature-id}.md
Phase 2: 技术设计
    ↓ 输出: /docs/design/{architecture,database,api}/{feature-id}-*.md
Phase 3: TDD编码
    ↓ 输出: 源代码 + 测试代码 (覆盖率≥80%)
Phase 4: 代码审查
    ↓ 输出: 审查通过标记
Phase 5: 功能总结
    ↓ 输出: /docs/summaries/{feature-id}-summary.md
```

### 关键检查点

| 阶段 | 检查点 |
|------|--------|
| 需求设计 | 需求编号唯一、验收标准可测试 |
| 技术设计 | 设计评审通过 |
| 编码 | 测试覆盖率 ≥80%、需求编号关联 |
| 代码审查 | 审查清单全部勾选 |
| 功能总结 | 实现清单与需求一致 |

---

## 文档模板

### 可用模板

| 模板 | 路径 | 用途 |
|------|------|------|
| 需求文档 | [standards/documentation.md](standards/documentation.md) | 编写功能需求 |
| 设计文档 | [standards/documentation.md](standards/documentation.md) | 编写技术设计 |
| API 文档 | [standards/documentation.md](standards/documentation.md) | 编写 API 定义 |
| 功能总结 | [standards/documentation.md](standards/documentation.md) | 编写功能总结 |

---

## 变更记录

### 文档变更

| 时间 | 变更内容 | 处理人 |
|------|----------|--------|
| 2026-03-17 | 创建文档目录结构，添加所有规范文档 | 技术负责人 |

---

## 联系方式

如有文档相关问题，请联系：

- **技术负责人**: {姓名}
- **前端负责人**: {姓名}
- **产品经理**: {姓名}

