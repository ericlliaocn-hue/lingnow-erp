import {findTodoTask, rejectApproval, revokeApproval, submitApproval, transferApproval} from '../approval.mjs';
import {createBill, loginAdmin} from '../seed.mjs';

export async function approvalCompositeFlow(ctx) {
  const rejectNo = `${ctx.prefix}_APPROVAL_REJECT`;
  const rejectId = await createBill(ctx, 'sale', rejectNo, ctx.seed.customerId, 1, 20, 0);
  await rejectApproval(ctx, 'SALE', rejectId, 'approval reject');
  const rejectStatus = ctx.rows(`SELECT audit_status, approval_status FROM erp_bill WHERE id=${rejectId}`)[0];
  ctx.ok(Number(rejectStatus?.[0]) === 0, 'reject does not audit bill', {rejectStatus});

  const revokeNo = `${ctx.prefix}_APPROVAL_REVOKE`;
  const revokeId = await createBill(ctx, 'sale', revokeNo, ctx.seed.customerId, 1, 20, 0);
  await revokeApproval(ctx, 'SALE', revokeId, 'approval revoke');
  const revokeOpenTasks = Number(ctx.scalar(`
    SELECT COUNT(*)
    FROM flow_task ft
    JOIN flow_instance fi ON fi.id = ft.instance_id
    WHERE fi.business_id='${ctx.escapeSql(String(revokeId))}'
  `));
  ctx.ok(revokeOpenTasks === 0, 'revoke removes open approval task', {revokeOpenTasks});

  const transferNo = `${ctx.prefix}_APPROVAL_TRANSFER`;
  const transferId = await createBill(ctx, 'sale', transferNo, ctx.seed.customerId, 1, 20, 0);
  await transferApproval(ctx, 'SALE', transferId, 'approval transfer', ctx.seed.transferUser.id);

  const limitedToken = await loginAdmin(ctx, ctx.seed.limitedUser.username, ctx.seed.limitedUser.password);
  const deniedNo = `${ctx.prefix}_APPROVAL_DENIED`;
  const deniedId = await createBill(ctx, 'sale', deniedNo, ctx.seed.customerId, 1, 20, 0);
  await submitApproval(ctx, 'SALE', deniedId, 'approval permission denied');
  const deniedTask = await findTodoTask(ctx, 'SALE', deniedId);
  ctx.ok(Boolean(deniedTask?.taskId), 'permission denied task exists', {deniedTask});
  await ctx.expectBusinessFail('POST', '/erp/approval/pass', {
    taskId: deniedTask.taskId,
    comment: 'limited user should not approve'
  }, 'limited user cannot approve', limitedToken);

  const mine = await ctx.expectOk('GET', '/erp/approval/mine/list?current=1&size=20', null, 'approval mine list');
  ctx.ok(Array.isArray(mine.records), 'approval mine returns page records');
  const done = await ctx.expectOk('GET', '/erp/approval/done/list?current=1&size=20', null, 'approval done list');
  ctx.ok(Array.isArray(done.records), 'approval done returns page records');
}
