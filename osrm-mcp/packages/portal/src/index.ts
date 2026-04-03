#!/usr/bin/env node
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { z } from 'zod';
import { createClient, OsrmApiError } from '@osrm-mcp/shared';

// ── Init API client (no auth required for portal) ────────────────────────────
const BASE_URL = process.env.OSRM_BASE_URL ?? 'http://localhost:8080/api/v1';
const api = createClient(BASE_URL, false);

// ── Tool definitions ──────────────────────────────────────────────────────────
const tools = [
  {
    name: 'search_software',
    description:
      '搜索已发布的开源软件包。支持关键词搜索和类型过滤。返回软件包列表，含名称、类型、当前版本、描述、下载次数等信息。',
    inputSchema: {
      type: 'object',
      properties: {
        keyword: { type: 'string', description: '搜索关键词（包名或描述）' },
        type: {
          type: 'string',
          description: '软件类型过滤',
          enum: ['DOCKER_IMAGE', 'HELM_CHART', 'MAVEN', 'NPM', 'PYPI', 'GENERIC'],
        },
        page: { type: 'number', description: '页码，从1开始，默认1' },
        size: { type: 'number', description: '每页数量，默认12' },
      },
    },
  },
  {
    name: 'get_software_detail',
    description:
      '获取指定软件包的详细信息，包含所有版本列表、制品路径、状态等。',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'number', description: '软件包ID' },
      },
      required: ['id'],
    },
  },
  {
    name: 'get_portal_stats',
    description:
      '获取软件门户的统计概览数据：已发布总数、各类型（Docker/Helm/Maven/NPM/PyPI/通用）数量。',
    inputSchema: { type: 'object', properties: {} },
  },
  {
    name: 'get_popular_software',
    description: '获取热门软件包列表（按下载量排序）。',
    inputSchema: {
      type: 'object',
      properties: {
        limit: { type: 'number', description: '返回数量，默认10，最大50' },
      },
    },
  },
  {
    name: 'list_software_types',
    description:
      '获取系统支持的软件类型枚举列表，含每种类型对应的存储后端信息。',
    inputSchema: { type: 'object', properties: {} },
  },
  {
    name: 'get_download_command',
    description:
      '根据软件包类型和版本，返回对应的下载/使用命令（docker pull / helm / maven / npm install / pip install 等）。',
    inputSchema: {
      type: 'object',
      properties: {
        package_id: { type: 'number', description: '软件包ID' },
        version_id: {
          type: 'number',
          description: '版本ID（可选，不填则返回最新版本命令）',
        },
      },
      required: ['package_id'],
    },
  },
] as const;

// ── Helper: format result as readable text ────────────────────────────────────
function ok(data: unknown): { content: Array<{ type: 'text'; text: string }> } {
  return {
    content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
  };
}

function err(message: string): { content: Array<{ type: 'text'; text: string }>; isError: true } {
  return {
    content: [{ type: 'text', text: `错误：${message}` }],
    isError: true,
  };
}

// ── Download command builder ──────────────────────────────────────────────────
function buildDownloadCommand(
  softwareType: string,
  packageName: string,
  versionNo: string,
  storagePath?: string | null,
  artifactUrl?: string | null
): string {
  const ver = versionNo;
  switch (softwareType) {
    case 'DOCKER_IMAGE':
      return storagePath
        ? `docker pull ${storagePath}`
        : `docker pull <registry>/${packageName}:${ver}`;
    case 'HELM_CHART':
      return `helm pull <repo>/${packageName} --version ${ver}`;
    case 'MAVEN':
      return `<!-- Maven pom.xml -->\n<dependency>\n  <groupId>com.example</groupId>\n  <artifactId>${packageName}</artifactId>\n  <version>${ver}</version>\n</dependency>`;
    case 'NPM':
      return `npm install ${packageName}@${ver}`;
    case 'PYPI':
      return `pip install ${packageName}==${ver}`;
    default:
      return artifactUrl
        ? `curl -O "${artifactUrl}"`
        : `# 下载路径：${storagePath ?? '待上传'}`;
  }
}

// ── MCP Server setup ──────────────────────────────────────────────────────────
const server = new Server(
  { name: 'osrm-portal', version: '1.0.0' },
  {
    capabilities: {
      tools: {},
      resources: {},
    },
  }
);

// List tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({ tools }));

// List resources
server.setRequestHandler(ListResourcesRequestSchema, async () => ({
  resources: [
    {
      uri: 'osrm://portal/stats',
      name: '门户统计数据',
      description: '软件门户实时统计概览',
      mimeType: 'application/json',
    },
  ],
}));

// Read resource
server.setRequestHandler(ReadResourceRequestSchema, async (req) => {
  if (req.params.uri === 'osrm://portal/stats') {
    try {
      const stats = await api.get('/portal/stats');
      return {
        contents: [
          {
            uri: 'osrm://portal/stats',
            mimeType: 'application/json',
            text: JSON.stringify(stats.data ?? stats, null, 2),
          },
        ],
      };
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      return {
        contents: [{ uri: 'osrm://portal/stats', mimeType: 'text/plain', text: `Error: ${msg}` }],
      };
    }
  }
  throw new Error(`Unknown resource: ${req.params.uri}`);
});

// Call tool
server.setRequestHandler(CallToolRequestSchema, async (req) => {
  const { name, arguments: args } = req.params;
  const a = (args ?? {}) as Record<string, unknown>;

  try {
    switch (name) {
      // ── search_software ────────────────────────────────────────────────────
      case 'search_software': {
        const params: Record<string, unknown> = {
          page: a.page ?? 1,
          size: a.size ?? 12,
        };
        if (a.keyword) params.keyword = a.keyword;
        if (a.type) params.type = a.type;
        const res = await api.get('/portal/software', { params });
        const data = res.data ?? res;
        return ok({
          total: data.totalElements ?? data.total ?? 0,
          page: data.number ?? data.page ?? 1,
          items: (data.content ?? data.items ?? data).map((p: any) => ({
            id: p.id,
            packageName: p.packageName,
            packageKey: p.packageKey,
            type: p.softwareType,
            typeName: p.softwareTypeName,
            currentVersion: p.currentVersion,
            versionCount: p.versionCount,
            description: p.description,
            downloadCount: p.downloadCount,
            status: p.status,
          })),
        });
      }

      // ── get_software_detail ───────────────────────────────────────────────
      case 'get_software_detail': {
        const id = z.number().parse(a.id);
        const [detailRes, versionsRes] = await Promise.all([
          api.get(`/portal/software/${id}`),
          api.get(`/software-packages/${id}/versions`),
        ]);
        const pkg = detailRes.data ?? detailRes;
        const versions = versionsRes.data ?? versionsRes;
        return ok({
          id: pkg.id,
          packageName: pkg.packageName,
          packageKey: pkg.packageKey,
          type: pkg.softwareType,
          typeName: pkg.softwareTypeName,
          status: pkg.status,
          description: pkg.description,
          currentVersion: pkg.currentVersion,
          downloadCount: pkg.downloadCount,
          subscriptionCount: pkg.subscriptionCount,
          publishedAt: pkg.publishedAt,
          versions: Array.isArray(versions)
            ? versions.map((v: any) => ({
                id: v.id,
                versionNo: v.versionNo,
                status: v.status,
                isLatest: v.isLatest,
                fileSize: v.fileSize,
                storagePath: v.storagePath,
                releaseNotes: v.releaseNotes,
                publishedAt: v.publishedAt,
              }))
            : [],
        });
      }

      // ── get_portal_stats ──────────────────────────────────────────────────
      case 'get_portal_stats': {
        const res = await api.get('/portal/stats');
        return ok(res.data ?? res);
      }

      // ── get_popular_software ──────────────────────────────────────────────
      case 'get_popular_software': {
        const limit = z.number().min(1).max(50).default(10).parse(a.limit ?? 10);
        const res = await api.get('/portal/popular', { params: { limit } });
        const list = res.data ?? res;
        return ok(
          (Array.isArray(list) ? list : list.content ?? []).map((p: any) => ({
            id: p.id,
            packageName: p.packageName,
            type: p.softwareTypeName,
            currentVersion: p.currentVersion,
            downloadCount: p.downloadCount,
            description: p.description,
          }))
        );
      }

      // ── list_software_types ───────────────────────────────────────────────
      case 'list_software_types': {
        return ok([
          { type: 'DOCKER_IMAGE', typeName: 'Docker 镜像', description: '容器镜像，存储于 Harbor' },
          { type: 'HELM_CHART',   typeName: 'Helm Chart',  description: 'Kubernetes 应用包，存储于 Harbor' },
          { type: 'MAVEN',        typeName: 'Maven',        description: 'Java/JVM 制品，存储于 Nexus' },
          { type: 'NPM',          typeName: 'NPM',          description: 'Node.js 包，存储于 Nexus' },
          { type: 'PYPI',         typeName: 'PyPI',         description: 'Python 包，存储于 Nexus' },
          { type: 'GENERIC',      typeName: '通用制品',      description: '二进制文件等，存储于 NAS/本地' },
        ]);
      }

      // ── get_download_command ──────────────────────────────────────────────
      case 'get_download_command': {
        const packageId = z.number().parse(a.package_id);
        const versionId = a.version_id ? z.number().parse(a.version_id) : null;

        const [pkgRes, versionsRes] = await Promise.all([
          api.get(`/portal/software/${packageId}`),
          api.get(`/software-packages/${packageId}/versions`),
        ]);
        const pkg = pkgRes.data ?? pkgRes;
        const versions: any[] = Array.isArray(versionsRes.data ?? versionsRes)
          ? (versionsRes.data ?? versionsRes)
          : [];

        let version = versionId
          ? versions.find((v) => v.id === versionId)
          : versions.find((v) => v.isLatest) ?? versions[0];

        if (!version) {
          return err('未找到可用版本，软件包可能还未上传制品');
        }

        const command = buildDownloadCommand(
          pkg.softwareType,
          pkg.packageName,
          version.versionNo,
          version.storagePath,
          version.artifactUrl
        );

        return ok({
          packageName: pkg.packageName,
          type: pkg.softwareType,
          versionNo: version.versionNo,
          command,
          fileSize: version.fileSize,
          checksum: version.checksum,
        });
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
  console.error('[osrm-portal] MCP server started');
}

main().catch((e) => {
  console.error('[osrm-portal] Fatal error:', e);
  process.exit(1);
});
