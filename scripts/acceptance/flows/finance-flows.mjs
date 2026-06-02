import {assertAuditStatus, assertFundFlow, assertNoTypedSourceFlows, assertPartnerFlow} from '../assertions.mjs';
import {submitAndPass} from '../approval.mjs';
import {createFinanceBill} from '../seed.mjs';

export async function receiptFlow(ctx) {
  const no = `${ctx.prefix}_RECEIPT`;
  const id = await createFinanceBill(ctx, 'receipt', no, ctx.seed.customerId, 15);
  await submitAndPass(ctx, 'RECEIPT', id, 'receipt');
  assertAuditStatus(ctx, 'erp_finance_bill', id, 1, 'receipt audited after approval');
  assertFundFlow(ctx, id, no, 'RECEIPT', 'IN', 15, 'receipt creates IN fund flow');
  assertPartnerFlow(ctx, id, no, 'RECEIPT', 'RECEIVE', 15, 'receipt creates RECEIVE partner flow');
  await ctx.expectOk('PUT', `/erp/finance/receipt/unaudit/${id}`, null, 'receipt unaudit rollback');
  assertAuditStatus(ctx, 'erp_finance_bill', id, 0, 'receipt unaudit resets audit status');
  assertNoTypedSourceFlows(ctx, id, 'RECEIPT', 'receipt unaudit removes flows');
}

export async function paymentFlow(ctx) {
  const no = `${ctx.prefix}_PAYMENT`;
  const id = await createFinanceBill(ctx, 'payment', no, ctx.seed.supplierId, 12);
  await submitAndPass(ctx, 'PAYMENT', id, 'payment');
  assertAuditStatus(ctx, 'erp_finance_bill', id, 1, 'payment audited after approval');
  assertFundFlow(ctx, id, no, 'PAYMENT', 'OUT', 12, 'payment creates OUT fund flow');
  assertPartnerFlow(ctx, id, no, 'PAYMENT', 'PAY', 12, 'payment creates PAY partner flow');
  await ctx.expectOk('PUT', `/erp/finance/payment/unaudit/${id}`, null, 'payment unaudit rollback');
  assertAuditStatus(ctx, 'erp_finance_bill', id, 0, 'payment unaudit resets audit status');
  assertNoTypedSourceFlows(ctx, id, 'PAYMENT', 'payment unaudit removes flows');
}
