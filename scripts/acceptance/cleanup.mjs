export function cleanupAcceptanceSql(ctx) {
  const p = ctx.escapeSql(ctx.prefix);
  ctx.mysql(`
    SET @p = '${p}%';
    SET @instances = (
      SELECT GROUP_CONCAT(approval_instance_id)
      FROM (
        SELECT approval_instance_id FROM erp_bill WHERE bill_no LIKE @p AND approval_instance_id IS NOT NULL
        UNION ALL
        SELECT approval_instance_id FROM erp_finance_bill WHERE bill_no LIKE @p AND approval_instance_id IS NOT NULL
        UNION ALL
        SELECT approval_instance_id FROM erp_stock_check WHERE check_no LIKE @p AND approval_instance_id IS NOT NULL
      ) t
    );
    DELETE fu FROM flow_user fu JOIN flow_task ft ON fu.associated = ft.id WHERE FIND_IN_SET(ft.instance_id, IFNULL(@instances, ''));
    DELETE FROM flow_his_task WHERE FIND_IN_SET(instance_id, IFNULL(@instances, ''));
    DELETE FROM flow_task WHERE FIND_IN_SET(instance_id, IFNULL(@instances, ''));
    DELETE FROM flow_instance WHERE FIND_IN_SET(id, IFNULL(@instances, ''));

    DELETE FROM sys_user_notification WHERE (title LIKE @p OR content LIKE CONCAT('%', '${p}', '%') OR biz_type LIKE @p OR action_url LIKE CONCAT('%', '${p}', '%'));
    DELETE FROM sys_job_log WHERE job_name LIKE @p OR invoke_target LIKE @p OR job_group LIKE @p;
    DELETE FROM sys_job WHERE job_name LIKE @p OR job_group LIKE @p;

    DELETE FROM erp_fund_flow WHERE source_bill_no LIKE @p;
    DELETE FROM erp_partner_flow WHERE source_bill_no LIKE @p;
    DELETE FROM erp_stock_flow WHERE source_bill_no LIKE @p;
    DELETE FROM erp_stock_check_item WHERE check_id IN (SELECT id FROM erp_stock_check WHERE check_no LIKE @p);
    DELETE FROM erp_stock_check WHERE check_no LIKE @p;
    DELETE FROM erp_bill_item WHERE bill_id IN (SELECT id FROM erp_bill WHERE bill_no LIKE @p);
    DELETE FROM erp_bill WHERE bill_no LIKE @p;
    DELETE FROM erp_finance_bill WHERE bill_no LIKE @p;
    DELETE FROM erp_stock_balance WHERE product_id IN (SELECT id FROM erp_product WHERE code LIKE @p);
    DELETE FROM erp_product WHERE code LIKE @p;
    DELETE FROM erp_customer WHERE code LIKE @p;
    DELETE FROM erp_supplier WHERE code LIKE @p;
    DELETE FROM erp_warehouse WHERE code LIKE @p;
    DELETE FROM erp_account WHERE code LIKE @p;
    DELETE FROM erp_product_brand WHERE code LIKE @p;
    DELETE FROM erp_unit WHERE code LIKE @p;
    DELETE FROM erp_product_category WHERE code LIKE @p;

    DELETE FROM erp_data_auth WHERE remark LIKE @p OR user_id IN (SELECT user_id FROM sys_user WHERE username LIKE @p);
    DELETE FROM erp_agent_level WHERE code LIKE @p;
    DELETE FROM erp_product_attribute WHERE code LIKE @p;

    DELETE FROM erp_bill_no_rule WHERE bill_type LIKE @p OR prefix LIKE @p;
    DELETE FROM erp_field_setting WHERE module_code LIKE @p OR field_key LIKE @p;
    DELETE FROM erp_print_template WHERE template_code LIKE @p;

    DELETE sur FROM sys_user_role sur JOIN sys_user su ON su.user_id = sur.user_id WHERE su.username LIKE @p;
    DELETE srm FROM sys_role_menu srm JOIN sys_role sr ON sr.role_id = srm.role_id WHERE sr.role_key LIKE @p;
    DELETE FROM sys_user WHERE username LIKE @p;
    DELETE FROM sys_role WHERE role_key LIKE @p;

    DELETE FROM app_user_info WHERE user_id IN (SELECT user_id FROM app_user WHERE username LIKE @p);
    DELETE FROM app_user WHERE username LIKE @p;
  `);
}

export async function cleanupRuntime(ctx) {
  for (const item of [...ctx.created.financeBills].reverse()) {
    await ctx.adminRequest('PUT', `/erp/finance/${item.module}/unaudit/${item.id}`, null).catch(() => null);
    await ctx.adminRequest('DELETE', `/erp/finance/${item.module}/${item.id}`, null).catch(() => null);
  }
  ctx.created.financeBills = [];

  for (const item of [...ctx.created.bills].reverse()) {
    await ctx.adminRequest('PUT', `/erp/${item.module}/unaudit/${item.id}`, null).catch(() => null);
    await ctx.adminRequest('DELETE', `/erp/${item.module}/${item.id}`, null).catch(() => null);
  }
  ctx.created.bills = [];

  for (const item of [...ctx.created.stockChecks].reverse()) {
    await ctx.adminRequest('PUT', `/erp/stock/check/unaudit/${item.id}`, null).catch(() => null);
    await ctx.adminRequest('DELETE', `/erp/stock/check/${item.id}`, null).catch(() => null);
  }
  ctx.created.stockChecks = [];

  for (const id of [...ctx.created.jobs].reverse()) {
    await ctx.adminRequest('DELETE', `/monitor/job/${id}`, null).catch(() => null);
  }
  ctx.created.jobs = [];

  cleanupAcceptanceSql(ctx);
}

export function assertClean(ctx) {
  cleanupAcceptanceSql(ctx);
  const p = ctx.escapeSql(ctx.prefix);
  const count = Number(ctx.scalar(`
    SELECT
      (SELECT COUNT(*) FROM erp_product WHERE code LIKE '${p}%') +
      (SELECT COUNT(*) FROM erp_customer WHERE code LIKE '${p}%') +
      (SELECT COUNT(*) FROM erp_supplier WHERE code LIKE '${p}%') +
      (SELECT COUNT(*) FROM erp_warehouse WHERE code LIKE '${p}%') +
      (SELECT COUNT(*) FROM erp_account WHERE code LIKE '${p}%') +
      (SELECT COUNT(*) FROM erp_bill WHERE bill_no LIKE '${p}%') +
      (SELECT COUNT(*) FROM erp_finance_bill WHERE bill_no LIKE '${p}%') +
      (SELECT COUNT(*) FROM erp_stock_check WHERE check_no LIKE '${p}%') +
      (SELECT COUNT(*) FROM sys_user WHERE username LIKE '${p}%') +
      (SELECT COUNT(*) FROM sys_role WHERE role_key LIKE '${p}%') +
      (SELECT COUNT(*) FROM sys_job WHERE job_name LIKE '${p}%') +
      (SELECT COUNT(*) FROM sys_user_notification WHERE title LIKE '${p}%' OR content LIKE '%${p}%') +
      (SELECT COUNT(*) FROM app_user WHERE username LIKE '${p}%')
  `));
  ctx.ok(count === 0, `ACCEPT temporary rows cleaned to 0 (${count})`, {expected: 0, actual: count});
}
