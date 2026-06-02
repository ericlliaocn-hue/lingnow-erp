# 外置 Chrome 页面回归清单

## 规则

- 只能使用外置 Chrome 可见窗口。
- 禁止使用 Codex 内置浏览器。
- 禁止使用 headless 浏览器。
- 默认地址：`http://localhost:8090`。
- 默认账号：`admin / 123456`。
- 页面不能 404、空白、明显错位或控制台红错。

## 管理端基础

- 登录页。
- 数据看板：`/dashboard`。
- 菜单展开、收起、刷新后路由保持。
- 右上角通知抽屉。

## 系统与权限

- `/sys/user` 用户管理。
- `/sys/role` 角色管理，新增角色弹窗和菜单权限树。
- `/sys/menu` 菜单管理。
- `/sys/file` 文件管理。
- `/system/staff` 职员管理，新增职员弹窗。

## ERP 基础资料

- `/erp/product/category` 商品分类。
- `/erp/product/unit` 单位管理。
- `/erp/product/brand` 商品品牌。
- `/erp/product/attribute` 属性设置。
- `/erp/product/list` 商品管理。
- `/erp/setting/customer` 客户管理。
- `/erp/setting/supplier` 供应商管理。
- `/erp/setting/warehouse` 仓库管理。
- `/erp/setting/account` 账户管理。

## 单据

- `/erp/purchase/add` 新增进货单。
- `/erp/purchase/list` 进货单。
- `/erp/sale/add` 新增销售单。
- `/erp/sale/list` 销售单。
- `/erp/sale-return/add` 新增销售退货。
- `/erp/sale-return/list` 销售退货单。
- `/erp/purchase-return/add` 新增进货退货。
- `/erp/purchase-return/list` 进货退货单。

每类单据至少验证：

- 新增。
- 保存并提交审批。
- 待我审批通过或驳回。
- 列表状态变化。
- 打印预览。

## 库存

- `/erp/stock/balance` 库存查询。
- `/erp/stock/flow` 商品收发明细。
- `/erp/stock/check-add` 新增库存盘点。
- `/erp/stock/check` 库存盘点。
- `/erp/stock/warning` 库存预警。

## 财务

- `/erp/finance/receipt` 收款单。
- `/erp/finance/payment` 付款单。
- `/erp/finance/income` 其他收入。
- `/erp/finance/expense` 其他支出。
- `/erp/finance/fund-flow` 资金流水。
- `/erp/finance/partner-flow` 往来流水。

## 审批

- `/erp/approval/todo` 待我审批。
- `/erp/approval/mine` 我发起的。
- `/erp/approval/done` 已办审批。
- `/erp/workflow/designer` 流程设计器入口。

## 报表

- `/erp/report/sale-stat`
- `/erp/report/sale-detail`
- `/erp/report/sale-profit-product`
- `/erp/report/sale-profit-bill`
- `/erp/report/sale-profit-customer`
- `/erp/report/sale-analysis`
- `/erp/report/business-profit`
- `/erp/report/hot-products`
- `/erp/report/purchase-stat`
- `/erp/report/purchase-detail`
- `/erp/report/stock-balance`
- `/erp/report/partner-balance`
- `/erp/report/account-balance`
- `/erp/report/employee-performance`
- `/erp/report/employee-commission`
- `/erp/report/stock-summary`
- `/erp/report/inventory-change`
- `/erp/report/summary`

## 监控

- `/monitor/admin` 服务监控。
- `/monitor/cache` 缓存监控。
- `/monitor/online` 在线用户。
- `/monitor/log` 实时日志。
- `/monitor/job` 任务监控。

## H5

- `http://localhost:8090/h5/#/pages/business/home/index`
- `http://localhost:8090/h5/#/pages/business/category/index`
- `http://localhost:8090/h5/#/pages/business/cart/index`
- `http://localhost:8090/h5/#/pages/business/order/index`
- `http://localhost:8090/h5/#/pages/business/mine/index`
- `http://localhost:8090/h5/#/pages/business/checkout/index`

H5 未登录态应显示真实鉴权提示，不得用假数据填充。

