import {assertAuditStatus, assertNoTypedSourceFlows, assertStockFlow, assertStockQty, currentStockQty} from '../assertions.mjs';
import {submitAndPass} from '../approval.mjs';

export async function stockCheckFlow(ctx) {
  const no = `${ctx.prefix}_CHECK`;
  const beforeProfit = currentStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId);
  const targetProfit = beforeProfit + 3;
  await ctx.expectOk('POST', '/erp/stock/check', {
    checkNo: no,
    checkDate: ctx.today(),
    warehouseId: ctx.seed.warehouseId,
    remark: `${ctx.prefix} stock check`,
    items: [{productId: ctx.seed.productId, checkQty: targetProfit, remark: '盘盈'}]
  }, 'create stock check profit');
  const id = ctx.stockCheckId(no);
  ctx.created.stockChecks.push({id});
  await submitAndPass(ctx, 'STOCK_CHECK', id, 'stock check profit');
  assertAuditStatus(ctx, 'erp_stock_check', id, 1, 'stock check audited after approval');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, targetProfit, 'stock check profit updates balance');
  assertStockFlow(ctx, id, no, 'STOCK_CHECK', 'IN', 3, 'stock check profit creates IN flow');

  await ctx.expectOk('PUT', `/erp/stock/check/unaudit/${id}`, null, 'stock check unaudit rollback');
  assertAuditStatus(ctx, 'erp_stock_check', id, 0, 'stock check unaudit resets status');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, beforeProfit, 'stock check unaudit restores balance');
  assertNoTypedSourceFlows(ctx, id, 'STOCK_CHECK', 'stock check unaudit removes flows');

  const lossNo = `${ctx.prefix}_CHECK_LOSS`;
  const beforeLoss = currentStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId);
  const targetLoss = beforeLoss - 2;
  await ctx.expectOk('POST', '/erp/stock/check', {
    checkNo: lossNo,
    checkDate: ctx.today(),
    warehouseId: ctx.seed.warehouseId,
    remark: `${ctx.prefix} stock check loss`,
    items: [{productId: ctx.seed.productId, checkQty: targetLoss, remark: '盘亏'}]
  }, 'create stock check loss');
  const lossId = ctx.stockCheckId(lossNo);
  ctx.created.stockChecks.push({id: lossId});
  await submitAndPass(ctx, 'STOCK_CHECK', lossId, 'stock check loss');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, targetLoss, 'stock check loss updates balance');
  assertStockFlow(ctx, lossId, lossNo, 'STOCK_CHECK', 'OUT', 2, 'stock check loss creates OUT flow');
  await ctx.expectOk('PUT', `/erp/stock/check/unaudit/${lossId}`, null, 'stock check loss unaudit rollback');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, beforeLoss, 'stock check loss unaudit restores balance');

  const negativeNo = `${ctx.prefix}_CHECK_NEG`;
  await ctx.expectBusinessFail('POST', '/erp/stock/check', {
    checkNo: negativeNo,
    checkDate: ctx.today(),
    warehouseId: ctx.seed.warehouseId,
    remark: `${ctx.prefix} invalid stock check`,
    items: [{productId: ctx.seed.productId, checkQty: -1, remark: '负库存'}]
  }, 'stock check negative qty is rejected');
}
