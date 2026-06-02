import {loginAdmin} from '../seed.mjs';

export async function permissionFlow(ctx) {
  const menuCount = Number(ctx.scalar('SELECT COUNT(*) FROM sys_menu WHERE del_flag=0'));
  const missingSuperBindings = Number(ctx.scalar(`
    SELECT COUNT(*)
    FROM sys_menu m
    LEFT JOIN sys_role_menu rm ON rm.menu_id=m.menu_id AND rm.role_id=1
    WHERE m.del_flag=0 AND rm.menu_id IS NULL
  `));
  ctx.ok(menuCount >= 200, `active menu/button count is complete (${menuCount})`, {menuCount});
  ctx.ok(missingSuperBindings === 0, 'super admin has every active menu/button binding', {missingSuperBindings});

  const limitedToken = await loginAdmin(ctx, ctx.seed.limitedUser.username, ctx.seed.limitedUser.password);
  const saleList = await ctx.adminRequest('GET', '/erp/sale/list?current=1&size=10', null, limitedToken);
  ctx.ok(saleList.status === 200 && saleList.json?.code === 200, 'limited user can access assigned sale list', {
    status: saleList.status,
    body: saleList.data
  });
  await ctx.expectBusinessFail('POST', '/erp/sale', {
    billNo: `${ctx.prefix}_LIMITED_SALE`,
    billDate: ctx.today(),
    partnerId: ctx.seed.customerId,
    warehouseId: ctx.seed.warehouseId,
    accountId: ctx.seed.accountId,
    paidAmount: 0,
    items: [{productId: ctx.seed.productId, warehouseId: ctx.seed.warehouseId, qty: 1, price: 20}]
  }, 'limited user cannot add sale without button permission', limitedToken);
  await ctx.expectBusinessFail('GET', '/erp/purchase/list?current=1&size=10', null, 'limited user cannot access unassigned purchase list', limitedToken);
}

export async function dataAuthorizationFlow(ctx) {
  const limitedToken = await loginAdmin(ctx, ctx.seed.limitedUser.username, ctx.seed.limitedUser.password);
  const allowedNo = `${ctx.prefix}_AUTH_ALLOWED`;
  const deniedNo = `${ctx.prefix}_AUTH_DENIED`;
  await ctx.expectOk('POST', '/erp/sale', {
    billNo: allowedNo,
    billDate: ctx.today(),
    partnerId: ctx.seed.secondCustomerId,
    warehouseId: ctx.seed.secondWarehouseId,
    accountId: ctx.seed.accountId,
    paidAmount: 0,
    items: [{productId: ctx.seed.productId, warehouseId: ctx.seed.secondWarehouseId, qty: 1, price: 20}]
  }, 'create authorized sale for data auth');
  const allowedId = ctx.billId(allowedNo);
  ctx.created.bills.push({module: 'sale', id: allowedId});
  await ctx.expectOk('POST', '/erp/sale', {
    billNo: deniedNo,
    billDate: ctx.today(),
    partnerId: ctx.seed.customerId,
    warehouseId: ctx.seed.warehouseId,
    accountId: ctx.seed.accountId,
    paidAmount: 0,
    items: [{productId: ctx.seed.productId, warehouseId: ctx.seed.warehouseId, qty: 1, price: 20}]
  }, 'create unauthorized sale for data auth');
  const deniedId = ctx.billId(deniedNo);
  ctx.created.bills.push({module: 'sale', id: deniedId});

  const dataScopeOnly = Number(ctx.scalar(`
    SELECT COUNT(*)
    FROM sys_role r
    JOIN sys_user_role ur ON ur.role_id = r.role_id
    WHERE ur.user_id=${ctx.seed.limitedUser.id}
      AND r.data_scope IS NOT NULL
  `));
  ctx.ok(dataScopeOnly > 0, 'limited role has data scope metadata');
  const hasDedicatedAuthTables = Number(ctx.scalar(`
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema='${ctx.escapeSql(ctx.dbName)}'
      AND table_name IN ('sys_user_data_auth', 'erp_data_auth', 'sys_role_data_scope')
  `));
  ctx.ok(hasDedicatedAuthTables > 0, 'customer/warehouse data authorization tables exist for isolation', {
    expected: '>0',
    actual: hasDedicatedAuthTables,
    note: '当前失败代表客户/仓库数据授权隔离还没真正落库实现'
  });
  const authRows = Number(ctx.scalar(`
    SELECT COUNT(*)
    FROM erp_data_auth
    WHERE user_id=${ctx.seed.limitedUser.id}
      AND del_flag=0
  `));
  ctx.ok(authRows >= 2, 'limited user has customer and warehouse auth rows', {authRows});

  const saleList = await ctx.adminRequest('GET', '/erp/sale/list?current=1&size=100', null, limitedToken);
  ctx.ok(saleList.status === 200 && saleList.json?.code === 200, 'data authorization precondition: limited user list works');
  const listText = JSON.stringify(saleList.json?.data || {});
  ctx.ok(listText.includes(allowedNo), 'limited user can see authorized sale bill', {allowedNo});
  ctx.ok(!listText.includes(deniedNo), 'limited user cannot see unauthorized sale bill', {deniedNo});

  const stockList = await ctx.adminRequest('GET', '/erp/stock/warning/list?current=1&size=100', null, limitedToken);
  ctx.ok(stockList.status === 200 && stockList.json?.code === 200, 'limited user stock list works with warehouse auth');
  const stockText = JSON.stringify(stockList.json?.data || {});
  ctx.ok(!stockText.includes(String(ctx.seed.warehouseId)), 'limited user stock list excludes unauthorized warehouse', {
    warehouseId: ctx.seed.warehouseId
  });
}
