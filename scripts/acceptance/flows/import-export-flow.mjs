export async function importExportFlow(ctx) {
  const template = await ctx.request(ctx.adminBase, '/erp/product/import-template', {headers: {'token-admin': ctx.token}});
  ctx.ok(template.status === 200 && template.data.includes('商品编号') && template.data.includes('商品名称'), 'product import template CSV fields');

  const code = `${ctx.prefix}_IMPORT_PRODUCT`;
  const csv = [
    '商品编号,商品名称,规格,分类,品牌,单位,辅助属性,条码,货位,采购价,销售价,零售价,最低库存,最高库存,状态,备注',
    `${code},${ctx.prefix} 导入商品,导入规格,${ctx.prefix} 分类,${ctx.prefix} 品牌,${ctx.prefix} 单位,,,A1,6,16,16,1,100,启用,${ctx.prefix} import`
  ].join('\n');
  const boundary = `acceptance-${Date.now()}`;
  const body = Buffer.concat([
    Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="products.csv"\r\nContent-Type: text/csv\r\n\r\n`, 'utf8'),
    Buffer.from(csv, 'utf8'),
    Buffer.from(`\r\n--${boundary}--\r\n`, 'utf8')
  ]);
  const imported = await ctx.request(ctx.adminBase, '/erp/product/import', {
    method: 'POST',
    headers: {
      'token-admin': ctx.token,
      'Content-Type': `multipart/form-data; boundary=${boundary}`,
      'Content-Length': String(body.length)
    },
    body
  });
  ctx.ok(imported.status === 200 && imported.json?.code === 200 && Number(imported.json?.data?.success) === 1, 'product import succeeds', {
    status: imported.status,
    data: imported.data
  });
  const importedId = ctx.latestId('erp_product', 'code', code);
  ctx.created.products.push(importedId);
  ctx.ok(Boolean(importedId), 'imported product persisted', {code, importedId});

  for (const [path, label, needle] of [
    ['/erp/product/export?current=1&size=20', 'product export CSV', code],
    ['/erp/sale/export?current=1&size=20', 'sale export CSV', ctx.seed.sale?.no],
    ['/erp/sale-return/export?current=1&size=20', 'sale return export CSV', ctx.seed.saleReturn?.no],
    ['/erp/purchase/export?current=1&size=20', 'purchase export CSV', ctx.seed.purchase?.no],
    ['/erp/purchase-return/export?current=1&size=20', 'purchase return export CSV', ctx.seed.purchaseReturn?.no]
  ]) {
    const res = await ctx.request(ctx.adminBase, path, {headers: {'token-admin': ctx.token}});
    ctx.ok(res.status === 200 && res.data.includes(',') && (!needle || res.data.includes(needle)), label, {
      path,
      needle
    });
  }
}
