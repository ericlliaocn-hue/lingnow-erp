#!/usr/bin/env node

import {existsSync} from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import {assertClean, cleanupAcceptanceSql, cleanupRuntime} from './acceptance/cleanup.mjs';
import {createContext} from './acceptance/context.mjs';
import {seedAll, loginAdmin} from './acceptance/seed.mjs';
import {masterDataFlow} from './acceptance/flows/master-data.mjs';
import {insufficientStockFlow, purchaseFlow, purchaseReturnFlow, saleFlow, saleReturnFlow, unauditBillFlow} from './acceptance/flows/bill-flows.mjs';
import {paymentFlow, receiptFlow} from './acceptance/flows/finance-flows.mjs';
import {stockCheckFlow} from './acceptance/flows/stock-check-flow.mjs';
import {approvalCompositeFlow} from './acceptance/flows/approval-flow.mjs';
import {importExportFlow} from './acceptance/flows/import-export-flow.mjs';
import {reportFlow} from './acceptance/flows/report-flow.mjs';
import {configFlow} from './acceptance/flows/config-flow.mjs';
import {dataAuthorizationFlow, permissionFlow} from './acceptance/flows/permission-flow.mjs';
import {notificationFlow} from './acceptance/flows/notification-flow.mjs';
import {monitorFlow} from './acceptance/flows/monitor-flow.mjs';
import {appFlow} from './acceptance/flows/app-flow.mjs';

function checkMenuComponents(ctx) {
  ctx.section('Static Menu Component Check');
  const rows = ctx.rows("SELECT menu_id,menu_name,menu_type,path,component FROM sys_menu WHERE del_flag=0 AND visible=1 AND status=1 ORDER BY menu_id")
    .map(([menuId, menuName, menuType, routePath, component]) => ({menuId, menuName, menuType, routePath, component}));
  const missing = [];
  for (const row of rows) {
    if (row.menuType !== '1' || !row.component || ['Layout', 'ParentView'].includes(row.component)) continue;
    const vueFile = path.join(ctx.root, 'admin-ui/src/views', `${row.component}.vue`);
    const indexFile = path.join(ctx.root, 'admin-ui/src/views', row.component, 'index.vue');
    if (!existsSync(vueFile) && !existsSync(indexFile)) {
      missing.push(row);
    }
  }
  const pageCount = rows.filter((row) => row.menuType === '1').length;
  ctx.ok(pageCount >= 70, `visible page menu count is at least 70 (${pageCount})`);
  ctx.ok(missing.length === 0, `all visible page menu components exist (${missing.length} missing)`, {missing});
}

async function checkApiMatrix(ctx) {
  ctx.section('Admin API Matrix');
  const endpoints = [
    ['/admin/menu/tree', 'menu tree'],
    ['/admin/menu/tree/all', 'all menu tree'],
    ['/admin/menu/list', 'menu list'],
    ['/user/list?current=1&size=10', 'users'],
    ['/user/stats', 'user stats'],
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
    ['/erp/report/inventory-change', 'inventory change'],
    ['/system/notification/unread-count', 'notification unread count'],
    ['/system/notification/list?current=1&size=10', 'notification list']
  ];

  let checked = 0;
  for (const [requestPath, label] of endpoints) {
    const res = await ctx.adminRequest('GET', requestPath, null);
    checked += 1;
    ctx.ok(res.status === 200 && res.json?.code === 200, `${label} ${requestPath}`, {
      status: res.status,
      body: res.data
    });
  }
  ctx.ok(checked >= 86, `admin API matrix checked at least 86 endpoints (${checked})`);
}

async function main() {
  const ctx = createContext();
  ctx.section('Environment');
  console.log(`root=${ctx.root}`);
  console.log(`adminBase=${ctx.adminBase}`);
  console.log(`appBase=${ctx.appBase}`);
  console.log(`dbName=${ctx.dbName}`);
  console.log(`prefix=${ctx.prefix}`);
  console.log('note=Do not run this API acceptance with the same admin account while external Chrome page testing is active.');

  try {
    cleanupAcceptanceSql(ctx);
    ctx.token = await loginAdmin(ctx);
    checkMenuComponents(ctx);
    await checkApiMatrix(ctx);
    await seedAll(ctx);

    await ctx.runFlow('基础资料：新增/编辑/查询/引用删除拦截', () => masterDataFlow(ctx));
    await ctx.runFlow('进货入库：保存不动库存，审批后库存/应付/资金正确', () => purchaseFlow(ctx));
    await ctx.runFlow('销售出库：审批扣库存、应收/资金/新销售通知正确', () => saleFlow(ctx));
    await ctx.runFlow('库存不足：超库存销售失败且余额/流水不变', () => insufficientStockFlow(ctx));
    await ctx.runFlow('销售退货：库存增加、应收冲减、退款方向正确', () => saleReturnFlow(ctx));
    await ctx.runFlow('进货退货：库存减少、应付冲减、退款方向正确', () => purchaseReturnFlow(ctx));
    await ctx.runFlow('收款单：审批生成资金/往来流水，反审核回滚', () => receiptFlow(ctx));
    await ctx.runFlow('付款单：审批生成资金/往来流水，反审核回滚', () => paymentFlow(ctx));
    await ctx.runFlow('库存盘点：盘盈/盘亏/负数拦截/反审核回滚', () => stockCheckFlow(ctx));
    await ctx.runFlow('审批综合：提交/审批人/无权/通过/驳回/撤回/转交/历史', () => approvalCompositeFlow(ctx));
    await ctx.runFlow('商品导入导出：模板/导入/导出字段和数据校验', () => importExportFlow(ctx));
    await ctx.runFlow('单据导出打印：销售/进货/退货导出、打印和反审核回滚校验', async () => {
      const salePrint = await ctx.expectOk('GET', `/erp/sale/print/${ctx.seed.sale.id}`, null, 'sale print data recheck');
      ctx.ok(JSON.stringify(salePrint).includes(ctx.seed.sale.no), 'sale print data contains accepted sale');
      for (const module of ['sale', 'sale-return', 'purchase', 'purchase-return']) {
        const res = await ctx.request(ctx.adminBase, `/erp/${module}/export?current=1&size=20`, {headers: {'token-admin': ctx.token}});
        ctx.ok(res.status === 200 && res.data.includes(','), `${module} export returns CSV`);
      }
      await unauditBillFlow(ctx);
    });
    await ctx.runFlow('报表：销售/进货/利润/库存/往来/账户与测试单据对账', () => reportFlow(ctx));
    await ctx.runFlow('系统配置：单号规则/字段设置/打印模板新增修改查询', () => configFlow(ctx));
    await ctx.runFlow('权限：角色/职员/菜单/按钮接口权限', () => permissionFlow(ctx));
    await ctx.runFlow('数据授权：受限用户客户/仓库隔离能力', () => dataAuthorizationFlow(ctx));
    await ctx.runFlow('通知中心：未读/已读/审批通知/新销售通知/跳转地址', () => notificationFlow(ctx));
    await ctx.runFlow('服务监控：服务/缓存/任务启停/执行/日志', () => monitorFlow(ctx));
    await ctx.runFlow('移动端/App：鉴权/业务壳接口/地址识别', () => appFlow(ctx));
  } finally {
    await cleanupRuntime(ctx).catch((error) => {
      ctx.failures.push(`cleanup failed: ${error.message}`);
      console.error(`FAIL cleanup failed: ${error.message}`);
    });
    assertClean(ctx);
  }

  ctx.section('Release Acceptance Summary');
  for (const [index, result] of ctx.flowResults.entries()) {
    const status = result.passed ? 'PASS' : 'FAIL';
    console.log(`${String(index + 1).padStart(2, '0')}. ${status} ${result.name}${result.error ? ` - ${result.error}` : ''}`);
  }

  if (ctx.flowResults.length !== 19) {
    ctx.failures.push(`expected 19 flows, got ${ctx.flowResults.length}`);
  }
  const failedFlows = ctx.flowResults.filter((item) => !item.passed);
  if (ctx.failures.length > 0 || failedFlows.length > 0) {
    console.error(`\nRelease acceptance failed: ${ctx.failures.length} failure(s), ${failedFlows.length} failed flow(s).`);
    process.exit(1);
  }
  console.log('\nRelease acceptance passed: 19/19 flows.');
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
