#!/usr/bin/env node
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { buildApiServer } from './server.js';
import { createClientForUser } from '@osrm-mcp/shared';

const BASE_URL = process.env.OSRM_BASE_URL ?? 'http://localhost:8080/api/v1';
const USERNAME = process.env.OSRM_USERNAME;
const PASSWORD = process.env.OSRM_PASSWORD;

if (!USERNAME || !PASSWORD) {
  console.error('[osrm-api] Error: OSRM_USERNAME and OSRM_PASSWORD environment variables are required');
  console.error('[osrm-api] Example: OSRM_USERNAME=admin OSRM_PASSWORD=admin123 node index.js');
  process.exit(1);
}

// Create API client with user credentials
const api = createClientForUser(BASE_URL, USERNAME, PASSWORD);

// Build and start server
const server: Server = buildApiServer(api);

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('[osrm-api] MCP server started');
  console.error(`[osrm-api] Connected to: ${BASE_URL}`);
  console.error(`[osrm-api] User: ${USERNAME}`);
}

main().catch((e) => {
  console.error('[osrm-api] Fatal error:', e);
  process.exit(1);
});
