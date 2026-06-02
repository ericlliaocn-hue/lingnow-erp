import {addMaster} from '../seed.mjs';

export async function masterDataFlow(ctx) {
  const code = `${ctx.prefix}_TEMP_CUS`;
  const id = await addMaster(ctx, 'customer', code, `${ctx.prefix} 临时客户`, {
    contact: '原联系人',
    phone: '13300000000'
  });
  await ctx.expectOk('GET', `/erp/master/customer/${id}`, null, 'customer detail');
  await ctx.expectOk('PUT', '/erp/master/customer', {
    id,
    code,
    name: `${ctx.prefix} 临时客户已编辑`,
    contact: '新联系人',
    phone: '13300000001',
    status: 1,
    sortOrder: 998
  }, 'customer edit');
  const edited = ctx.rows(`SELECT name, contact, phone FROM erp_customer WHERE id=${id}`)[0];
  ctx.ok(edited?.[0] === `${ctx.prefix} 临时客户已编辑`, 'customer edit persisted', {actual: edited});

  const refNo = `${ctx.prefix}_MASTER_REF`;
  await ctx.expectOk('POST', '/erp/sale', {
    billNo: refNo,
    billDate: ctx.today(),
    partnerId: id,
    warehouseId: ctx.seed.warehouseId,
    accountId: ctx.seed.accountId,
    paidAmount: 0,
    items: [{productId: ctx.seed.productId, warehouseId: ctx.seed.warehouseId, qty: 1, price: 20}]
  }, 'create draft sale to reference temp customer');
  const refBillId = ctx.billId(refNo);
  ctx.created.bills.push({module: 'sale', id: refBillId});
  await ctx.expectBusinessFail('DELETE', `/erp/master/customer/${id}`, null, 'referenced customer delete is blocked');
  await ctx.expectOk('DELETE', `/erp/sale/${refBillId}`, null, 'delete draft reference sale');
  ctx.created.bills = ctx.created.bills.filter((item) => String(item.id) !== String(refBillId));
  await ctx.expectOk('DELETE', `/erp/master/customer/${id}`, null, 'unreferenced temp customer delete');
  ctx.created.masters = ctx.created.masters.filter((item) => String(item.id) !== String(id));
  const delFlag = Number(ctx.scalar(`SELECT del_flag FROM erp_customer WHERE id='${ctx.escapeSql(id)}'`));
  ctx.ok(delFlag === 1, 'deleted temp customer is logically removed', {delFlag});
}
