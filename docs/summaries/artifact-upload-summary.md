# 制品上传功能开发总结

**功能ID**: 制品上传（跨 REQ-300）
**完成日期**: 2026-03-21
**开发阶段**: Phase 1–7（完整）

---

## 一、实现清单

### 后端实现

| 组件 | 文件路径 | 说明 |
|------|----------|------|
| 上传结果 DTO | `application/artifact/dto/ArtifactUploadResult.java` | success/filePath/fileSize/md5Hash/downloadCommand/message |
| 上传应用服务 | `application/artifact/service/ArtifactUploadService.java` | 路由入口：按 storageBackendId 查 StorageBackend，分发到对应客户端 |
| Nexus 客户端 | `infrastructure/storage/NexusArtifactClient.java` | 上传 Maven/NPM/PyPI/Raw 到 Nexus |
| Harbor 客户端 | `infrastructure/storage/HarborArtifactClient.java` | 生成 docker push 命令 + 连接验证（Docker 镜像不直接上传文件） |
| 上传控制器 | `interfaces/rest/ArtifactUploadController.java` | `POST /api/v1/artifacts/upload`，需 `package:create` 权限；`GET /api/v1/artifacts/download/{versionId}` |

### 前端实现

| 文件 | 说明 |
|------|------|
| `api/artifact.ts` | `upload(packageId, versionId, file)` - multipart/form-data，5分钟超时 |
| `views/software/Packages.vue` | 版本管理弹窗内 `el-upload` 按钮 + 上传结果弹窗 |

---

## 二、路由逻辑

```
上传文件
  ↓
ArtifactUploadService.uploadArtifact(packageId, versionId, file)
  ↓ 查询 StorageBackend（storageBackendId）
  ├─ type = NEXUS  → NexusArtifactClient（Maven/NPM/PyPI/Raw，按软件类型路由仓库）
  ├─ type = HARBOR → HarborArtifactClient（返回 docker push 命令）
  └─ 其他/本地      → 本地文件系统（target/uploads/）
```

### StorageBackend.configJson 格式
```json
{
  "mavenRepo": "maven-releases",
  "npmRepo": "npm-hosted",
  "pypiRepo": "pypi-hosted",
  "rawRepo": "raw-hosted"
}
```

---

## 三、上传结果展示

上传成功后前端弹出结果对话框，展示：
- 文件路径（`filePath`）
- 文件大小（格式化）
- MD5 校验值
- 下载/使用命令（可复制）

---

## 四、已验证的服务地址

| 服务 | 地址 | 账号 |
|------|------|------|
| Nexus | http://114.66.38.81:8081 | admin / 14cdf79a-e549-45c5-80de-245395c6c293 |
| Harbor | http://114.66.38.81:8080 | admin / Harbor12345 |

---

## 五、遗留问题

- Harbor 镜像为命令提示方式（返回 push 命令），未实现服务端代理 push
- 大文件上传（>100MB）未做分片，依赖前端 5分钟超时配置
