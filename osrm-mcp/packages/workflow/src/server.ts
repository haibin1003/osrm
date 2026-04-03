import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { z } from 'zod';
import { type AxiosInstance } from 'axios';
import { OsrmApiError } from '@osrm-mcp/shared';

const tools = [
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
] as const;

function ok(data: unknown) {
  return { content: [{ type: 'text' as const, text: JSON.stringify(data, null, 2) }] };
}
function err(message: string) {
  return { content: [{ type: 'text' as const, text: `错误：${message}` }], isError: true as const };
}

export function buildWorkflowServer(api: AxiosInstance): Server {
  const server = new Server({ name: 'osrm-workflow', version: '1.0.0' }, { capabilities: { tools: {} } });

  server.setRequestHandler(ListToolsRequestSchema, async () => ({ tools }));

  server.setRequestHandler(CallToolRequestSchema, async (req) => {
    const { name, arguments: args } = req.params;
    const a = (args ?? {}) as Record<string, unknown>;
    try {
      switch (name) {
        case 'apply_subscription': {
          const body = {
            packageId: z.number().parse(a.package_id),
            versionId: z.number().parse(a.version_id),
            systemId: z.number().parse(a.system_id),
            useScene: z.string().parse(a.use_scene),
          };
          const res = await api.post('/subscriptions', body);
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
          const id = z.number().parse(a.subscription_id);
          const res = await api.get(`/subscriptions/${id}/token`);
          return ok(res.data ?? res);
        }
        case 'create_software_package': {
          const body = {
            packageName: z.string().parse(a.package_name),
            packageKey: z.string().parse(a.package_key),
            softwareType: z.string().parse(a.software_type),
            description: a.description ?? '',
          };
          const res = await api.post('/software-packages', body);
          return ok(res.data ?? res);
        }
        case 'add_version': {
          const packageId = z.number().parse(a.package_id);
          const body = {
            versionNo: z.string().parse(a.version_no),
            storageBackendId: z.number().parse(a.storage_backend_id),
            releaseNotes: a.release_notes ?? '',
          };
          const res = await api.post(`/software-packages/${packageId}/versions`, body);
          return ok(res.data ?? res);
        }
        case 'submit_for_review': {
          const packageId = z.number().parse(a.package_id);
          const res = await api.post(`/software-packages/${packageId}/submit`);
          return ok(res.data ?? res);
        }
        case 'publish_version': {
          const packageId = z.number().parse(a.package_id);
          const versionId = z.number().parse(a.version_id);
          const res = await api.post(`/software-packages/${packageId}/versions/${versionId}/publish`);
          return ok(res.data ?? res);
        }
        case 'offline_version': {
          const packageId = z.number().parse(a.package_id);
          const versionId = z.number().parse(a.version_id);
          const res = await api.post(`/software-packages/${packageId}/versions/${versionId}/offline`);
          return ok(res.data ?? res);
        }
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
          const systemId = z.number().parse(a.system_id);
          const res = await api.get(`/subscriptions/compliance/${systemId}`);
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
