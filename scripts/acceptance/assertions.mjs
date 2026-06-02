export function numberScalar(ctx, query) {
  return Number(ctx.scalar(query) || 0);
}

export function assertStockQty(ctx, productId, warehouseId, expected, label) {
  const actual = currentStockQty(ctx, productId, warehouseId);
  ctx.ok(actual === expected, label, {expected, actual, productId, warehouseId});
}

export function currentStockQty(ctx, productId, warehouseId) {
  return numberScalar(ctx, `SELECT IFNULL(qty,0) FROM erp_stock_balance WHERE product_id='${ctx.escapeSql(productId)}' AND warehouse_id='${ctx.escapeSql(warehouseId)}'`);
}

export function assertStockFlow(ctx, billId, billNo, billType, direction, expectedQty, label) {
  const row = ctx.rows(`
    SELECT direction, IFNULL(SUM(qty),0)
    FROM erp_stock_flow
    WHERE source_bill_id='${ctx.escapeSql(billId)}' AND source_bill_no='${ctx.escapeSql(billNo)}' AND source_bill_type='${ctx.escapeSql(billType)}'
    GROUP BY direction
  `).find((item) => item[0] === direction);
  const actual = Number(row?.[1] || 0);
  ctx.ok(actual === expectedQty, label, {expected: expectedQty, actual, billId, billNo, billType, direction});
}

export function assertFundFlow(ctx, sourceId, sourceNo, sourceType, direction, expectedAmount, label) {
  const actual = numberScalar(ctx, `
    SELECT IFNULL(SUM(amount),0)
    FROM erp_fund_flow
    WHERE source_bill_id='${ctx.escapeSql(sourceId)}'
      AND source_bill_no='${ctx.escapeSql(sourceNo)}'
      AND source_bill_type='${ctx.escapeSql(sourceType)}'
      AND direction='${ctx.escapeSql(direction)}'
  `);
  ctx.ok(actual === expectedAmount, label, {expected: expectedAmount, actual, sourceId, sourceNo, sourceType, direction});
}

export function assertPartnerFlow(ctx, sourceId, sourceNo, sourceType, direction, expectedAmount, label) {
  const actual = numberScalar(ctx, `
    SELECT IFNULL(SUM(amount),0)
    FROM erp_partner_flow
    WHERE source_bill_id='${ctx.escapeSql(sourceId)}'
      AND source_bill_no='${ctx.escapeSql(sourceNo)}'
      AND source_bill_type='${ctx.escapeSql(sourceType)}'
      AND direction='${ctx.escapeSql(direction)}'
  `);
  ctx.ok(actual === expectedAmount, label, {expected: expectedAmount, actual, sourceId, sourceNo, sourceType, direction});
}

export function assertNoSourceFlows(ctx, sourceId, sourceNo, label) {
  const actual = numberScalar(ctx, `
    SELECT
      (SELECT COUNT(*) FROM erp_stock_flow WHERE del_flag=0 AND (source_bill_id='${ctx.escapeSql(sourceId)}' OR source_bill_no='${ctx.escapeSql(sourceNo)}')) +
      (SELECT COUNT(*) FROM erp_fund_flow WHERE del_flag=0 AND (source_bill_id='${ctx.escapeSql(sourceId)}' OR source_bill_no='${ctx.escapeSql(sourceNo)}')) +
      (SELECT COUNT(*) FROM erp_partner_flow WHERE del_flag=0 AND (source_bill_id='${ctx.escapeSql(sourceId)}' OR source_bill_no='${ctx.escapeSql(sourceNo)}'))
  `);
  ctx.ok(actual === 0, label, {expected: 0, actual, sourceId, sourceNo});
}

export function assertNoTypedSourceFlows(ctx, sourceId, sourceType, label) {
  const actual = numberScalar(ctx, `
    SELECT
      (SELECT COUNT(*) FROM erp_stock_flow WHERE del_flag=0 AND source_bill_id='${ctx.escapeSql(sourceId)}' AND source_bill_type='${ctx.escapeSql(sourceType)}') +
      (SELECT COUNT(*) FROM erp_fund_flow WHERE del_flag=0 AND source_bill_id='${ctx.escapeSql(sourceId)}' AND source_bill_type='${ctx.escapeSql(sourceType)}') +
      (SELECT COUNT(*) FROM erp_partner_flow WHERE del_flag=0 AND source_bill_id='${ctx.escapeSql(sourceId)}' AND source_bill_type='${ctx.escapeSql(sourceType)}')
  `);
  ctx.ok(actual === 0, label, {expected: 0, actual, sourceId, sourceType});
}

export function assertAuditStatus(ctx, table, id, expected, label) {
  const actual = numberScalar(ctx, `SELECT audit_status FROM ${table} WHERE id='${ctx.escapeSql(id)}'`);
  ctx.ok(actual === expected, label, {expected, actual, table, id});
}

export function assertReportHasBill(ctx, data, billNo, label) {
  const text = JSON.stringify(data);
  ctx.ok(text.includes(billNo), label, {billNo});
}

export function assertNotification(ctx, bizId, bizType, label) {
  const count = numberScalar(ctx, `
    SELECT COUNT(*)
    FROM sys_user_notification
    WHERE biz_id='${ctx.escapeSql(bizId)}'
      AND biz_type='${ctx.escapeSql(bizType)}'
      AND del_flag=0
  `);
  ctx.ok(count > 0, label, {bizId, bizType, count});
}
