export async function notificationFlow(ctx) {
  const unreadBefore = await ctx.expectOk('GET', '/system/notification/unread-count', null, 'notification unread count');
  ctx.ok(typeof unreadBefore === 'number' || typeof unreadBefore?.count === 'number', 'unread count returns number-like payload', {unreadBefore});

  const list = await ctx.expectOk('GET', '/system/notification/list?current=1&size=50', null, 'notification list');
  ctx.ok(Array.isArray(list.records), 'notification list returns page records');
  const related = list.records.find((item) =>
    (String(item.category || '') === 'ORDER' && String(item.bizId) === String(ctx.seed.sale.id))
    || (String(item.category || '') === 'ORDER' && String(item.content || '').includes(ctx.seed.sale.no))
    || String(item.actionUrl || '').includes(ctx.seed.sale.no)
  );
  ctx.ok(Boolean(related), 'new sale notification is visible in notification center', {saleNo: ctx.seed.sale.no});
  if (related?.id) {
    await ctx.expectOk('PUT', `/system/notification/${related.id}/read`, null, 'mark notification read');
  }
  await ctx.expectOk('PUT', '/system/notification/read-all', null, 'mark all notifications read');

  const approvalNoticeCount = Number(ctx.scalar(`
    SELECT COUNT(*)
    FROM sys_user_notification
    WHERE title LIKE '%审批%'
      AND content LIKE '%${ctx.escapeSql(ctx.prefix)}%'
      AND del_flag=0
  `));
  ctx.ok(approvalNoticeCount > 0, 'approval notifications persisted', {approvalNoticeCount});

  const saleNotice = ctx.rows(`
    SELECT category, action_url
    FROM sys_user_notification
    WHERE biz_id=${ctx.seed.sale.id}
      AND biz_type='SALE'
      AND category='ORDER'
      AND del_flag=0
    ORDER BY create_time DESC
    LIMIT 1
  `)[0];
  ctx.ok(Boolean(saleNotice), 'sale notification DB row exists', {saleNotice});
  if (saleNotice) {
    ctx.ok(String(saleNotice[1] || '').includes('/erp/sale'), 'sale notification action url points to sale page', {actionUrl: saleNotice[1]});
  }
}
