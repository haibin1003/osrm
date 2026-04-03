# REQ-500 订购管理开发总结

**功能ID**: REQ-500
**完成日期**: 2026-03-21
**开发阶段**: Phase 1–7（完整）

---

## 一、实现清单

### 后端实现

| 组件 | 文件路径 | 说明 |
|------|----------|------|
| 订购实体 | `domain/subscription/entity/Subscription.java` | 含 packageId、versionId、businessSystemId、useScene、status |
| 订购状态枚举 | `domain/subscription/entity/SubscriptionStatus.java` | PENDING / APPROVED / REJECTED / CANCELLED |
| 订购仓库 | `domain/subscription/repository/SubscriptionRepository.java` | 按申请人/状态/软件查询 |
| 订购应用服务 | `application/subscription/service/SubscriptionAppService.java` | 申请、通过、拒绝、取消、查询 |
| 请求 DTO | `application/subscription/dto/request/CreateSubscriptionRequest.java` | packageId、versionId、businessSystemId、useScene |
| 响应 DTO | `application/subscription/dto/response/SubscriptionDTO.java` | 含包名、版本、系统名、状态等展示字段 |
| 订购控制器 | `interfaces/rest/SubscriptionController.java` | 完整 CRUD + 审批端点 |

### 前端实现

| 文件 | 说明 |
|------|------|
| `views/subscription/ApplySubscription.vue` | 申请订购表单（软件包/版本/业务系统/使用场景） |
| `views/subscription/MySubscriptions.vue` | 我的订购列表，审批通过后展示"下载令牌"按钮 |
| `views/subscription/PendingApproval.vue` | 待审批列表（共享 REQ-400） |
| `views/subscription/ApprovalHistory.vue` | 审批历史（共享 REQ-400） |

---

## 二、核心流程

```
用户在门户选中软件 → 申请订购（填写业务系统/使用场景）
  → 提交后状态 PENDING → 管理员在审批中心审批
  → APPROVED：用户可获取下载令牌
  → REJECTED：申请失败，可重新申请
```

---

## 三、REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/subscriptions` | 提交订购申请 |
| GET | `/api/v1/subscriptions/my` | 我的订购列表 |
| GET | `/api/v1/subscriptions/pending` | 待审批列表（管理员） |
| GET | `/api/v1/subscriptions/history` | 审批历史 |
| POST | `/api/v1/subscriptions/{id}/approve` | 审批通过 |
| POST | `/api/v1/subscriptions/{id}/reject` | 审批拒绝 |

---

## 四、遗留问题

- 无重大遗留问题
- 下载令牌功能为展示占位，实际令牌生成可后续完善
