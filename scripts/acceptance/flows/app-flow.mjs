import {loginApp} from '../seed.mjs';

export async function appFlow(ctx) {
  const noToken = await ctx.request(ctx.appBase, '/app/erp/dashboard');
  ctx.ok(noToken.status === 200 && noToken.json?.code === 401, 'app dashboard rejects missing token', {
    status: noToken.status,
    body: noToken.data
  });
  ctx.appToken = await loginApp(ctx, ctx.seed.appUser.phone, ctx.seed.appUser.password);
  await ctx.expectAppOk('GET', '/app/erp/dashboard', null, 'app dashboard');
  await ctx.expectAppOk('GET', '/app/erp/customers?current=1&size=10', null, 'app customers');
  await ctx.expectAppOk('GET', '/app/erp/suppliers?current=1&size=10', null, 'app suppliers');
  await ctx.expectAppOk('GET', '/app/erp/products?current=1&size=10', null, 'app products');
  await ctx.expectAppOk('GET', `/app/erp/products/${ctx.seed.productId}`, null, 'app product detail');
  await ctx.expectAppOk('GET', '/app/erp/bills?current=1&size=10', null, 'app bills');
  const parsed = await ctx.expectAppOk('POST', '/app/erp/address/parse', {
    rawText: '张三 13800138000 广东省深圳市南山区科技园1号'
  }, 'app address parse');
  ctx.ok(parsed?.phone === '13800138000' && String(parsed?.normalizedAddress || parsed?.detailAddress || '').includes('深圳'), 'app address parse returns phone and address', {parsed});
}
