import {execFileSync} from 'node:child_process';
import http from 'node:http';
import path from 'node:path';
import process from 'node:process';

export function createContext() {
  const root = path.resolve(import.meta.dirname, '../..');
  const prefix = process.env.ACCEPT_PREFIX || `ACCEPT_${Date.now()}`;
  const ctx = {
    root,
    adminBase: process.env.ADMIN_BASE_URL || 'http://localhost:8090/admin-api',
    appBase: process.env.APP_BASE_URL || 'http://localhost:8090/app-api',
    dbName: process.env.DB_NAME || 'lingnow_erp',
    dbUser: process.env.DB_USER || 'root',
    dbPassword: process.env.MYSQL_PWD || '',
    adminUsername: process.env.ADMIN_USERNAME || 'admin',
    adminPassword: process.env.ADMIN_PASSWORD || '123456',
    prefix,
    token: null,
    appToken: null,
    failures: [],
    flowResults: [],
    created: {
      bills: [],
      financeBills: [],
      stockChecks: [],
      products: [],
      masters: [],
      jobs: [],
      configRows: [],
      users: [],
      roles: [],
      appUsers: []
    },
    seed: {}
  };

  ctx.escapeSql = escapeSql;
  ctx.mysql = (query) => mysql(ctx, query);
  ctx.rows = (query) => rows(ctx, query);
  ctx.scalar = (query) => scalar(ctx, query);
  ctx.request = (base, requestPath, options = {}) => request(base, requestPath, options);
  ctx.adminRequest = (method, requestPath, body, token = ctx.token) => jsonRequest(ctx.adminBase, tokenHeaders(token, 'admin'), method, requestPath, body);
  ctx.appRequest = (method, requestPath, body, token = ctx.appToken) => jsonRequest(ctx.appBase, tokenHeaders(token, 'app'), method, requestPath, body);
  ctx.ok = (condition, message, detail = {}) => assertOk(ctx, condition, message, detail);
  ctx.expectOk = (method, requestPath, body, label, token = ctx.token) => expectOk(ctx, method, requestPath, body, label, token);
  ctx.expectAppOk = (method, requestPath, body, label, token = ctx.appToken) => expectAppOk(ctx, method, requestPath, body, label, token);
  ctx.expectBusinessFail = (method, requestPath, body, label, token = ctx.token) => expectBusinessFail(ctx, method, requestPath, body, label, token);
  ctx.section = section;
  ctx.today = today;
  ctx.latestId = (table, column, value, idColumn = 'id') => latestId(ctx, table, column, value, idColumn);
  ctx.billId = (billNo) => latestId(ctx, 'erp_bill', 'bill_no', billNo);
  ctx.financeBillId = (billNo) => latestId(ctx, 'erp_finance_bill', 'bill_no', billNo);
  ctx.stockCheckId = (checkNo) => latestId(ctx, 'erp_stock_check', 'check_no', checkNo);
  ctx.masterId = (table, code) => latestId(ctx, table, 'code', code);
  ctx.runFlow = (name, fn) => runFlow(ctx, name, fn);
  return ctx;
}

export function section(name) {
  console.log(`\n## ${name}`);
}

export function today() {
  return new Date().toISOString().slice(0, 10);
}

export function escapeSql(value) {
  return String(value ?? '').replaceAll('\\', '\\\\').replaceAll("'", "''");
}

function mysql(ctx, query) {
  if (!ctx.dbPassword) {
    throw new Error('MYSQL_PWD is required for release acceptance database checks');
  }
  return execFileSync('mysql', ['-u' + ctx.dbUser, ctx.dbName, '-N', '-B', '-e', query], {
    encoding: 'utf8',
    env: {...process.env, MYSQL_PWD: ctx.dbPassword}
  });
}

function rows(ctx, query) {
  return mysql(ctx, query)
    .trim()
    .split('\n')
    .filter(Boolean)
    .map((line) => line.split('\t').map((value) => (value === 'NULL' ? null : value)));
}

function scalar(ctx, query) {
  const output = mysql(ctx, query).trim();
  if (!output) return null;
  return output.split(/\t|\n/)[0];
}

export function request(base, requestPath, options = {}) {
  const baseUrl = new URL(base);
  const basePath = baseUrl.pathname.endsWith('/') ? baseUrl.pathname.slice(0, -1) : baseUrl.pathname;
  const normalizedPath = requestPath.startsWith('/') ? requestPath : `/${requestPath}`;
  const url = new URL(`${basePath}${normalizedPath}`, baseUrl.origin);
  return new Promise((resolve) => {
    const req = http.request({
      hostname: url.hostname,
      port: url.port || 80,
      path: url.pathname + url.search,
      method: options.method || 'GET',
      headers: options.headers || {}
    }, (res) => {
      const chunks = [];
      res.on('data', (chunk) => chunks.push(Buffer.from(chunk)));
      res.on('end', () => {
        const body = Buffer.concat(chunks);
        const data = body.toString('utf8');
        let json = null;
        try {
          json = JSON.parse(data);
        } catch {
          // CSV/export/print endpoints are intentionally not JSON.
        }
        resolve({status: res.statusCode, headers: res.headers, body, data, json});
      });
    });
    req.on('error', (error) => resolve({status: 0, error: error.message, data: '', body: Buffer.alloc(0)}));
    if (options.body != null) {
      req.write(options.body);
    }
    req.end();
  });
}

function tokenHeaders(token, type) {
  const headers = {'Content-Type': 'application/json'};
  if (!token) return headers;
  if (type === 'app') {
    headers['token-app'] = token;
    headers.Authorization = token;
  } else {
    headers['token-admin'] = token;
    headers['satoken-admin'] = token;
    headers.satoken = token;
    headers.Authorization = token;
  }
  return headers;
}

async function jsonRequest(base, headers, method, requestPath, body) {
  return request(base, requestPath, {
    method,
    headers,
    body: body == null ? undefined : JSON.stringify(body)
  });
}

function assertOk(ctx, condition, message, detail = {}) {
  if (!condition) {
    const suffix = Object.keys(detail).length ? ` ${JSON.stringify(detail)}` : '';
    const full = `${message}${suffix}`;
    ctx.failures.push(full);
    console.error(`FAIL ${full}`);
    return false;
  }
  console.log(`OK   ${message}`);
  return true;
}

export async function expectOk(ctx, method, requestPath, body, label, token = ctx.token) {
  const res = await ctx.adminRequest(method, requestPath, body, token);
  if (!(res.status === 200 && res.json?.code === 200)) {
    const detail = {method, path: requestPath, status: res.status, body: abbreviate(res.data)};
    ctx.ok(false, label || `${method} ${requestPath}`, detail);
    throw new Error(`${label || requestPath} failed: ${JSON.stringify(detail)}`);
  }
  ctx.ok(true, label || `${method} ${requestPath}`);
  return res.json.data;
}

export async function expectAppOk(ctx, method, requestPath, body, label, token = ctx.appToken) {
  const res = await ctx.appRequest(method, requestPath, body, token);
  if (!(res.status === 200 && res.json?.code === 200)) {
    const detail = {method, path: requestPath, status: res.status, body: abbreviate(res.data)};
    ctx.ok(false, label || `${method} ${requestPath}`, detail);
    throw new Error(`${label || requestPath} failed: ${JSON.stringify(detail)}`);
  }
  ctx.ok(true, label || `${method} ${requestPath}`);
  return res.json.data;
}

export async function expectBusinessFail(ctx, method, requestPath, body, label, token = ctx.token) {
  const res = await ctx.adminRequest(method, requestPath, body, token);
  const failed = res.status !== 200 || res.json?.code !== 200;
  if (!failed) {
    const detail = {method, path: requestPath, status: res.status, body: abbreviate(res.data)};
    ctx.ok(false, label || `${method} ${requestPath} should fail`, detail);
    throw new Error(`${label || requestPath} unexpectedly succeeded`);
  }
  ctx.ok(true, label || `${method} ${requestPath} business fail`);
  return res;
}

function abbreviate(value) {
  const text = String(value ?? '');
  return text.length > 500 ? `${text.slice(0, 500)}...` : text;
}

function latestId(ctx, table, column, value, idColumn = 'id') {
  return ctx.scalar(`SELECT ${idColumn} FROM ${table} WHERE ${column}='${escapeSql(value)}' ORDER BY create_time DESC, ${idColumn} DESC LIMIT 1`);
}

async function runFlow(ctx, name, fn) {
  section(`Flow ${ctx.flowResults.length + 1}: ${name}`);
  const before = ctx.failures.length;
  try {
    await fn();
    const passed = ctx.failures.length === before;
    ctx.flowResults.push({name, passed});
    if (passed) {
      console.log(`PASS ${name}`);
    } else {
      console.error(`FAIL ${name}`);
    }
  } catch (error) {
    ctx.flowResults.push({name, passed: false, error: error.message});
    ctx.failures.push(`${name}: ${error.message}`);
    console.error(`FAIL ${name}: ${error.message}`);
  }
}
