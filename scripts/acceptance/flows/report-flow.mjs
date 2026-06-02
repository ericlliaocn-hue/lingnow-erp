import {assertReportHasBill} from '../assertions.mjs';

export async function reportFlow(ctx) {
  const endpoints = [
    ['/erp/report/summary', 'report summary'],
    [`/erp/report/stock-balance?current=1&size=20&warehouseId=${ctx.seed.warehouseId}`, 'stock balance report'],
    [`/erp/report/stock-flow?current=1&size=20&warehouseId=${ctx.seed.warehouseId}`, 'stock flow report'],
    ['/erp/report/bill-detail?current=1&size=20&billType=SALE', 'sale detail report'],
    ['/erp/report/bill-detail?current=1&size=20&billType=PURCHASE', 'purchase detail report'],
    ['/erp/report/partner-balance', 'partner balance report'],
    ['/erp/report/account-balance', 'account balance report'],
    ['/erp/report/bill-stat?billType=SALE&groupBy=date', 'sale stat report'],
    ['/erp/report/bill-stat?billType=PURCHASE&groupBy=date', 'purchase stat report'],
    ['/erp/report/profit?groupBy=product', 'profit by product'],
    ['/erp/report/profit?groupBy=bill', 'profit by bill'],
    ['/erp/report/profit?groupBy=customer', 'profit by customer'],
    ['/erp/report/trend', 'trend report'],
    ['/erp/report/business-profit', 'business profit report'],
    ['/erp/report/hot-products', 'hot products report'],
    ['/erp/report/employee-performance', 'employee performance report'],
    ['/erp/report/stock-summary', 'stock summary report'],
    ['/erp/report/inventory-change', 'inventory change report']
  ];

  const payloads = new Map();
  for (const [path, label] of endpoints) {
    const data = await ctx.expectOk('GET', path, null, label);
    payloads.set(label, data);
  }
  assertReportHasBill(ctx, payloads.get('sale detail report'), ctx.seed.sale.no, 'sale detail report includes accepted sale');
  assertReportHasBill(ctx, payloads.get('purchase detail report'), ctx.seed.purchase.no, 'purchase detail report includes accepted purchase');

  const stockQty = Number(ctx.scalar(`SELECT IFNULL(qty,0) FROM erp_stock_balance WHERE product_id='${ctx.escapeSql(ctx.seed.productId)}' AND warehouse_id='${ctx.escapeSql(ctx.seed.warehouseId)}'`));
  const stockBalanceText = JSON.stringify(payloads.get('stock balance report'));
  ctx.ok(stockBalanceText.includes(String(stockQty)), 'stock balance report matches database balance', {stockQty});

  const expectedBalance = Number(ctx.scalar(`
    SELECT
      (SELECT IFNULL(opening_balance,0) FROM erp_account WHERE id='${ctx.escapeSql(ctx.seed.accountId)}') +
      (SELECT IFNULL(SUM(CASE WHEN direction='IN' THEN amount ELSE -amount END),0) FROM erp_fund_flow WHERE account_id='${ctx.escapeSql(ctx.seed.accountId)}' AND del_flag=0)
  `));
  const accountRows = payloads.get('account balance report') || [];
  const row = accountRows.find((item) => String(item.accountName) === `${ctx.prefix} 现金账户`);
  ctx.ok(Boolean(row), 'account balance report includes acceptance account', {
    accountId: ctx.seed.accountId,
    accountName: `${ctx.prefix} 现金账户`
  });
  if (row) {
    ctx.ok(Number(row.balance) === expectedBalance, 'account balance report matches database fund-flow balance', {
      expected: expectedBalance,
      actual: Number(row.balance)
    });
  }
}
