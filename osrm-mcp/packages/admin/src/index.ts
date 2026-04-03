#!/usr/bin/env node
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { z } from 'zod';
import { createClient, OsrmApiError, configureAuth } from '@osrm-mcp/shared';

// ── Init API client (admin auth required) ────────────────────────────────────
const BASE_URL = process.env.OSRM_BASE_URL ?? 'http://localhost:8080/api/v1';
configureAuth(BASE_URL);
const api = createClient(BASE_URL, true);

// ── Tool definitions ──────────────────────────────────────────────────────────
const tools = [
  // ── 审批类 ──────────────────────────────────────────────────────────────────
  {
    name: 'list_pending_approvals',
    description:
      '查询待审批事项列表，支持按类型过滤（订购申请 或 软件包审核）。',
    inputSchema: {
      type: 'object',
      properties: {
        type: {
          type: 'string',
          description: '审批类型过滤',
          enum: ['SUBSCRIPTION', 'SOFTWARE_PACKAGE'],
        },
        page: { type: 'number', description: '页码，默认1' },
        size: { type: 'number', description: '每页数量，默认10' },
      },
    },
  },
  {
    name: 'approve_item',
    description: '批准一个审批项（订购申请或软件包）。',
    inputSchema: {
      type: 'object',
      properties: {
        type: {
          type: 'string',
          description: '审批类型',
          enum: ['SUBSCRIPTION', 'SOFTWARE_PACKAGE'],
        },
        id: { type: 'number', description: '待审批项的ID' },
        comment: { type: 'string', description: '审批意见（可选）' },
      },
      required: ['type', 'id'],
    },
  },
  {
    name: 'reject_item',
    description: '拒绝一个审批项（订购申请或软件包），必须提供拒绝原因。',
    inputSchema: {
      type: 'object',
      properties: {
        type: {
          type: 'string',
          description: '审批类型',
          enum: ['SUBSCRIPTION', 'SOFTWARE_PACKAGE'],
        },
        id: { type: 'number', description: '待审批项的ID' },
        reason: { type: 'string', description: '拒绝原因（必填）' },
      },
      required: ['type', 'id', 'reason'],
    },
  },
  {
    name: 'batch_approve',
    description: '批量批准多个同类型的审批项。',
    inputSchema: {
      type: 'object',
      properties: {
        type: {
          type: 'string',
          description: '审批类型',
          enum: ['SUBSCRIPTION', 'SOFTWARE_PACKAGE'],
        },
        ids: {
          type: 'array',
          items: { type: 'number' },
          description: '待批准的ID列表',
        },
        comment: { type: 'string', description: '批量审批意见（可选）' },
      },
      required: ['type', 'ids'],
    },
  },
  // ── 用户管理 ─────────────────────────────────────────────────────────────────
  {
    name: 'list_users',
    description: '查询系统用户列表，支持关键词搜索。',
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
    description: '创建新用户并分配角色。',
    inputSchema: {
      type: 'object',
      properties: {
        username: { type: 'string', description: '用户名（唯一）' },
        password: { type: 'string', description: '初始密码' },
        real_name: { type: 'string', description: '真实姓名' },
        role: {
          type: 'string',
          description: '角色',
          enum: ['ADMIN', 'SOFTWARE_ADMIN', 'DEVELOPER', 'GUEST'],
        },
      },
      required: ['username', 'password', 'role'],
    },
  },
  // ── 存储管理 ─────────────────────────────────────────────────────────────────
  {
    name: 'check_storage_health',
    description: '检查所有存储后端的健康状态（连通性、可用空间等）。',
    inputSchema: { type: 'object', properties: {} },
  },
  {
    name: 'list_storage_backends_detail',
    description: '查询所有存储后端的完整配置详情（含访问凭据状态、仓库映射等）。',
    inputSchema: { type: 'object', properties: {} },
  },
  // ── 软件管理（管理员视角）──────────────────────────────────────────────────
  {
    name: 'list_all_packages',
    description: '查询系统中所有软件包（含其他用户创建的），管理员专用视图。',
    inputSchema: {
      type: 'object',
      properties: {
        status: {
          type: 'string',
          description: '软件包状态过滤',
          enum: ['DRAFT', 'PENDING', 'PUBLISHED', 'OFFLINE'],
        },
        software_type: {
          type: 'string',
          description: '软件类型过滤',
          enum: ['DOCKER_IMAGE', 'HELM_CHART', 'MAVEN', 'NPM', 'PYPI', 'GENERIC'],
        },
        keyword: { type: 'string', description: '关键词搜索' },
        page: { type: 'number', description: '页码，默认1' },
        size: { type: 'number', description: '每页数量，默认10' },
      },
    },
  },
  {
    name: 'offline_package',
    description: '强制下线软件包（管理员操作，需提供原因）。',
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

// ── Helpers ───────────────────────────────────────────────────────────────────
function ok(data: unknown): { content: Array<{ type: 'text'; text: string }> } {
  return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] };
}

function err(message: string): { content: Array<{ type: 'text'; text: string }>; isError: true } {
  return { content: [{ type: 'text', text: `错误：${message}` }], isError: true };
}

// ── MCP Server setup ──────────────────────────────────────────────────────────
const server = new Server(
  { name: 'osrm-admin', version: '1.0.0' },
  { capabilities: { tools: {} } }
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({ tools }));

server.setRequestHandler(CallToolRequestSchema, async (req) => {
  const { name, arguments: args } = req.params;
  const a = (args ?? {}) as Record<string, unknown>;

  try {
    switch (name) {
      // ── list_pending_approvals ────────────────────────────────────────────
      case 'list_pending_approvals': {
        const params: Record<string, unknown> = {
          page: a.page ?? 1,
          size: a.size ?? 10,
          status: 'PENDING',
        };
        if (a.type) params.approvalType = a.type;

        // Fetch subscription approvals and package approvals in parallel
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
              id: s.id,
              type: 'SUBSCRIPTION',
              packageName: s.packageName,
              versionNo: s.versionNo,
              systemName: s.systemName,
              applicantName: s.applicantName,
              useScene: s.useScene,
              appliedAt: s.createdAt,
            })),
          };
        }

        if (pkgRes) {
          const d = pkgRes.data ?? pkgRes;
          result.packages = {
            total: d.totalElements ?? d.total ?? 0,
            items: (d.content ?? d.items ?? d).map((p: any) => ({
              id: p.id,
              type: 'SOFTWARE_PACKAGE',
              packageName: p.packageName,
              softwareType: p.softwareType,
              submitterName: p.submitterName,
              submittedAt: p.submittedAt,
            })),
          };
        }

        return ok(result);
      }

      // ── approve_item ──────────────────────────────────────────────────────
      case 'approve_item': {
        const type = z.enum(['SUBSCRIPTION', 'SOFTWARE_PACKAGE']).parse(a.type);
        const id = z.number().parse(a.id);
        const body = { comment: a.comment ?? '' };

        let res;
        if (type === 'SUBSCRIPTION') {
          res = await api.post(`/subscriptions/${id}/approve`, body);
        } else {
          res = await api.post(`/software-packages/${id}/approve`, body);
        }
        return ok(res.data ?? res);
      }

      // ── reject_item ───────────────────────────────────────────────────────
      case 'reject_item': {
        const type = z.enum(['SUBSCRIPTION', 'SOFTWARE_PACKAGE']).parse(a.type);
        const id = z.number().parse(a.id);
        const reason = z.string().min(1).parse(a.reason);
        const body = { reason };

        let res;
        if (type === 'SUBSCRIPTION') {
          res = await api.post(`/subscriptions/${id}/reject`, body);
        } else {
          res = await api.post(`/software-packages/${id}/reject`, body);
        }
        return ok(res.data ?? res);
      }

      // ── batch_approve ─────────────────────────────────────────────────────
      case 'batch_approve': {
        const type = z.enum(['SUBSCRIPTION', 'SOFTWARE_PACKAGE']).parse(a.type);
        const ids = z.array(z.number()).parse(a.ids);
        const body = { ids, comment: a.comment ?? '' };

        let res;
        if (type === 'SUBSCRIPTION') {
          res = await api.post('/subscriptions/batch-approve', body);
        } else {
          res = await api.post('/software-packages/batch-approve', body);
        }
        return ok(res.data ?? res);
      }

      // ── list_users ────────────────────────────────────────────────────────
      case 'list_users': {
        const params: Record<string, unknown> = {
          page: a.page ?? 1,
          size: a.size ?? 10,
        };
        if (a.keyword) params.keyword = a.keyword;
        const res = await api.get('/users', { params });
        const data = res.data ?? res;
        return ok({
          total: data.totalElements ?? data.total ?? 0,
          items: (data.content ?? data.items ?? data).map((u: any) => ({
            id: u.id,
            username: u.username,
            realName: u.realName,
            role: u.role,
            roleName: u.roleName,
            createdAt: u.createdAt,
          })),
        });
      }

      // ── create_user ───────────────────────────────────────────────────────
      case 'create_user': {
        const body = {
          username: z.string().parse(a.username),
          password: z.string().parse(a.password),
          realName: a.real_name ?? '',
          role: z.enum(['ADMIN', 'SOFTWARE_ADMIN', 'DEVELOPER', 'GUEST']).parse(a.role),
        };
        const res = await api.post('/users', body);
        return ok(res.data ?? res);
      }

      // ── check_storage_health ──────────────────────────────────────────────
      case 'check_storage_health': {
        const res = await api.get('/storage-backends/health');
        return ok(res.data ?? res);
      }

      // ── list_storage_backends_detail ──────────────────────────────────────
      case 'list_storage_backends_detail': {
        const res = await api.get('/storage-backends');
        const data = res.data ?? res;
        return ok(
          (Array.isArray(data) ? data : data.content ?? []).map((b: any) => ({
            id: b.id,
            backendName: b.backendName,
            backendType: b.backendType,
            url: b.url,
            status: b.status,
            configJson: b.configJson,
            createdAt: b.createdAt,
          }))
        );
      }

      // ── list_all_packages ─────────────────────────────────────────────────
      case 'list_all_packages': {
        const params: Record<string, unknown> = {
          page: a.page ?? 1,
          size: a.size ?? 10,
        };
        if (a.status) params.status = a.status;
        if (a.software_type) params.softwareType = a.software_type;
        if (a.keyword) params.keyword = a.keyword;
        const res = await api.get('/software-packages', { params });
        const data = res.data ?? res;
        return ok({
          total: data.totalElements ?? data.total ?? 0,
          items: (data.content ?? data.items ?? data).map((p: any) => ({
            id: p.id,
            packageName: p.packageName,
            packageKey: p.packageKey,
            type: p.softwareType,
            status: p.status,
            currentVersion: p.currentVersion,
            createdBy: p.createdBy,
            createdAt: p.createdAt,
          })),
        });
      }

      // ── offline_package ───────────────────────────────────────────────────
      case 'offline_package': {
        const packageId = z.number().parse(a.package_id);
        const reason = z.string().min(1).parse(a.reason);
        const res = await api.post(`/software-packages/${packageId}/offline`, { reason });
        return ok(res.data ?? res);
      }

      default:
        return err(`未知工具: ${name}`);
    }
  } catch (e: unknown) {
    if (e instanceof OsrmApiError) {
      return err(`API错误 [${e.code}]: ${e.message}`);
    }
    return err(e instanceof Error ? e.message : String(e));
  }
});

// ── Start server ──────────────────────────────────────────────────────────────
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('[osrm-admin] MCP server started');
}

main().catch((e) => {
  console.error('[osrm-admin] Fatal error:', e);
  process.exit(1);
});
