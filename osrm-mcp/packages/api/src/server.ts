import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { z } from 'zod';
import { type AxiosInstance } from 'axios';
import { OsrmApiError } from '@osrm-mcp/shared';

// ── 全部 22 个工具（workflow 12 + admin 10）────────────────────────────────────
const tools = [
  // ── 订购类 ─────────────────────────────────────────────────────────────────
  {
    name: 'apply_subscription',
    description: '提交软件订购申请。需要指定软件包版本、关联的业务系统和使用场景描述。申请提交后进入审批流程。',
    inputSchema: {
      type: 'object',
      properties: {
        package_id: { type: 'number', description: '软件包ID' },
        version_id: { type: 'number', description: '版本ID' },
        system_id: { type: 'number', description: '业务系统ID（软件将用于哪个系统）' },
        use_scene: { type: 'string', description: '使用场景描述（说明为何需要该软件）' },
      },
      required: ['package_id', 'version_id', 'system_id', 'use_scene'],
    },
  },
  {
    name: 'list_my_subscriptions',
    description: '查询当前用户的订购记录列表，可按状态过滤。',
    inputSchema: {
      type: 'object',
      properties: {
        status: { type: 'string', description: '订购状态过滤', enum: ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'] },
        page: { type: 'number', description: '页码，从1开始，默认1' },
        size: { type: 'number', description: '每页数量，默认10' },
      },
    },
  },
  {
    name: 'get_subscription_token',
    description: '获取已批准订购的访问令牌（Token），用于拉取受控制品。',
    inputSchema: {
      type: 'object',
      properties: { subscription_id: { type: 'number', description: '订购记录ID' } },
      required: ['subscription_id'],
    },
  },
  // ── 软件管理类 ──────────────────────────────────────────────────────────────
  {
    name: 'create_software_package',
    description: '创建新软件包（草稿状态）。packageKey 是全局唯一的英文标识符，创建后不可修改。',
    inputSchema: {
      type: 'object',
      properties: {
        package_name: { type: 'string', description: '软件包显示名称' },
        package_key: { type: 'string', description: '全局唯一英文标识符（字母/数字/连字符，创建后不可修改）' },
        software_type: { type: 'string', description: '软件类型', enum: ['DOCKER_IMAGE', 'HELM_CHART', 'MAVEN', 'NPM', 'PYPI', 'GENERIC'] },
        description: { type: 'string', description: '软件包描述' },
      },
      required: ['package_name', 'package_key', 'software_type'],
    },
  },
  {
    name: 'add_version',
    description: '为软件包添加新版本，指定存储后端。版本创建后状态为 DRAFT，需上传制品后才能发布。',
    inputSchema: {
      type: 'object',
      properties: {
        package_id: { type: 'number', description: '软件包ID' },
        version_no: { type: 'string', description: '版本号（如 1.0.0、v2.1.3）' },
        storage_backend_id: { type: 'number', description: '存储后端ID（用 list_storage_backends 获取）' },
        release_notes: { type: 'string', description: '版本发布说明（可选）' },
      },
      required: ['package_id', 'version_no', 'storage_backend_id'],
    },
  },
  {
    name: 'submit_for_review',
    description: '将软件包提交审核（从 DRAFT 状态提交为 PENDING 状态，等待管理员审核）。',
    inputSchema: {
      type: 'object',
      properties: { package_id: { type: 'number', description: '软件包ID' } },
      required: ['package_id'],
    },
  },
  {
    name: 'publish_version',
    description: '发布指定版本（将版本状态从 DRAFT 改为 PUBLISHED，使其在门户可见）。',
    inputSchema: {
      type: 'object',
      properties: {
        package_id: { type: 'number', description: '软件包ID' },
        version_id: { type: 'number', description: '版本ID' },
      },
      required: ['package_id', 'version_id'],
    },
  },
  {
    name: 'offline_version',
    description: '下线指定版本（版本状态改为 OFFLINE，门户不再展示该版本）。',
    inputSchema: {
      type: 'object',
      properties: {
        package_id: { type: 'number', description: '软件包ID' },
        version_id: { type: 'number', description: '版本ID' },
      },
      required: ['package_id', 'version_id'],
    },
  },
  // ── 查询类 ──────────────────────────────────────────────────────────────────
  {
    name: 'list_my_packages',
    description: '查询当前用户管理的软件包列表，可按状态和类型过滤。',
    inputSchema: {
      type: 'object',
      properties: {
        status: { type: 'string', description: '软件包状态过滤', enum: ['DRAFT', 'PENDING', 'PUBLISHED', 'OFFLINE'] },
        software_type: { type: 'string', description: '软件类型过滤', enum: ['DOCKER_IMAGE', 'HELM_CHART', 'MAVEN', 'NPM', 'PYPI', 'GENERIC'] },
        page: { type: 'number', description: '页码，默认1' },
        size: { type: 'number', description: '每页数量，默认10' },
      },
    },
  },
  {
    name: 'list_business_systems',
    description: '获取业务系统列表，用于订购申请时选择关联的业务系统。',
    inputSchema: {
      type: 'object',
      properties: { keyword: { type: 'string', description: '按系统名称搜索（可选）' } },
    },
  },
  {
    name: 'list_storage_backends',
    description: '获取可用的存储后端列表，用于新建软件版本时选择存储位置。',
    inputSchema: { type: 'object', properties: {} },
  },
  {
    name: 'check_compliance',
    description: '检查指定业务系统的软件使用合规情况：列出该系统订购并使用了哪些软件，标注是否已审批通过。',
    inputSchema: {
      type: 'object',
      properties: { system_id: { type: 'number', description: '业务系统ID' } },
      required: ['system_id'],
    },
  },
  // ── 审批类 ─────────────────────────────────────────────────────────────────
  {
    name: 'list_pending_approvals',
    description: '查询待审批事项列表，支持按类型过滤（订购申请 或 软件包审核）。【管理员功能】',
    inputSchema: {
      type: 'object',
      properties: {
        type: { type: 'string', description: '审批类型过滤', enum: ['SUBSCRIPTION', 'SOFTWARE_PACKAGE'] },
        page: { type: 'number', description: '页码，默认1' },
        size: { type: 'number', description: '每页数量，默认10' },
      },
    },
  },
  {
    name: 'approve_item',
    description: '批准一个审批项（订购申请或软件包）。【管理员功能】',
    inputSchema: {
      type: 'object',
      properties: {
        type: { type: 'string', description: '审批类型', enum: ['SUBSCRIPTION', 'SOFTWARE_PACKAGE'] },
        id: { type: 'number', description: '待审批项的ID' },
        comment: { type: 'string', description: '审批意见（可选）' },
      },
      required: ['type', 'id'],
    },
  },
  {
    name: 'reject_item',
    description: '拒绝一个审批项（订购申请或软件包），必须提供拒绝原因。【管理员功能】',
    inputSchema: {
      type: 'object',
      properties: {
        type: { type: 'string', description: '审批类型', enum: ['SUBSCRIPTION', 'SOFTWARE_PACKAGE'] },
        id: { type: 'number', description: '待审批项的ID' },
        reason: { type: 'string', description: '拒绝原因（必填）' },
      },
      required: ['type', 'id', 'reason'],
    },
  },
  {
    name: 'batch_approve',
    description: '批量批准多个同类型的审批项。【管理员功能】',
    inputSchema: {
      type: 'object',
      properties: {
        type: { type: 'string', description: '审批类型', enum: ['SUBSCRIPTION', 'SOFTWARE_PACKAGE'] },
        ids: { type: 'array', items: { type: 'number' }, description: '待批准的ID列表' },
        comment: { type: 'string', description: '批量审批意见（可选）' },
      },
      required: ['type', 'ids'],
    },
  },
  // ── 用户管理 ─────────────────────────────────────────────────────────────────
  {
    name: 'list_users',
    description: '查询系统用户列表，支持关键词搜索。【管理员功能】',
    inputSchema: {
      type: 'object',
      properties: {
        keyword: { type: 'string', description: '按用户名或姓名搜索（可选）' },
        page: { type: 'number', description: '页码，默认1' },
        size: { type: 'number', description: '每页数量，默认10' },
      },
    },
  },
  {
    name: 'create_user',
    description: '创建新用户并分配角色。【管理员功能】',
    inputSchema: {
      type: 'object',
      properties: {
        username: { type: 'string', description: '用户名（唯一）' },
        password: { type: 'string', description: '初始密码' },
        real_name: { type: 'string', description: '真实姓名' },
        role: { type: 'string', description: '角色', enum: ['ADMIN', 'SOFTWARE_ADMIN', 'DEVELOPER', 'GUEST'] },
      },
      required: ['username', 'password', 'role'],
    },
  },
  // ── 存储管理 ─────────────────────────────────────────────────────────────────
  {
    name: 'check_storage_health',
    description: '检查所有存储后端的健康状态（连通性、可用空间等）。【管理员功能】',
    inputSchema: { type: 'object', properties: {} },
  },
  {
    name: 'list_storage_backends_detail',
    description: '查询所有存储后端的完整配置详情（含访问凭据状态、仓库映射等）。【管理员功能】',
    inputSchema: { type: 'object', properties: {} },
  },
  // ── 软件管理（管理员视角）────────────────────────────────────────────────────
  {
    name: 'list_all_packages',
    description: '查询系统中所有软件包（含其他用户创建的），管理员专用视图。【管理员功能】',
    inputSchema: {
      type: 'object',
      properties: {
        status: { type: 'string', description: '软件包状态过滤', enum: ['DRAFT', 'PENDING', 'PUBLISHED', 'OFFLINE'] },
        software_type: { type: 'string', description: '软件类型过滤', enum: ['DOCKER_IMAGE', 'HELM_CHART', 'MAVEN', 'NPM', 'PYPI', 'GENERIC'] },
        keyword: { type: 'string', description: '关键词搜索' },
        page: { type: 'number', description: '页码，默认1' },
        size: { type: 'number', description: '每页数量，默认10' },
      },
    },
  },
  {
    name: 'offline_package',
    description: '强制下线软件包（管理员操作，需提供原因）。【管理员功能】',
    inputSchema: {
      type: 'object',
      properties: {
        package_id: { type: 'number', description: '软件包ID' },
        reason: { type: 'string', description: '下线原因' },
      },
      required: ['package_id', 'reason'],
    },
  },
] as const;

function ok(data: unknown) {
  return { content: [{ type: 'text' as const, text: JSON.stringify(data, null, 2) }] };
}
function err(message: string) {
  return { content: [{ type: 'text' as const, text: `错误：${message}` }], isError: true as const };
}

export function buildApiServer(api: AxiosInstance): Server {
  const server = new Server({ name: 'osrm-api', version: '1.0.0' }, { capabilities: { tools: {} } });

  server.setRequestHandler(ListToolsRequestSchema, async () => ({ tools }));

  server.setRequestHandler(CallToolRequestSchema, async (req) => {
    const { name, arguments: args } = req.params;
    const a = (args ?? {}) as Record<string, unknown>;
    try {
      switch (name) {
        // ── 订购类 ──────────────────────────────────────────────────────────
        case 'apply_subscription': {
          const res = await api.post('/subscriptions', {
            packageId: z.number().parse(a.package_id),
            versionId: z.number().parse(a.version_id),
            systemId: z.number().parse(a.system_id),
            useScene: z.string().parse(a.use_scene),
          });
          return ok(res.data ?? res);
        }
        case 'list_my_subscriptions': {
          const params: Record<string, unknown> = { page: a.page ?? 1, size: a.size ?? 10 };
          if (a.status) params.status = a.status;
          const res = await api.get('/subscriptions/my', { params });
          const data = res.data ?? res;
          return ok({
            total: data.totalElements ?? data.total ?? 0,
            items: (data.content ?? data.items ?? data).map((s: any) => ({
              id: s.id, packageName: s.packageName, versionNo: s.versionNo,
              systemName: s.systemName, status: s.status, useScene: s.useScene,
              appliedAt: s.createdAt, approvedAt: s.approvedAt,
            })),
          });
        }
        case 'get_subscription_token': {
          const res = await api.get(`/subscriptions/${z.number().parse(a.subscription_id)}/token`);
          return ok(res.data ?? res);
        }
        // ── 软件管理类 ────────────────────────────────────────────────────────
        case 'create_software_package': {
          const res = await api.post('/software-packages', {
            packageName: z.string().parse(a.package_name),
            packageKey: z.string().parse(a.package_key),
            softwareType: z.string().parse(a.software_type),
            description: a.description ?? '',
          });
          return ok(res.data ?? res);
        }
        case 'add_version': {
          const packageId = z.number().parse(a.package_id);
          const res = await api.post(`/software-packages/${packageId}/versions`, {
            versionNo: z.string().parse(a.version_no),
            storageBackendId: z.number().parse(a.storage_backend_id),
            releaseNotes: a.release_notes ?? '',
          });
          return ok(res.data ?? res);
        }
        case 'submit_for_review': {
          const res = await api.post(`/software-packages/${z.number().parse(a.package_id)}/submit`);
          return ok(res.data ?? res);
        }
        case 'publish_version': {
          const res = await api.post(
            `/software-packages/${z.number().parse(a.package_id)}/versions/${z.number().parse(a.version_id)}/publish`
          );
          return ok(res.data ?? res);
        }
        case 'offline_version': {
          const res = await api.post(
            `/software-packages/${z.number().parse(a.package_id)}/versions/${z.number().parse(a.version_id)}/offline`
          );
          return ok(res.data ?? res);
        }
        // ── 查询类 ────────────────────────────────────────────────────────────
        case 'list_my_packages': {
          const params: Record<string, unknown> = { page: a.page ?? 1, size: a.size ?? 10 };
          if (a.status) params.status = a.status;
          if (a.software_type) params.softwareType = a.software_type;
          const res = await api.get('/software-packages/my', { params });
          const data = res.data ?? res;
          return ok({
            total: data.totalElements ?? data.total ?? 0,
            items: (data.content ?? data.items ?? data).map((p: any) => ({
              id: p.id, packageName: p.packageName, packageKey: p.packageKey,
              type: p.softwareType, status: p.status,
              currentVersion: p.currentVersion, versionCount: p.versionCount, createdAt: p.createdAt,
            })),
          });
        }
        case 'list_business_systems': {
          const params: Record<string, unknown> = {};
          if (a.keyword) params.keyword = a.keyword;
          const res = await api.get('/business-systems', { params });
          const data = res.data ?? res;
          return ok(
            (Array.isArray(data) ? data : data.content ?? data.items ?? []).map((s: any) => ({
              id: s.id, systemName: s.systemName, systemCode: s.systemCode,
              description: s.description, ownerName: s.ownerName,
            }))
          );
        }
        case 'list_storage_backends': {
          const res = await api.get('/storage-backends');
          const data = res.data ?? res;
          return ok(
            (Array.isArray(data) ? data : data.content ?? []).map((b: any) => ({
              id: b.id, backendName: b.backendName, backendType: b.backendType,
              url: b.url, status: b.status,
            }))
          );
        }
        case 'check_compliance': {
          const res = await api.get(`/subscriptions/compliance/${z.number().parse(a.system_id)}`);
          return ok(res.data ?? res);
        }
        // ── 审批类 ────────────────────────────────────────────────────────────
        case 'list_pending_approvals': {
          const [subRes, pkgRes] = await Promise.all([
            (!a.type || a.type === 'SUBSCRIPTION')
              ? api.get('/subscriptions/pending', { params: { page: a.page ?? 1, size: a.size ?? 10 } })
              : Promise.resolve(null),
            (!a.type || a.type === 'SOFTWARE_PACKAGE')
              ? api.get('/software-packages/pending', { params: { page: a.page ?? 1, size: a.size ?? 10 } })
              : Promise.resolve(null),
          ]);
          const result: Record<string, unknown> = {};
          if (subRes) {
            const d = subRes.data ?? subRes;
            result.subscriptions = {
              total: d.totalElements ?? d.total ?? 0,
              items: (d.content ?? d.items ?? d).map((s: any) => ({
                id: s.id, type: 'SUBSCRIPTION', packageName: s.packageName,
                versionNo: s.versionNo, systemName: s.systemName,
                applicantName: s.applicantName, useScene: s.useScene, appliedAt: s.createdAt,
              })),
            };
          }
          if (pkgRes) {
            const d = pkgRes.data ?? pkgRes;
            result.packages = {
              total: d.totalElements ?? d.total ?? 0,
              items: (d.content ?? d.items ?? d).map((p: any) => ({
                id: p.id, type: 'SOFTWARE_PACKAGE', packageName: p.packageName,
                softwareType: p.softwareType, submitterName: p.submitterName, submittedAt: p.submittedAt,
              })),
            };
          }
          return ok(result);
        }
        case 'approve_item': {
          const type = z.enum(['SUBSCRIPTION', 'SOFTWARE_PACKAGE']).parse(a.type);
          const id = z.number().parse(a.id);
          const res = type === 'SUBSCRIPTION'
            ? await api.post(`/subscriptions/${id}/approve`, { comment: a.comment ?? '' })
            : await api.post(`/software-packages/${id}/approve`, { comment: a.comment ?? '' });
          return ok(res.data ?? res);
        }
        case 'reject_item': {
          const type = z.enum(['SUBSCRIPTION', 'SOFTWARE_PACKAGE']).parse(a.type);
          const id = z.number().parse(a.id);
          const reason = z.string().min(1).parse(a.reason);
          const res = type === 'SUBSCRIPTION'
            ? await api.post(`/subscriptions/${id}/reject`, { reason })
            : await api.post(`/software-packages/${id}/reject`, { reason });
          return ok(res.data ?? res);
        }
        case 'batch_approve': {
          const type = z.enum(['SUBSCRIPTION', 'SOFTWARE_PACKAGE']).parse(a.type);
          const ids = z.array(z.number()).parse(a.ids);
          const body = { ids, comment: a.comment ?? '' };
          const res = type === 'SUBSCRIPTION'
            ? await api.post('/subscriptions/batch-approve', body)
            : await api.post('/software-packages/batch-approve', body);
          return ok(res.data ?? res);
        }
        // ── 用户管理 ──────────────────────────────────────────────────────────
        case 'list_users': {
          const params: Record<string, unknown> = { page: a.page ?? 1, size: a.size ?? 10 };
          if (a.keyword) params.keyword = a.keyword;
          const res = await api.get('/users', { params });
          const data = res.data ?? res;
          return ok({
            total: data.totalElements ?? data.total ?? 0,
            items: (data.content ?? data.items ?? data).map((u: any) => ({
              id: u.id, username: u.username, realName: u.realName,
              role: u.role, roleName: u.roleName, createdAt: u.createdAt,
            })),
          });
        }
        case 'create_user': {
          const res = await api.post('/users', {
            username: z.string().parse(a.username),
            password: z.string().parse(a.password),
            realName: a.real_name ?? '',
            role: z.enum(['ADMIN', 'SOFTWARE_ADMIN', 'DEVELOPER', 'GUEST']).parse(a.role),
          });
          return ok(res.data ?? res);
        }
        // ── 存储管理 ──────────────────────────────────────────────────────────
        case 'check_storage_health': {
          const res = await api.get('/storage-backends/health');
          return ok(res.data ?? res);
        }
        case 'list_storage_backends_detail': {
          const res = await api.get('/storage-backends');
          const data = res.data ?? res;
          return ok(
            (Array.isArray(data) ? data : data.content ?? []).map((b: any) => ({
              id: b.id, backendName: b.backendName, backendType: b.backendType,
              url: b.url, status: b.status, configJson: b.configJson, createdAt: b.createdAt,
            }))
          );
        }
        // ── 软件管理（管理员视角）─────────────────────────────────────────────
        case 'list_all_packages': {
          const params: Record<string, unknown> = { page: a.page ?? 1, size: a.size ?? 10 };
          if (a.status) params.status = a.status;
          if (a.software_type) params.softwareType = a.software_type;
          if (a.keyword) params.keyword = a.keyword;
          const res = await api.get('/software-packages', { params });
          const data = res.data ?? res;
          return ok({
            total: data.totalElements ?? data.total ?? 0,
            items: (data.content ?? data.items ?? data).map((p: any) => ({
              id: p.id, packageName: p.packageName, packageKey: p.packageKey,
              type: p.softwareType, status: p.status,
              currentVersion: p.currentVersion, createdBy: p.createdBy, createdAt: p.createdAt,
            })),
          });
        }
        case 'offline_package': {
          const res = await api.post(
            `/software-packages/${z.number().parse(a.package_id)}/offline`,
            { reason: z.string().min(1).parse(a.reason) }
          );
          return ok(res.data ?? res);
        }
        default: return err(`未知工具: ${name}`);
      }
    } catch (e: unknown) {
      if (e instanceof OsrmApiError) return err(`API错误 [${e.code}]: ${e.message}`);
      return err(e instanceof Error ? e.message : String(e));
    }
  });

  return server;
}
