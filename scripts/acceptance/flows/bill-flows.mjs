import {assertAuditStatus, assertFundFlow, assertNoSourceFlows, assertNotification, assertPartnerFlow, assertStockFlow, assertStockQty} from '../assertions.mjs';
import {submitAndPass} from '../approval.mjs';
import {createBill} from '../seed.mjs';

export async function purchaseFlow(ctx) {
  const no = `${ctx.prefix}_PURCHASE`;
  const id = await createBill(ctx, 'purchase', no, ctx.seed.supplierId, 10, 10, 30);
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, 0, 'purchase save does not change stock');
  await submitAndPass(ctx, 'PURCHASE', id, 'purchase');
  assertAuditStatus(ctx, 'erp_bill', id, 1, 'purchase audited after approval');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, 10, 'purchase approval increases stock');
  assertStockFlow(ctx, id, no, 'PURCHASE', 'IN', 10, 'purchase creates IN stock flow');
  assertPartnerFlow(ctx, id, no, 'PURCHASE', 'PAYABLE', 100, 'purchase creates payable partner flow');
  assertFundFlow(ctx, id, no, 'PURCHASE', 'OUT', 30, 'purchase paid amount creates OUT fund flow');
  ctx.seed.purchase = {id, no};
}

export async function saleFlow(ctx) {
  const no = `${ctx.prefix}_SALE`;
  const id = await createBill(ctx, 'sale', no, ctx.seed.customerId, 2, 20, 20);
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, 10, 'sale save does not change stock');
  await submitAndPass(ctx, 'SALE', id, 'sale');
  assertAuditStatus(ctx, 'erp_bill', id, 1, 'sale audited after approval');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, 8, 'sale approval decreases stock');
  assertStockFlow(ctx, id, no, 'SALE', 'OUT', 2, 'sale creates OUT stock flow');
  assertPartnerFlow(ctx, id, no, 'SALE', 'RECEIVABLE', 40, 'sale creates receivable partner flow');
  assertFundFlow(ctx, id, no, 'SALE', 'IN', 20, 'sale paid amount creates IN fund flow');
  assertNotification(ctx, id, 'SALE', 'sale creates new order notification');
  const print = await ctx.expectOk('GET', `/erp/sale/print/${id}`, null, 'sale print data');
  ctx.ok(JSON.stringify(print).includes(no), 'sale print contains bill no', {billNo: no});
  ctx.seed.sale = {id, no};
}

export async function insufficientStockFlow(ctx) {
  const no = `${ctx.prefix}_SALE_OVER`;
  await ctx.expectOk('POST', '/erp/sale', {
    billNo: no,
    billDate: ctx.today(),
    partnerId: ctx.seed.customerId,
    warehouseId: ctx.seed.warehouseId,
    accountId: ctx.seed.accountId,
    paidAmount: 0,
    items: [{productId: ctx.seed.productId, warehouseId: ctx.seed.warehouseId, qty: 9999, price: 20}]
  }, 'create over-stock sale');
  const id = ctx.billId(no);
  ctx.created.bills.push({module: 'sale', id});
  await ctx.expectOk('POST', '/erp/approval/submit', {bizType: 'SALE', bizId: id, comment: 'over stock submit'}, 'over-stock sale submit');
  const todo = await ctx.expectOk('GET', '/erp/approval/todo/list?current=1&size=50&bizType=SALE', null, 'over-stock todo');
  const task = todo.records?.find((item) => String(item.bizId) === String(id));
  await ctx.expectBusinessFail('POST', '/erp/approval/pass', {taskId: task?.taskId, comment: 'over stock pass fail'}, 'over-stock sale approval fails');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, 8, 'over-stock sale leaves stock unchanged');
  assertNoSourceFlows(ctx, id, no, 'over-stock sale creates no flows');
}

export async function saleReturnFlow(ctx) {
  const no = `${ctx.prefix}_SALE_RETURN`;
  const id = await createBill(ctx, 'sale-return', no, ctx.seed.customerId, 1, 20, 5);
  await submitAndPass(ctx, 'SALE_RETURN', id, 'sale return');
  assertAuditStatus(ctx, 'erp_bill', id, 1, 'sale return audited after approval');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, 9, 'sale return increases stock');
  assertStockFlow(ctx, id, no, 'SALE_RETURN', 'IN', 1, 'sale return creates IN stock flow');
  assertPartnerFlow(ctx, id, no, 'SALE_RETURN', 'RECEIVE', 20, 'sale return offsets receivable');
  assertFundFlow(ctx, id, no, 'SALE_RETURN', 'OUT', 5, 'sale return paid amount creates OUT refund');
  ctx.seed.saleReturn = {id, no};
}

export async function purchaseReturnFlow(ctx) {
  const no = `${ctx.prefix}_PURCHASE_RETURN`;
  const id = await createBill(ctx, 'purchase-return', no, ctx.seed.supplierId, 1, 10, 5);
  await submitAndPass(ctx, 'PURCHASE_RETURN', id, 'purchase return');
  assertAuditStatus(ctx, 'erp_bill', id, 1, 'purchase return audited after approval');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, 8, 'purchase return decreases stock');
  assertStockFlow(ctx, id, no, 'PURCHASE_RETURN', 'OUT', 1, 'purchase return creates OUT stock flow');
  assertPartnerFlow(ctx, id, no, 'PURCHASE_RETURN', 'PAY', 10, 'purchase return offsets payable');
  assertFundFlow(ctx, id, no, 'PURCHASE_RETURN', 'IN', 5, 'purchase return paid amount creates IN refund');
  ctx.seed.purchaseReturn = {id, no};
}

export async function unauditBillFlow(ctx) {
  const no = `${ctx.prefix}_SALE_UNAUDIT`;
  const id = await createBill(ctx, 'sale', no, ctx.seed.customerId, 1, 20, 20);
  await submitAndPass(ctx, 'SALE', id, 'sale unaudit');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, 7, 'sale unaudit setup stock decreased');
  await ctx.expectOk('PUT', `/erp/sale/unaudit/${id}`, null, 'sale unaudit rollback');
  assertAuditStatus(ctx, 'erp_bill', id, 0, 'sale unaudit resets audit status');
  assertStockQty(ctx, ctx.seed.productId, ctx.seed.warehouseId, 8, 'sale unaudit restores stock');
  assertNoSourceFlows(ctx, id, no, 'sale unaudit removes source flows');
}
