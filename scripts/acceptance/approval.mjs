export async function submitApproval(ctx, bizType, bizId, label, token = ctx.token) {
  await ctx.expectOk('POST', '/erp/approval/submit', {
    bizType,
    bizId,
    comment: `${ctx.prefix} ${label} submit`
  }, `${label} submit approval`, token);
}

export async function findTodoTask(ctx, bizType, bizId, token = ctx.token) {
  const page = await ctx.expectOk('GET', `/erp/approval/todo/list?current=1&size=50&bizType=${encodeURIComponent(bizType)}`, null, `query ${bizType} todo`, token);
  return page.records?.find((item) => String(item.bizId) === String(bizId) && item.bizType === bizType);
}

export async function submitAndPass(ctx, bizType, bizId, label, token = ctx.token) {
  await submitApproval(ctx, bizType, bizId, label, token);
  await assertApprovalAssignees(ctx, bizType, bizId, label);
  const task = await findTodoTask(ctx, bizType, bizId, token);
  ctx.ok(Boolean(task?.taskId), `${label} creates approval todo`, {bizType, bizId, task});
  await ctx.expectOk('POST', '/erp/approval/pass', {
    taskId: task.taskId,
    comment: `${ctx.prefix} ${label} pass`
  }, `${label} pass approval`, token);
  await assertApprovalHistory(ctx, bizType, bizId, label);
}

export async function rejectApproval(ctx, bizType, bizId, label) {
  await submitApproval(ctx, bizType, bizId, label);
  const task = await findTodoTask(ctx, bizType, bizId);
  ctx.ok(Boolean(task?.taskId), `${label} reject task exists`, {task});
  await ctx.expectOk('POST', '/erp/approval/reject', {
    taskId: task.taskId,
    comment: `${ctx.prefix} ${label} reject`
  }, `${label} reject approval`);
  await assertApprovalHistory(ctx, bizType, bizId, label);
}

export async function revokeApproval(ctx, bizType, bizId, label) {
  await submitApproval(ctx, bizType, bizId, label);
  await ctx.expectOk('POST', '/erp/approval/revoke', {
    bizType,
    bizId,
    comment: `${ctx.prefix} ${label} revoke`
  }, `${label} revoke approval`);
}

export async function transferApproval(ctx, bizType, bizId, label, transferUserId) {
  await submitApproval(ctx, bizType, bizId, label);
  const task = await findTodoTask(ctx, bizType, bizId);
  ctx.ok(Boolean(task?.taskId), `${label} transfer task exists`, {task});
  await ctx.expectOk('POST', '/erp/approval/transfer', {
    taskId: task.taskId,
    transferUserId,
    comment: `${ctx.prefix} ${label} transfer`
  }, `${label} transfer approval`);
  const taskUserCount = Number(ctx.scalar(`
    SELECT COUNT(*)
    FROM flow_user fu
    JOIN flow_task ft ON ft.id = fu.associated
    JOIN flow_instance fi ON fi.id = ft.instance_id
    WHERE fi.business_id LIKE '%:${ctx.escapeSql(String(bizId))}'
      AND fu.processed_by='${ctx.escapeSql(String(transferUserId))}'
  `));
  ctx.ok(taskUserCount > 0, `${label} transfer user becomes approver`, {transferUserId, taskUserCount});
}

export async function assertApprovalAssignees(ctx, bizType, bizId, label) {
  const rows = ctx.rows(`
    SELECT DISTINCT fu.processed_by
    FROM flow_user fu
    JOIN flow_task ft ON ft.id = fu.associated
    JOIN flow_instance fi ON fi.id = ft.instance_id
    WHERE fi.business_id LIKE '%:${ctx.escapeSql(String(bizId))}'
  `).map((row) => row[0]);
  ctx.ok(rows.includes('1'), `${label} approval includes admin approver`, {approvers: rows});
  ctx.ok(rows.includes(String(ctx.seed.approver.id)), `${label} approval includes permission approver`, {approvers: rows, expected: ctx.seed.approver.id});
  ctx.ok(!rows.includes(String(ctx.seed.limitedUser.id)), `${label} excludes limited user from approvers`, {approvers: rows, limited: ctx.seed.limitedUser.id});
}

export async function assertApprovalHistory(ctx, bizType, bizId, label) {
  const history = await ctx.expectOk('GET', `/erp/approval/history?bizType=${encodeURIComponent(bizType)}&bizId=${bizId}`, null, `${label} approval history`);
  ctx.ok(Array.isArray(history) && history.length > 0, `${label} approval history has rows`, {historyLength: history?.length});
}
