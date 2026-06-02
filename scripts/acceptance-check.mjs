#!/usr/bin/env node

import { existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import http from 'node:http';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(import.meta.dirname, '..');
const adminBase = process.env.ADMIN_BASE_URL || 'http://localhost:6060';
const appBase = process.env.APP_BASE_URL || 'http://localhost:6061';
const dbName = process.env.DB_NAME || 'lingnow_erp';
const dbUser = process.env.DB_USER || 'root';
const dbPassword = process.env.MYSQL_PWD || '';
const adminUsername = process.env.ADMIN_USERNAME || 'admin';
const adminPassword = process.env.ADMIN_PASSWORD || '123456';
const deliveryPrefix = process.env.DELIVERY_PREFIX || `DELIVERY_${Date.now()}`;

const failures = [];
const created = {
  bills: [],
  financeBills: [],
  stockChecks: [],
  products: [],
  masters: []
};

function section(name) {
  console.log(`\n## ${name}`);
}

function assert(condition, message) {
  if (!condition) {
    failures.push(message);
    console.error(`FAIL ${message}`);
    return;
  }
  console.log(`OK   ${message}`);
}

function mysql(query) {
  if (!dbPassword) {
    throw new Error('MYSQL_PWD is required for acceptance database checks');
  }
  return execFileSync('mysql', ['-u' + dbUser, dbName, '-N', '-B', '-e', query], {
    encoding: 'utf8',
    env: {...process.env, MYSQL_PWD: dbPassword}
  });
}

function mysqlScalar(query) {
  const output = mysql(query).trim();
  if (!output) return null;
  return output.split(/\t|\n/)[0];
}

function request(base, requestPath, options = {}) {
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
      let data = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => {
        data += chunk;
      });
      res.on('end', () => {
        let json = null;
        try {
          json = JSON.parse(data);
        } catch {
          // CSV/export endpoints are intentionally not JSON.
        }
        resolve({status: res.statusCode, headers: res.headers, data, json});
      });
    });
    req.on('error', (error) => resolve({status: 0, error: error.message, data: ''}));
    if (options.body) {
      req.write(options.body);
    }
    req.end();
  });
}

async function jsonRequest(token, method, requestPath, body) {
  const res = await request(adminBase, requestPath, {
    method,
    headers: {
      'Content-Type': 'application/json',
      'token-admin': token
    },
    body: body == null ? undefined : JSON.stringify(body)
  });
  return res;
}

async function expectOk(token, method, requestPath, body, label) {
  const res = await jsonRequest(token, method, requestPath, body);
  assert(res.status === 200 && res.json?.code === 200, label || `${method} ${requestPath}`);
  if (!(res.status === 200 && res.json?.code === 200)) {
    throw new Error(`${label || requestPath} failed: ${res.status} ${res.data}`);
  }
  return res.json.data;
}

async function loginAdmin() {
  const res = await request(adminBase, '/auth/login', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({username: adminUsername, password: adminPassword})
  });
  assert(res.status === 200 && res.json?.code === 200 && res.json?.data?.token, 'admin real login returns token');
  return res.json?.data?.token;
}

function checkMenuComponents() {
  section('Menu Components');
  const rows = mysql("SELECT menu_id,menu_name,menu_type,path,component FROM sys_menu WHERE del_flag=0 AND visible=1 AND status=1 ORDER BY menu_id")
    .trim()
    .split('\n')
    .filter(Boolean)
    .map((line) => {
      const [menuId, menuName, menuType, routePath, component] = line.split('\t');
      return {
        menuId,
        menuName,
        menuType,
        routePath: routePath === 'NULL' ? null : routePath,
        component: component === 'NULL' ? null : component
      };
    });
  const missing = [];
  for (const row of rows) {
    if (row.menuType !== '1' || !row.component || ['Layout', 'ParentView'].includes(row.component)) {
      continue;
    }
    const vueFile = path.join(root, 'admin-ui/src/views', `${row.component}.vue`);
    const indexFile = path.join(root, 'admin-ui/src/views', row.component, 'index.vue');
    if (!existsSync(vueFile) && !existsSync(indexFile)) {
      missing.push(row);
    }
  }
  const pageCount = rows.filter((row) => row.menuType === '1').length;
  assert(pageCount >= 70, `visible page menu count is at least 70 (${pageCount})`);
  assert(missing.length === 0, `all visible page menu components exist (${missing.length} missing)`);
}

function checkPermissionsAndCleanup() {
  section('Permissions And Cleanup');
  cleanupDeliverySql();
  const [[menus], [bindings], [missingBindings], [stage20Users], [stage20Infos], [stage20Products], [deliveryRows]] = [
    "SELECT COUNT(*) FROM sys_menu WHERE del_flag=0",
    "SELECT COUNT(*) FROM sys_role_menu WHERE role_id=1",
    "SELECT COUNT(*) FROM sys_menu m LEFT JOIN sys_role_menu rm ON rm.menu_id=m.menu_id AND rm.role_id=1 WHERE m.del_flag=0 AND rm.menu_id IS NULL",
    "SELECT COUNT(*) FROM app_user WHERE username LIKE 'STAGE20%'",
    "SELECT COUNT(*) FROM app_user_info WHERE user_id=92020010020",
    "SELECT COUNT(*) FROM erp_product WHERE code LIKE 'STAGE20%'",
    "SELECT (SELECT COUNT(*) FROM erp_product WHERE code LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_customer WHERE code LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_supplier WHERE code LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_warehouse WHERE code LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_account WHERE code LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_bill WHERE bill_no LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_finance_bill WHERE bill_no LIKE 'DELIVERY_%')"
  ].map((query) => mysql(query).trim().split('\t'));
  assert(Number(menus) >= 219, `active menu/button count is at least 219 (${menus})`);
  assert(Number(bindings) >= Number(menus), `super admin has all active menu/button bindings (${bindings}/${menus})`);
  assert(Number(missingBindings) === 0, 'super admin has no missing menu bindings');
  assert(Number(stage20Users) === 0, 'no STAGE20 app users remain');
  assert(Number(stage20Infos) === 0, 'no STAGE20 app user info remains');
  assert(Number(stage20Products) === 0, 'no STAGE20 product rows remain');
  assert(Number(deliveryRows) === 0, 'no DELIVERY verification rows remain before run');
}

function adminEndpoints() {
  return [
    ['/admin/menu/tree', 'menu tree'],
    ['/admin/menu/tree/all', 'all menu tree'],
    ['/admin/menu/list', 'menu list'],
    ['/user/list?current=1&size=10', 'users'],
    ['/role/list?current=1&size=10', 'roles'],
    ['/role/active', 'active roles'],
    ['/admin/file/page?current=1&size=10', 'files'],
    ['/admin/file/config/list', 'file config'],
    ['/system/dept/list', 'dept'],
    ['/system/post/list?current=1&size=10', 'post'],
    ['/system/dict/type/list?current=1&size=10', 'dict type'],
    ['/system/dict/type/optionselect', 'dict options'],
    ['/system/config/list?current=1&size=10', 'system config'],
    ['/system/notice/list?current=1&size=10', 'notice'],
    ['/system/staff/list?current=1&size=10', 'staff'],
    ['/sys/log/oper/list?current=1&size=10', 'oper log'],
    ['/sys/log/login/list?current=1&size=10', 'login log'],
    ['/sys/log/error/list?current=1&size=10', 'error log'],
    ['/sys/log/slowSql/list?current=1&size=10', 'slow sql log'],
    ['/monitor/admin/dashboard', 'service monitor'],
    ['/monitor/cache', 'cache monitor'],
    ['/monitor/online/list', 'online users'],
    ['/monitor/job/list?current=1&size=10', 'job monitor'],
    ['/monitor/job/log/list?current=1&size=10', 'job logs'],
    ['/erp/approval/todo/list?current=1&size=10', 'approval todo'],
    ['/erp/approval/mine/list?current=1&size=10', 'approval mine'],
    ['/erp/approval/done/list?current=1&size=10', 'approval done'],
    ['/warm-flow-ui/token-name', 'warm-flow ui token name'],
    ['/warm-flow/handler-type', 'warm-flow handler type'],
    ['/erp/product/list?current=1&size=10', 'product'],
    ['/erp/product/options', 'product options'],
    ...['product-category', 'unit', 'product-brand', 'product-attribute', 'customer', 'supplier', 'warehouse', 'account', 'agent-level']
      .map((type) => [`/erp/master/${type}/list?current=1&size=10`, `master ${type}`]),
    ...['bill-no-rule', 'field-setting', 'print-template']
      .map((type) => [`/erp/config/${type}/list?current=1&size=10`, `config ${type}`]),
    ['/erp/system/params', 'erp params'],
    ...['sale', 'sale-return', 'purchase', 'purchase-return']
      .flatMap((module) => [
        [`/erp/${module}/list?current=1&size=10`, `${module} list`],
        [`/erp/${module}/nextNo`, `${module} next no`]
      ]),
    ['/erp/stock/check/list?current=1&size=10', 'stock check'],
    ['/erp/stock/check/nextNo', 'stock check next no'],
    ['/erp/stock/warning/list?current=1&size=10', 'stock warning'],
    ...['receipt', 'payment', 'income', 'expense']
      .flatMap((module) => [
        [`/erp/finance/${module}/list?current=1&size=10`, `finance ${module}`],
        [`/erp/finance/${module}/nextNo`, `finance ${module} next no`]
      ]),
    ['/erp/finance/fund-flow/list?current=1&size=10', 'fund flow'],
    ['/erp/finance/partner-flow/list?current=1&size=10', 'partner flow'],
    ['/erp/report/summary', 'report summary'],
    ['/erp/report/stock-balance?current=1&size=10', 'stock balance report'],
    ['/erp/report/stock-flow?current=1&size=10', 'stock flow report'],
    ['/erp/report/bill-detail?current=1&size=10&billType=SALE', 'sale detail report'],
    ['/erp/report/bill-detail?current=1&size=10&billType=PURCHASE', 'purchase detail report'],
    ['/erp/report/partner-balance', 'partner balance'],
    ['/erp/report/account-balance', 'account balance'],
    ['/erp/report/bill-stat?billType=SALE&groupBy=date', 'sale stat'],
    ['/erp/report/bill-stat?billType=PURCHASE&groupBy=date', 'purchase stat'],
    ['/erp/report/profit?groupBy=product', 'profit product'],
    ['/erp/report/profit?groupBy=bill', 'profit bill'],
    ['/erp/report/profit?groupBy=customer', 'profit customer'],
    ['/erp/report/trend', 'trend'],
    ['/erp/report/business-profit', 'business profit'],
    ['/erp/report/hot-products', 'hot products'],
    ['/erp/report/employee-performance', 'employee performance'],
    ['/erp/report/stock-summary', 'stock summary'],
    ['/erp/report/inventory-change', 'inventory change']
  ];
}

function csvEndpoints() {
  return [
    ['/erp/product/import-template', 'product template'],
    ['/erp/product/export?current=1&size=10', 'product export'],
    ['/erp/sale/export?current=1&size=10', 'sale export'],
    ['/erp/sale-return/export?current=1&size=10', 'sale return export'],
    ['/erp/purchase/export?current=1&size=10', 'purchase export'],
    ['/erp/purchase-return/export?current=1&size=10', 'purchase return export']
  ];
}

async function checkAdminApi(token) {
  section('Admin API Matrix');
  let checked = 0;
  for (const [requestPath, label] of adminEndpoints()) {
    const res = await request(adminBase, requestPath, {headers: {'token-admin': token}});
    checked += 1;
    assert(res.status === 200 && res.json?.code === 200, `${label} ${requestPath}`);
  }
  for (const [requestPath, label] of csvEndpoints()) {
    const res = await request(adminBase, requestPath, {headers: {'token-admin': token}});
    checked += 1;
    assert(res.status === 200 && /^\uFEFF?[^,{]+,/.test(res.data), `${label} CSV ${requestPath}`);
  }
  assert(checked === 89, 'admin API matrix checked 89 endpoints');
}

async function checkAppApi() {
  section('App API Auth');
  const noToken = await request(appBase, '/app/erp/dashboard');
  assert(noToken.status === 200 && noToken.json?.code === 401, 'app ERP dashboard rejects missing token');
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function escapeSql(value) {
  return String(value).replaceAll("'", "''");
}

function latestId(table, column, value) {
  return mysqlScalar(`SELECT id FROM ${table} WHERE ${column}='${escapeSql(value)}' ORDER BY create_time DESC, id DESC LIMIT 1`);
}

function billId(billNo) {
  return latestId('erp_bill', 'bill_no', billNo);
}

function financeBillId(billNo) {
  return latestId('erp_finance_bill', 'bill_no', billNo);
}

function masterId(table, code) {
  return latestId(table, 'code', code);
}

async function addMaster(token, type, code, name, extra = {}) {
  await expectOk(token, 'POST', `/erp/master/${type}`, {
    code,
    name,
    status: 1,
    sortOrder: 999,
    remark: 'delivery acceptance temporary data',
    ...extra
  }, `create ${type} ${code}`);
  const table = {
    'product-category': 'erp_product_category',
    unit: 'erp_unit',
    'product-brand': 'erp_product_brand',
    customer: 'erp_customer',
    supplier: 'erp_supplier',
    warehouse: 'erp_warehouse',
    account: 'erp_account'
  }[type];
  const id = masterId(table, code);
  created.masters.push({type, id});
  return id;
}

async function addProduct(token, ids) {
  const code = `${deliveryPrefix}_PRODUCT`;
  await expectOk(token, 'POST', '/erp/product', {
    code,
    name: `${deliveryPrefix} 商品`,
    spec: '验收规格',
    categoryId: ids.categoryId,
    brandId: ids.brandId,
    unitId: ids.unitId,
    purchasePrice: 10,
    salePrice: 20,
    retailPrice: 20,
    minStock: 1,
    maxStock: 999,
    status: 1,
    remark: 'delivery acceptance temporary data'
  }, 'create delivery product');
  const id = latestId('erp_product', 'code', code);
  created.products.push(id);
  return id;
}

async function submitAndPass(token, bizType, bizId, label) {
  await expectOk(token, 'POST', '/erp/approval/submit', {bizType, bizId, comment: `${label} submit`}, `${label} submit approval`);
  const todo = await expectOk(token, 'GET', '/erp/approval/todo/list?current=1&size=20', null, `${label} query todo`);
  const task = todo.records.find((item) => String(item.bizId) === String(bizId) && item.bizType === bizType);
  assert(Boolean(task?.taskId), `${label} creates approval task`);
  await expectOk(token, 'POST', '/erp/approval/pass', {taskId: task.taskId, comment: `${label} pass`}, `${label} pass approval`);
}

async function checkDeliveryBusinessFlow(token) {
  section('Delivery Business Approval Flow');
  const date = today();
  const ids = {};
  ids.categoryId = await addMaster(token, 'product-category', `${deliveryPrefix}_CAT`, `${deliveryPrefix} 分类`);
  ids.unitId = await addMaster(token, 'unit', `${deliveryPrefix}_UNIT`, `${deliveryPrefix} 单位`);
  ids.brandId = await addMaster(token, 'product-brand', `${deliveryPrefix}_BRAND`, `${deliveryPrefix} 品牌`);
  ids.customerId = await addMaster(token, 'customer', `${deliveryPrefix}_CUS`, `${deliveryPrefix} 客户`, {contact: '验收客户', phone: '13000000000'});
  ids.supplierId = await addMaster(token, 'supplier', `${deliveryPrefix}_SUP`, `${deliveryPrefix} 供应商`, {contact: '验收供应商', phone: '13100000000'});
  ids.warehouseId = await addMaster(token, 'warehouse', `${deliveryPrefix}_WH`, `${deliveryPrefix} 仓库`);
  ids.accountId = await addMaster(token, 'account', `${deliveryPrefix}_ACC`, `${deliveryPrefix} 账户`, {accountType: 'CASH', openingBalance: 0});
  ids.productId = await addProduct(token, ids);

  const purchaseNo = `${deliveryPrefix}_PURCHASE`;
  await expectOk(token, 'POST', '/erp/purchase', {
    billNo: purchaseNo,
    billDate: date,
    partnerId: ids.supplierId,
    warehouseId: ids.warehouseId,
    accountId: ids.accountId,
    paidAmount: 0,
    items: [{productId: ids.productId, warehouseId: ids.warehouseId, qty: 5, price: 10}]
  }, 'create purchase bill');
  const purchaseId = billId(purchaseNo);
  created.bills.push({module: 'purchase', id: purchaseId});
  await submitAndPass(token, 'PURCHASE', purchaseId, 'purchase');
  assert(Number(mysqlScalar(`SELECT audit_status FROM erp_bill WHERE id=${purchaseId}`)) === 1, 'purchase approval audits bill');
  assert(Number(mysqlScalar(`SELECT qty FROM erp_stock_balance WHERE product_id=${ids.productId} AND warehouse_id=${ids.warehouseId}`)) === 5, 'purchase approval increases stock to 5');

  const saleNo = `${deliveryPrefix}_SALE`;
  await expectOk(token, 'POST', '/erp/sale', {
    billNo: saleNo,
    billDate: date,
    partnerId: ids.customerId,
    warehouseId: ids.warehouseId,
    accountId: ids.accountId,
    paidAmount: 20,
    items: [{productId: ids.productId, warehouseId: ids.warehouseId, qty: 1, price: 20}]
  }, 'create sale bill');
  const saleId = billId(saleNo);
  created.bills.push({module: 'sale', id: saleId});
  await submitAndPass(token, 'SALE', saleId, 'sale');
  assert(Number(mysqlScalar(`SELECT audit_status FROM erp_bill WHERE id=${saleId}`)) === 1, 'sale approval audits bill');
  assert(Number(mysqlScalar(`SELECT qty FROM erp_stock_balance WHERE product_id=${ids.productId} AND warehouse_id=${ids.warehouseId}`)) === 4, 'sale approval decreases stock to 4');
  assert(Number(mysqlScalar(`SELECT COUNT(*) FROM erp_fund_flow WHERE source_bill_id=${saleId} AND source_bill_type='SALE'`)) > 0, 'sale approval creates fund flow');

  const receiptNo = `${deliveryPrefix}_RECEIPT`;
  await expectOk(token, 'POST', '/erp/finance/receipt', {
    billNo: receiptNo,
    billDate: date,
    partnerId: ids.customerId,
    accountId: ids.accountId,
    amount: 5,
    remark: 'delivery receipt'
  }, 'create receipt bill');
  const receiptId = financeBillId(receiptNo);
  created.financeBills.push({module: 'receipt', id: receiptId});
  await submitAndPass(token, 'RECEIPT', receiptId, 'receipt');
  assert(Number(mysqlScalar(`SELECT audit_status FROM erp_finance_bill WHERE id=${receiptId}`)) === 1, 'receipt approval audits finance bill');
  assert(Number(mysqlScalar(`SELECT COUNT(*) FROM erp_fund_flow WHERE source_bill_id=${receiptId} AND source_bill_type='RECEIPT'`)) > 0, 'receipt approval creates fund flow');

  await expectOk(token, 'PUT', `/erp/finance/receipt/unaudit/${receiptId}`, null, 'receipt unaudit');
  await expectOk(token, 'DELETE', `/erp/finance/receipt/${receiptId}`, null, 'delete receipt');
  created.financeBills = created.financeBills.filter((item) => item.id !== receiptId);

  await expectOk(token, 'PUT', `/erp/sale/unaudit/${saleId}`, null, 'sale unaudit');
  await expectOk(token, 'DELETE', `/erp/sale/${saleId}`, null, 'delete sale');
  created.bills = created.bills.filter((item) => item.id !== saleId);

  await expectOk(token, 'PUT', `/erp/purchase/unaudit/${purchaseId}`, null, 'purchase unaudit');
  await expectOk(token, 'DELETE', `/erp/purchase/${purchaseId}`, null, 'delete purchase');
  created.bills = created.bills.filter((item) => item.id !== purchaseId);

  await cleanupDeliveryData(token);
  assert(Number(mysqlScalar(`SELECT COUNT(*) FROM erp_product WHERE code LIKE '${escapeSql(deliveryPrefix)}%'`)) === 0, 'delivery product cleaned');
}

async function cleanupDeliveryData(token) {
  section('Delivery Data Cleanup');
  for (const item of [...created.financeBills].reverse()) {
    await jsonRequest(token, 'PUT', `/erp/finance/${item.module}/unaudit/${item.id}`, null);
    await jsonRequest(token, 'DELETE', `/erp/finance/${item.module}/${item.id}`, null);
  }
  created.financeBills = [];
  for (const item of [...created.bills].reverse()) {
    await jsonRequest(token, 'PUT', `/erp/${item.module}/unaudit/${item.id}`, null);
    await jsonRequest(token, 'DELETE', `/erp/${item.module}/${item.id}`, null);
  }
  created.bills = [];
  cleanupDeliverySql();
  for (const id of [...created.products].reverse()) {
    await jsonRequest(token, 'DELETE', `/erp/product/${id}`, null);
  }
  created.products = [];
  for (const item of [...created.masters].reverse()) {
    await jsonRequest(token, 'DELETE', `/erp/master/${item.type}/${item.id}`, null);
  }
  created.masters = [];
  cleanupDeliverySql();
}

function checkNoDeliveryRows() {
  cleanupDeliverySql();
  const count = Number(mysqlScalar("SELECT (SELECT COUNT(*) FROM erp_product WHERE code LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_customer WHERE code LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_supplier WHERE code LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_warehouse WHERE code LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_account WHERE code LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_bill WHERE bill_no LIKE 'DELIVERY_%') + (SELECT COUNT(*) FROM erp_finance_bill WHERE bill_no LIKE 'DELIVERY_%')"));
  assert(count === 0, `no DELIVERY verification rows remain after run (${count})`);
}

function cleanupDeliverySql() {
  mysql(`
    SET @delivery_instances = (
      SELECT GROUP_CONCAT(approval_instance_id)
      FROM (
        SELECT approval_instance_id FROM erp_bill WHERE bill_no LIKE 'DELIVERY_%' AND approval_instance_id IS NOT NULL
        UNION ALL
        SELECT approval_instance_id FROM erp_finance_bill WHERE bill_no LIKE 'DELIVERY_%' AND approval_instance_id IS NOT NULL
        UNION ALL
        SELECT approval_instance_id FROM erp_stock_check WHERE check_no LIKE 'DELIVERY_%' AND approval_instance_id IS NOT NULL
      ) t
    );
    DELETE fu FROM flow_user fu JOIN flow_task ft ON fu.associated = ft.id WHERE FIND_IN_SET(ft.instance_id, IFNULL(@delivery_instances, ''));
    DELETE FROM flow_his_task WHERE FIND_IN_SET(instance_id, IFNULL(@delivery_instances, ''));
    DELETE FROM flow_task WHERE FIND_IN_SET(instance_id, IFNULL(@delivery_instances, ''));
    DELETE FROM flow_instance WHERE FIND_IN_SET(id, IFNULL(@delivery_instances, ''));
    DELETE FROM erp_fund_flow WHERE source_bill_no LIKE 'DELIVERY_%';
    DELETE FROM erp_partner_flow WHERE source_bill_no LIKE 'DELIVERY_%';
    DELETE FROM erp_stock_flow WHERE source_bill_no LIKE 'DELIVERY_%';
    DELETE FROM erp_stock_check_item WHERE check_id IN (SELECT id FROM erp_stock_check WHERE check_no LIKE 'DELIVERY_%');
    DELETE FROM erp_stock_check WHERE check_no LIKE 'DELIVERY_%';
    DELETE FROM erp_bill_item WHERE bill_id IN (SELECT id FROM erp_bill WHERE bill_no LIKE 'DELIVERY_%');
    DELETE FROM erp_bill WHERE bill_no LIKE 'DELIVERY_%';
    DELETE FROM erp_finance_bill WHERE bill_no LIKE 'DELIVERY_%';
    DELETE FROM erp_stock_balance WHERE product_id IN (SELECT id FROM erp_product WHERE code LIKE 'DELIVERY_%');
    DELETE FROM erp_product WHERE code LIKE 'DELIVERY_%';
    DELETE FROM erp_customer WHERE code LIKE 'DELIVERY_%';
    DELETE FROM erp_supplier WHERE code LIKE 'DELIVERY_%';
    DELETE FROM erp_warehouse WHERE code LIKE 'DELIVERY_%';
    DELETE FROM erp_account WHERE code LIKE 'DELIVERY_%';
    DELETE FROM erp_product_brand WHERE code LIKE 'DELIVERY_%';
    DELETE FROM erp_unit WHERE code LIKE 'DELIVERY_%';
    DELETE FROM erp_product_category WHERE code LIKE 'DELIVERY_%';
  `);
}

async function main() {
  section('Environment');
  console.log(`root=${root}`);
  console.log(`adminBase=${adminBase}`);
  console.log(`appBase=${appBase}`);
  const token = await loginAdmin();
  checkMenuComponents();
  checkPermissionsAndCleanup();
  await checkAdminApi(token);
  await checkAppApi();
  try {
    await checkDeliveryBusinessFlow(token);
  } finally {
    await cleanupDeliveryData(token).catch((error) => {
      failures.push(`cleanup failed: ${error.message}`);
      console.error(`FAIL cleanup failed: ${error.message}`);
    });
  }
  checkNoDeliveryRows();

  if (failures.length > 0) {
    console.error(`\nAcceptance check failed: ${failures.length} failure(s).`);
    process.exit(1);
  }
  console.log('\nAcceptance check passed.');
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
