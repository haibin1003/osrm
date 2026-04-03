#!/usr/bin/env node
import { createServer, IncomingMessage, ServerResponse } from 'node:http';
import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { SSEServerTransport } from '@modelcontextprotocol/sdk/server/sse.js';
import { type Server } from '@modelcontextprotocol/sdk/server/index.js';
import { buildPortalServer } from 'osrm-portal/server';
import { buildApiServer } from 'osrm-api/server';
import { createClient, createClientForUser } from '@osrm-mcp/shared';

const BASE_URL = process.env.OSRM_BASE_URL ?? 'http://localhost:8080/api/v1';
const PORT = Number(process.env.PORT ?? 3000);

// Portal 使用公开客户端（无需认证）
const portalApi = createClient(BASE_URL, false);

// ── Auth helpers ──────────────────────────────────────────────────────────────

function parseBasicAuth(authHeader: string | undefined): { username: string; password: string } | null {
  if (!authHeader?.startsWith('Basic ')) return null;
  try {
    const decoded = Buffer.from(authHeader.slice(6), 'base64').toString('utf8');
    const colon = decoded.indexOf(':');
    if (colon < 1) return null;
    return { username: decoded.slice(0, colon), password: decoded.slice(colon + 1) };
  } catch {
    return null;
  }
}

function sendJson(res: ServerResponse, status: number, body: unknown): void {
  const payload = JSON.stringify(body);
  res.writeHead(status, { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) });
  res.end(payload);
}

// ── Session management ────────────────────────────────────────────────────────

// 存储活跃的 SSE transports（用于传统 SSE 协议）
const sseTransports: Map<string, SSEServerTransport> = new Map();

// ── MCP request handlers ──────────────────────────────────────────────────────

async function handleMcpStreamableHTTP(server: Server, req: IncomingMessage, res: ServerResponse): Promise<void> {
  const transport = new StreamableHTTPServerTransport({
    sessionIdGenerator: undefined, // 无状态，每个请求独立
  });
  await server.connect(transport);
  await transport.handleRequest(req, res);
}

async function handleMcpSSE(
  server: Server, 
  req: IncomingMessage, 
  res: ServerResponse,
  messageEndpoint: string
): Promise<string> {
  const transport = new SSEServerTransport(messageEndpoint, res);
  const sessionId = transport.sessionId;
  
  // 存储 transport
  sseTransports.set(sessionId, transport);
  
  // 清理 handlers
  transport.onclose = () => {
    console.error(`[osrm-gateway] SSE transport closed for session ${sessionId}`);
    sseTransports.delete(sessionId);
  };
  
  await server.connect(transport);
  console.error(`[osrm-gateway] SSE stream established: sessionId=${sessionId}`);
  
  return sessionId;
}

async function handleMcpSSEMessage(
  req: IncomingMessage, 
  res: ServerResponse,
  sessionId: string
): Promise<void> {
  const transport = sseTransports.get(sessionId);
  
  if (!transport) {
    sendJson(res, 404, { error: 'Session not found', sessionId });
    return;
  }
  
  // 读取请求体
  let body = '';
  for await (const chunk of req) {
    body += chunk;
  }
  
  try {
    const jsonBody = JSON.parse(body);
    await transport.handlePostMessage(req, res, jsonBody);
  } catch (error) {
    console.error('[osrm-gateway] Error handling SSE message:', error);
    sendJson(res, 400, { error: 'Invalid JSON body' });
  }
}

// ── HTTP server ───────────────────────────────────────────────────────────────

const httpServer = createServer(async (req: IncomingMessage, res: ServerResponse) => {
  const url = new URL(req.url ?? '/', `http://${req.headers.host}`);
  const pathname = url.pathname;

  // 设置 CORS 头
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, mcp-session-id');
  
  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  try {
    // 健康检查
    if (pathname === '/health' || pathname === '/health/') {
      sendJson(res, 200, { 
        status: 'ok', 
        version: '1.0.0', 
        services: ['portal', 'api'],
        protocols: ['streamable-http', 'sse']
      });
      return;
    }

    // ============================================================
    // STREAMABLE HTTP (协议版本 2025-03-26)
    // ============================================================

    // Portal —— 无需认证
    if (pathname === '/portal/mcp') {
      await handleMcpStreamableHTTP(buildPortalServer(portalApi), req, res);
      return;
    }

    // API —— HTTP Basic Auth
    if (pathname === '/api/mcp') {
      const creds = parseBasicAuth(req.headers['authorization']);
      if (!creds) {
        res.setHeader('WWW-Authenticate', 'Basic realm="OSRM"');
        sendJson(res, 401, { error: '需要认证。请使用 OSRM 用户名和密码通过 HTTP Basic Auth 访问。' });
        return;
      }
      const userApi = createClientForUser(BASE_URL, creds.username, creds.password);
      await handleMcpStreamableHTTP(buildApiServer(userApi), req, res);
      return;
    }

    // ============================================================
    // DEPRECATED HTTP+SSE (协议版本 2024-11-05)
    // 用于兼容 Claude Code 等旧客户端
    // ============================================================

    // Portal SSE —— 建立 SSE 流
    if (pathname === '/portal/sse' && req.method === 'GET') {
      res.setHeader('Content-Type', 'text/event-stream');
      res.setHeader('Cache-Control', 'no-cache');
      res.setHeader('Connection', 'keep-alive');
      await handleMcpSSE(buildPortalServer(portalApi), req, res, '/portal/messages');
      return;
    }

    // Portal SSE —— 接收消息
    if (pathname === '/portal/messages' && req.method === 'POST') {
      const sessionId = url.searchParams.get('sessionId');
      if (!sessionId) {
        sendJson(res, 400, { error: 'Missing sessionId parameter' });
        return;
      }
      await handleMcpSSEMessage(req, res, sessionId);
      return;
    }

    // API SSE —— 建立 SSE 流（需要认证）
    if (pathname === '/api/sse' && req.method === 'GET') {
      const creds = parseBasicAuth(req.headers['authorization']);
      if (!creds) {
        res.setHeader('WWW-Authenticate', 'Basic realm="OSRM"');
        sendJson(res, 401, { error: '需要认证。' });
        return;
      }
      res.setHeader('Content-Type', 'text/event-stream');
      res.setHeader('Cache-Control', 'no-cache');
      res.setHeader('Connection', 'keep-alive');
      const userApi = createClientForUser(BASE_URL, creds.username, creds.password);
      await handleMcpSSE(buildApiServer(userApi), req, res, '/api/messages');
      return;
    }

    // API SSE —— 接收消息
    if (pathname === '/api/messages' && req.method === 'POST') {
      const sessionId = url.searchParams.get('sessionId');
      if (!sessionId) {
        sendJson(res, 400, { error: 'Missing sessionId parameter' });
        return;
      }
      await handleMcpSSEMessage(req, res, sessionId);
      return;
    }

    // 404
    sendJson(res, 404, { 
      error: 'Not found', 
      availablePaths: [
        // Streamable HTTP
        '/health', 
        '/portal/mcp', 
        '/api/mcp',
        // Legacy SSE
        '/portal/sse',
        '/portal/messages',
        '/api/sse',
        '/api/messages'
      ] 
    });
  } catch (e: unknown) {
    console.error('[osrm-gateway] Unhandled error:', e);
    if (!res.headersSent) {
      sendJson(res, 500, { error: 'Internal server error' });
    }
  }
});

httpServer.listen(PORT, '0.0.0.0', () => {
  console.error(`[osrm-gateway] MCP gateway listening on :${PORT}`);
  console.error(`[osrm-gateway] =============================================`);
  console.error(`[osrm-gateway] Streamable HTTP (Protocol 2025-03-26):`);
  console.error(`[osrm-gateway]   Portal → http://localhost:${PORT}/portal/mcp  (公开)`);
  console.error(`[osrm-gateway]   API    → http://localhost:${PORT}/api/mcp     (Basic Auth)`);
  console.error(`[osrm-gateway] ---------------------------------------------`);
  console.error(`[osrm-gateway] Legacy SSE (Protocol 2024-11-05):`);
  console.error(`[osrm-gateway]   Portal SSE → http://localhost:${PORT}/portal/sse`);
  console.error(`[osrm-gateway]   Portal Msg → http://localhost:${PORT}/portal/messages?sessionId=xxx`);
  console.error(`[osrm-gateway]   API SSE    → http://localhost:${PORT}/api/sse    (Basic Auth)`);
  console.error(`[osrm-gateway]   API Msg    → http://localhost:${PORT}/api/messages?sessionId=xxx`);
  console.error(`[osrm-gateway] =============================================`);
});

// 优雅关闭
process.on('SIGINT', async () => {
  console.error('[osrm-gateway] Shutting down...');
  
  // 关闭所有 SSE transports
  for (const [sessionId, transport] of sseTransports) {
    try {
      console.error(`[osrm-gateway] Closing SSE transport: ${sessionId}`);
      await transport.close();
    } catch (e) {
      console.error(`[osrm-gateway] Error closing transport ${sessionId}:`, e);
    }
  }
  sseTransports.clear();
  
  httpServer.close(() => {
    console.error('[osrm-gateway] Server closed');
    process.exit(0);
  });
});
