# ERP Manual Chrome Checklist

This checklist is the remaining visible-browser gate for ERP v1 delivery.

Rules:

- Use the user's external visible Chrome window only.
- Do not use Codex in-app browser.
- Do not use headless browser.
- Confirm the visible URL is `http://localhost:8080` for Docker delivery validation.
- Login with `admin / 123456`.
- Every page below must open without 404, blank screen, or console error.
- Search/reset buttons must not break the page.
- Add/edit/delete/audit/unaudit/import/export/print buttons must open the expected dialog, perform validation, or return a real backend response.

## Precheck

- Backend Admin: `http://localhost:6060`
- Backend App: `http://localhost:6061`
- Docker Web gateway: `http://localhost:8080`
- Admin UI: `http://localhost:8080`
- Uniapp H5: `http://localhost:8080/h5/`
- Automated acceptance script:

```bash
node scripts/acceptance-check.mjs
```

## System

- 数据看板: `/dashboard`
- 用户管理: `/sys/user`
- 角色管理: `/sys/role`
- 菜单管理: `/sys/menu`
- 文件管理: `/sys/file`
- 部门管理: `/system/dept`
- 岗位管理: `/system/post`
- 字典管理: `/system/dict`
- 参数配置: `/system/config`
- 通知公告: `/system/notice`
- 职员管理: `/system/staff`

## Logs And Monitor

- 操作日志: `/sys/log/oper`
- 登录日志: `/sys/log/login`
- 错误日志: `/sys/log/error`
- 慢SQL日志: `/sys/log/slow-sql`
- 服务监控: `/monitor/admin`
- 缓存监控: `/monitor/cache`
- 在线用户: `/monitor/online`
- 实时日志: `/monitor/log`
- 任务监控: `/monitor/job`

## Product

- 商品管理: `/erp/product/list`
  - Search/reset.
  - Add/edit/delete.
  - Import.
  - Template download.
  - Export.
- 商品分类: `/erp/product/category`
- 单位管理: `/erp/product/unit`
- 商品品牌: `/erp/product/brand`
- 属性设置: `/erp/product/attribute`

## ERP Settings

- 客户管理: `/erp/setting/customer`
- 供应商管理: `/erp/setting/supplier`
- 仓库管理: `/erp/setting/warehouse`
- 账户管理: `/erp/setting/account`
- 代理等级: `/erp/setting/agent-level`
- 单号规则: `/erp/setting/bill-no-rule`
- 字段设置: `/erp/setting/field-setting`
- 打印模板: `/erp/setting/print-template`

## Sales And Purchase

- 销售单: `/erp/sale/list`
- 新增销售单: `/erp/sale/add`
- 销售退货单: `/erp/sale-return/list`
- 新增销售退货: `/erp/sale-return/add`
- 进货单: `/erp/purchase/list`
- 新增进货单: `/erp/purchase/add`
- 进货退货单: `/erp/purchase-return/list`
- 新增进货退货: `/erp/purchase-return/add`

For each bill list:

- Search/reset.
- Add page navigation.
- Edit/copy/delete.
- Audit/unaudit.
- Print preview.
- Export.

For each bill form:

- Next bill number loads.
- Partner selector loads.
- Product selector loads.
- Required field validation blocks incomplete submit.
- Cancel/back works.

## Stock

- 库存查询: `/erp/stock/balance`
- 商品收发明细: `/erp/stock/flow`
- 库存盘点: `/erp/stock/check`
- 新增库存盘点: `/erp/stock/check-add`
- 库存预警: `/erp/stock/warning`

## Finance

- 收款单: `/erp/finance/receipt`
- 付款单: `/erp/finance/payment`
- 其他收入: `/erp/finance/income`
- 其他支出: `/erp/finance/expense`
- 资金流水: `/erp/finance/fund-flow`
- 往来流水: `/erp/finance/partner-flow`

For finance forms:

- Search/reset.
- Add/edit/delete.
- Audit/unaudit.
- Required field validation blocks incomplete submit.

## Reports

- 销售统计: `/erp/report/sale-stat`
- 销售明细: `/erp/report/sale-detail`
- 销售利润表（按商品）: `/erp/report/sale-profit-product`
- 销售利润表（按单据）: `/erp/report/sale-profit-bill`
- 销售利润表（按客户）: `/erp/report/sale-profit-customer`
- 销售分析: `/erp/report/sale-analysis`
- 经营利润: `/erp/report/business-profit`
- 商品热销榜: `/erp/report/hot-products`
- 进货统计: `/erp/report/purchase-stat`
- 进货明细: `/erp/report/purchase-detail`
- 库存余额: `/erp/report/stock-balance`
- 应收应付: `/erp/report/partner-balance`
- 账户余额: `/erp/report/account-balance`
- 员工业绩统计: `/erp/report/employee-performance`
- 员工业绩提成: `/erp/report/employee-commission`
- 商品收发汇总表: `/erp/report/stock-summary`
- 商品进销存变动统计: `/erp/report/inventory-change`
- 经营汇总: `/erp/report/summary`

For reports:

- Page opens.
- Empty data state is real empty state, not fake data.
- Chart pages render without blank canvas errors.
- Export works where visible.

## Mobile H5 Already Verified

- `http://localhost:8080/h5/#/pages/business/home/index`
- `http://localhost:8080/h5/#/pages/business/category/index`
- `http://localhost:8080/h5/#/pages/business/product/detail?id=1`
- `http://localhost:8080/h5/#/pages/business/cart/index`
- `http://localhost:8080/h5/#/pages/business/order/index`
- `http://localhost:8080/h5/#/pages/business/checkout/index`
- `http://localhost:8080/h5/#/pages/business/mine/index`

## Latest Docker Gateway Chrome Pass

2026-06-02 external visible Chrome pass:

- Admin login at `http://localhost:8080` succeeded with `admin / 123456`.
- `73` Admin routes opened without 404, blank page, or console error.
- Product list search/reset and add dialog opened and closed.
- Sale add form opened and required-field validation blocked incomplete submit.
- Receipt add dialog opened and required-field validation blocked incomplete submit.
- Sale analysis report rendered with no console errors.
- `7` H5 routes under `http://localhost:8080/h5/` opened with no console errors.
