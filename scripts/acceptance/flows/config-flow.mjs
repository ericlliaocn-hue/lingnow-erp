export async function configFlow(ctx) {
  const billType = `${ctx.prefix}_RULE`;
  await ctx.expectOk('POST', '/erp/config/bill-no-rule', {
    billType,
    billName: `${ctx.prefix} 单号规则`,
    prefix: `${ctx.prefix}_NO`,
    datePattern: 'yyyyMMdd',
    serialLength: 4,
    nextSerial: 1,
    resetCycle: 'DAY',
    enabled: 1,
    remark: `${ctx.prefix} config`
  }, 'create bill no rule');
  const ruleId = ctx.latestId('erp_bill_no_rule', 'bill_type', billType);
  ctx.created.configRows.push({table: 'erp_bill_no_rule', id: ruleId});
  await ctx.expectOk('PUT', '/erp/config/bill-no-rule', {
    id: ruleId,
    billType,
    billName: `${ctx.prefix} 单号规则已改`,
    prefix: `${ctx.prefix}_NX`,
    datePattern: 'yyyyMMdd',
    serialLength: 5,
    nextSerial: 2,
    resetCycle: 'DAY',
    enabled: 1
  }, 'edit bill no rule');

  const moduleCode = `${ctx.prefix}_MODULE`;
  const fieldKey = `${ctx.prefix}_FIELD`;
  await ctx.expectOk('POST', '/erp/config/field-setting', {
    moduleCode,
    fieldKey,
    fieldLabel: `${ctx.prefix} 字段`,
    visible: 1,
    required: 0,
    sortOrder: 1,
    width: 120
  }, 'create field setting');
  const fieldId = ctx.latestId('erp_field_setting', 'module_code', moduleCode);
  ctx.created.configRows.push({table: 'erp_field_setting', id: fieldId});
  await ctx.expectOk('PUT', '/erp/config/field-setting', {
    id: fieldId,
    moduleCode,
    fieldKey,
    fieldLabel: `${ctx.prefix} 字段已改`,
    visible: 1,
    required: 1,
    sortOrder: 2,
    width: 160
  }, 'edit field setting');

  const templateCode = `${ctx.prefix}_TPL`;
  await ctx.expectOk('POST', '/erp/config/print-template', {
    templateCode,
    templateName: `${ctx.prefix} 打印模板`,
    billType: 'SALE',
    paperType: 'A4',
    contentJson: JSON.stringify({fields: ['billNo', 'receiverName']}),
    isDefault: 0,
    status: 1
  }, 'create print template');
  const templateId = ctx.latestId('erp_print_template', 'template_code', templateCode);
  ctx.created.configRows.push({table: 'erp_print_template', id: templateId});
  await ctx.expectOk('PUT', '/erp/config/print-template', {
    id: templateId,
    templateCode,
    templateName: `${ctx.prefix} 打印模板已改`,
    billType: 'SALE',
    paperType: 'A5',
    contentJson: JSON.stringify({fields: ['billNo', 'receiverPhone']}),
    isDefault: 0,
    status: 1
  }, 'edit print template');

  await ctx.expectOk('GET', `/erp/config/bill-no-rule/${ruleId}`, null, 'bill no rule detail');
  await ctx.expectOk('GET', `/erp/config/field-setting/${fieldId}`, null, 'field setting detail');
  await ctx.expectOk('GET', `/erp/config/print-template/${templateId}`, null, 'print template detail');
}
