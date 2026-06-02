export async function loginAdmin(ctx, username = ctx.adminUsername, password = ctx.adminPassword) {
  const res = await ctx.request(ctx.adminBase, '/auth/login', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({username, password})
  });
  ctx.ok(res.status === 200 && res.json?.code === 200 && res.json?.data?.token, `admin login ${username}`, {
    status: res.status,
    body: res.data
  });
  if (!(res.status === 200 && res.json?.code === 200 && res.json?.data?.token)) {
    throw new Error(`admin login failed for ${username}: ${res.status} ${res.data}`);
  }
  return res.json.data.token;
}

export async function loginApp(ctx, phone, password) {
  const res = await ctx.request(ctx.appBase, '/app/auth/login', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({account: phone, credential: password, type: 'password'})
  });
  ctx.ok(res.status === 200 && res.json?.code === 200 && res.json?.data?.token, `app login ${phone}`, {
    status: res.status,
    body: res.data
  });
  if (!(res.status === 200 && res.json?.code === 200 && res.json?.data?.token)) {
    throw new Error(`app login failed for ${phone}: ${res.status} ${res.data}`);
  }
  return res.json.data.token;
}

export async function seedAll(ctx) {
  await seedMasterData(ctx);
  seedAdminUsers(ctx);
  seedDataAuth(ctx);
  seedAppUser(ctx);
}

export async function seedMasterData(ctx) {
  const p = ctx.prefix;
  const ids = {};
  ids.categoryId = await addMaster(ctx, 'product-category', `${p}_CAT`, `${p} 分类`);
  ids.unitId = await addMaster(ctx, 'unit', `${p}_UNIT`, `${p} 单位`);
  ids.brandId = await addMaster(ctx, 'product-brand', `${p}_BRAND`, `${p} 品牌`);
  ids.customerId = await addMaster(ctx, 'customer', `${p}_CUS`, `${p} 客户`, {
    contact: `${p} 客户联系人`,
    phone: '13000000000',
    address: '广东省深圳市南山区科技园'
  });
  ids.secondCustomerId = await addMaster(ctx, 'customer', `${p}_CUS2`, `${p} 授权客户`, {
    contact: `${p} 授权联系人`,
    phone: '13000000001',
    address: '广东省广州市天河区'
  });
  ids.supplierId = await addMaster(ctx, 'supplier', `${p}_SUP`, `${p} 供应商`, {
    contact: `${p} 供应商联系人`,
    phone: '13100000000',
    address: '浙江省杭州市西湖区'
  });
  ids.warehouseId = await addMaster(ctx, 'warehouse', `${p}_WH`, `${p} 仓库`);
  ids.secondWarehouseId = await addMaster(ctx, 'warehouse', `${p}_WH2`, `${p} 授权仓库`);
  ids.accountId = await addMaster(ctx, 'account', `${p}_ACC`, `${p} 现金账户`, {
    accountType: 'CASH',
    openingBalance: 0
  });
  ids.productId = await addProduct(ctx, ids, `${p}_PRODUCT`, `${p} 商品`, 10, 20);
  ids.secondProductId = await addProduct(ctx, ids, `${p}_PRODUCT2`, `${p} 商品二`, 8, 18);
  ctx.seed = {...ctx.seed, ...ids};
}

export async function addMaster(ctx, type, code, name, extra = {}) {
  await ctx.expectOk('POST', `/erp/master/${type}`, {
    code,
    name,
    status: 1,
    sortOrder: 999,
    remark: `${ctx.prefix} acceptance temporary data`,
    ...extra
  }, `create master ${type} ${code}`);
  const table = {
    'product-category': 'erp_product_category',
    unit: 'erp_unit',
    'product-brand': 'erp_product_brand',
    'product-attribute': 'erp_product_attribute',
    customer: 'erp_customer',
    supplier: 'erp_supplier',
    warehouse: 'erp_warehouse',
    account: 'erp_account',
    'agent-level': 'erp_agent_level'
  }[type];
  const id = ctx.masterId(table, code);
  ctx.ok(Boolean(id), `created ${type} id resolved`, {code, id});
  ctx.created.masters.push({type, id});
  return id;
}

export async function addProduct(ctx, ids, code, name, purchasePrice = 10, salePrice = 20) {
  await ctx.expectOk('POST', '/erp/product', {
    code,
    name,
    spec: '验收规格',
    categoryId: ids.categoryId,
    brandId: ids.brandId,
    unitId: ids.unitId,
    purchasePrice,
    salePrice,
    retailPrice: salePrice,
    minStock: 1,
    maxStock: 999,
    status: 1,
    remark: `${ctx.prefix} acceptance temporary data`
  }, `create product ${code}`);
  const id = ctx.latestId('erp_product', 'code', code);
  ctx.ok(Boolean(id), `created product id resolved`, {code, id});
  ctx.created.products.push(id);
  return id;
}

export function seedAdminUsers(ctx) {
  const p = ctx.escapeSql(ctx.prefix);
  const hash = ctx.scalar("SELECT password FROM sys_user WHERE username='admin' LIMIT 1");
  const base = Date.now();
  const approverId = base + 11;
  const transferId = base + 12;
  const limitedId = base + 13;
  const approverRoleId = base + 21;
  const limitedRoleId = base + 22;
  const approverUsername = `${ctx.prefix}_APPROVER`;
  const transferUsername = `${ctx.prefix}_TRANSFER`;
  const limitedUsername = `${ctx.prefix}_LIMITED`;

  ctx.mysql(`
    INSERT INTO sys_role (role_id, role_name, role_key, sort_order, status, data_scope, remark, create_time, del_flag)
    VALUES
      (${approverRoleId}, '${p}审批角色', '${p}_approval_role', 900, 1, 1, '${p} temporary role', NOW(), 0),
      (${limitedRoleId}, '${p}受限角色', '${p}_limited_role', 901, 1, 1, '${p} temporary role', NOW(), 0);

    INSERT INTO sys_role_menu (role_id, menu_id)
    SELECT ${approverRoleId}, menu_id FROM sys_menu
    WHERE del_flag=0 AND permission IN (
      'erp:approval:submit', 'erp:approval:approve', 'erp:approval:reject', 'erp:approval:revoke',
      'erp:approval:transfer', 'erp:approval:task',
      'erp:sale:list', 'erp:sale:add', 'erp:sale:audit',
      'erp:sale-return:list', 'erp:sale-return:add', 'erp:sale-return:audit',
      'erp:purchase:list', 'erp:purchase:add', 'erp:purchase:audit',
      'erp:purchase-return:list', 'erp:purchase-return:add', 'erp:purchase-return:audit',
      'erp:stock-check:list', 'erp:stock-check:add', 'erp:stock-check:audit',
      'erp:finance:receipt:list', 'erp:finance:receipt:add', 'erp:finance:receipt:audit',
      'erp:finance:payment:list', 'erp:finance:payment:add', 'erp:finance:payment:audit'
    );

    INSERT INTO sys_role_menu (role_id, menu_id)
    SELECT ${limitedRoleId}, menu_id FROM sys_menu
    WHERE del_flag=0 AND permission IN ('erp:sale:list', 'erp:stock:warning');

    INSERT INTO sys_user (user_id, username, password, nickname, status, create_time, del_flag)
    VALUES
      (${approverId}, '${ctx.escapeSql(approverUsername)}', '${ctx.escapeSql(hash)}', '${p}审批人', 1, NOW(), 0),
      (${transferId}, '${ctx.escapeSql(transferUsername)}', '${ctx.escapeSql(hash)}', '${p}转交人', 1, NOW(), 0),
      (${limitedId}, '${ctx.escapeSql(limitedUsername)}', '${ctx.escapeSql(hash)}', '${p}受限用户', 1, NOW(), 0);

    INSERT INTO sys_user_role (user_id, role_id)
    VALUES
      (${approverId}, ${approverRoleId}),
      (${transferId}, ${approverRoleId}),
      (${limitedId}, ${limitedRoleId});
  `);

  ctx.created.roles.push(approverRoleId, limitedRoleId);
  ctx.created.users.push(approverId, transferId, limitedId);
  ctx.seed.limitedUserId = limitedId;
  ctx.seed.approver = {id: approverId, username: approverUsername, password: ctx.adminPassword};
  ctx.seed.transferUser = {id: transferId, username: transferUsername, password: ctx.adminPassword};
  ctx.seed.limitedUser = {id: limitedId, username: limitedUsername, password: ctx.adminPassword};
}

export function seedDataAuth(ctx) {
  const base = Date.now() + 41;
  const remark = `${ctx.escapeSql(ctx.prefix)} data auth`;
  ctx.mysql(`
    INSERT INTO erp_data_auth (id, user_id, resource_type, resource_id, remark, create_time, del_flag)
    VALUES
      (${base}, ${ctx.seed.limitedUser.id}, 'CUSTOMER', ${ctx.seed.secondCustomerId}, '${remark}', NOW(), 0),
      (${base + 1}, ${ctx.seed.limitedUser.id}, 'WAREHOUSE', ${ctx.seed.secondWarehouseId}, '${remark}', NOW(), 0);
  `);
}

export function seedAppUser(ctx) {
  const p = ctx.escapeSql(ctx.prefix);
  const hash = ctx.scalar("SELECT password FROM sys_user WHERE username='admin' LIMIT 1");
  const userId = Date.now() + 31;
  const phone = `199${String(userId).slice(-8)}`;
  const username = `${ctx.prefix}_APP`;
  ctx.mysql(`
    INSERT INTO app_user (user_id, username, password, phone, nickname, status, create_time, del_flag)
    VALUES (${userId}, '${ctx.escapeSql(username)}', '${ctx.escapeSql(hash)}', '${phone}', '${p}App用户', 1, NOW(), 0);
    INSERT INTO app_user_info (user_id, create_time, del_flag)
    VALUES (${userId}, NOW(), 0);
  `);
  ctx.created.appUsers.push(userId);
  ctx.seed.appUser = {id: userId, username, phone, password: ctx.adminPassword};
}

export async function createBill(ctx, module, billNo, partnerId, qty, price, paidAmount = 0, extra = {}) {
  const body = {
    billNo,
    billDate: ctx.today(),
    partnerId,
    warehouseId: ctx.seed.warehouseId,
    accountId: ctx.seed.accountId,
    paidAmount,
    receiverName: `${ctx.prefix} 收货人`,
    receiverPhone: '13200000000',
    receiverAddress: '广东省深圳市南山区科技园验收地址',
    items: [{productId: ctx.seed.productId, warehouseId: ctx.seed.warehouseId, qty, price}],
    ...extra
  };
  await ctx.expectOk('POST', `/erp/${module}`, body, `create ${module} ${billNo}`);
  const id = ctx.billId(billNo);
  ctx.ok(Boolean(id), `${module} id resolved`, {billNo, id});
  ctx.created.bills.push({module, id});
  return id;
}

export async function createFinanceBill(ctx, module, billNo, partnerId, amount, extra = {}) {
  await ctx.expectOk('POST', `/erp/finance/${module}`, {
    billNo,
    billDate: ctx.today(),
    partnerId,
    accountId: ctx.seed.accountId,
    amount,
    remark: `${ctx.prefix} ${module}`,
    ...extra
  }, `create finance ${module} ${billNo}`);
  const id = ctx.financeBillId(billNo);
  ctx.ok(Boolean(id), `finance ${module} id resolved`, {billNo, id});
  ctx.created.financeBills.push({module, id});
  return id;
}
