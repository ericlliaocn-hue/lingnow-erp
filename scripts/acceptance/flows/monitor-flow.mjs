export async function monitorFlow(ctx) {
  await ctx.expectOk('GET', '/monitor/admin/dashboard', null, 'service monitor dashboard');
  await ctx.expectOk('GET', '/monitor/cache', null, 'cache monitor');
  await ctx.expectOk('GET', '/monitor/online/list', null, 'online user monitor');

  const jobName = `${ctx.prefix}_JOB`;
  const jobGroup = `${ctx.prefix}_GROUP`;
  await ctx.expectOk('POST', '/monitor/job', {
    jobName,
    jobGroup,
    invokeTarget: 'sysJobManager.scheduleEnabledJobs',
    cronExpression: '0 0/5 * * * ?',
    misfirePolicy: 'DO_NOTHING',
    concurrent: 'N',
    status: 0,
    remark: `${ctx.prefix} acceptance job`
  }, 'create paused monitor job');
  const jobId = ctx.scalar(`SELECT job_id FROM sys_job WHERE job_name='${ctx.escapeSql(jobName)}' ORDER BY create_time DESC LIMIT 1`);
  ctx.created.jobs.push(jobId);
  ctx.ok(Boolean(jobId), 'created job id resolved', {jobId});
  await ctx.expectOk('GET', `/monitor/job/${jobId}`, null, 'job detail');
  await ctx.expectOk('PUT', '/monitor/job/changeStatus', {jobId, status: 1}, 'enable job');
  await ctx.expectOk('PUT', '/monitor/job/changeStatus', {jobId, status: 0}, 'pause job');
  await ctx.expectOk('POST', `/monitor/job/run/${jobId}`, null, 'run job once');
  const logCount = Number(ctx.scalar(`SELECT COUNT(*) FROM sys_job_log WHERE job_id='${ctx.escapeSql(jobId)}'`));
  ctx.ok(logCount > 0, 'job run writes job log', {logCount, jobId});
  await ctx.expectOk('GET', `/monitor/job/log/list?current=1&size=10&jobName=${encodeURIComponent(jobName)}`, null, 'job log list');
}
