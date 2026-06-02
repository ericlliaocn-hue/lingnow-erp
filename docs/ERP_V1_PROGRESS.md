# LingNow ERP v1 Progress

## Stage 12 Audit

Status: completed on local workspace.

Evidence:

- Runtime services are available:
  - Backend: `http://localhost:6060`
  - Admin UI: `http://localhost:6100`
  - Uniapp H5: `http://localhost:6200`
- Empty database initialization was verified by running `backend/sql/init.sql` against a temporary audit database.
- Temporary audit database contained:
  - `208` total menus.
  - `142` ERP menu/button entries.
  - `208` role-menu bindings for super admin.
  - All ERP tables from `erp_account` through `erp_warehouse`.
  - `erp_bill_no_rule.last_date_part`.
  - Default bill number rules with `next_serial = 1` and `last_date_part = NULL`.
- Admin menu component audit checked `70` menu page components and found `0` missing Vue files.
- Current `lingnow_erp` database has `0` rows with `STAGE%` verification markers.
- Source scan found no old mobile/e-commerce business terms in ERP admin/mobile source areas.

Important current baseline:

- Stage 8: sale return, purchase return, stock check, stock warning, other income, other expense, stock/fund/partner flows are implemented and API-verified.
- Stage 9: ERP reports are implemented and API-verified.
- Stage 10: bill number rules, field settings, print templates are implemented and API-verified.
- Stage 11: uniapp was converted to an ERP business shell and verified in external visible Chrome.

## Stage 13 Master Data Hardening

Status: completed on local workspace.

Scope completed:

- Added backend reference-delete guards for ERP master data:
  - Customers referenced by business bills, finance bills, or partner flows cannot be deleted.
  - Suppliers referenced by business bills, finance bills, or partner flows cannot be deleted.
  - Warehouses referenced by bills, bill items, stock balances, stock flows, stock checks, or stock check items cannot be deleted.
  - Accounts referenced by business bills, finance bills, or fund flows cannot be deleted.
  - Product categories referenced by products cannot be deleted.
  - Product categories with child categories cannot be deleted.
  - Units referenced by products, bill items, or stock check items cannot be deleted.
  - Product brands referenced by products cannot be deleted.
  - Agent levels referenced by customers cannot be deleted.
- Product attributes remain deletable when unused because current products store `attribute_text` only; there is no real attribute ID foreign key to enforce.
- Updated the master data delete confirmation text to explain that referenced business data must be disabled instead of deleted.

Evidence:

- Backend build passed:
  - `mvn -q -pl lingnow-admin -am -DskipTests package`
- Admin build passed:
  - `npm run build`
- Real API regression used `/auth/login` with `admin / 123456`, then created temporary `STAGE13_1780302228750` business records through ERP APIs.
- API regression verified:
  - Unreferenced customer delete succeeds.
  - Referenced customer, supplier, warehouse, account, product category, unit, product brand, and agent level deletes are rejected with business messages.
- External visible Chrome validation was used for:
  - `http://localhost:6100/login?redirect=%2Ferp%2Fstock%2Fcheck-add`
  - `http://localhost:6100/erp/setting/customer`
  - `http://localhost:6100/erp/product/list`
  - `http://localhost:6100/erp/product/category`
- External Chrome validation confirmed:
  - Admin login succeeds.
  - Customer management, product management, and product category pages load without 404.
  - Customer delete confirmation includes the referenced-data disable guidance.
  - Browser console errors after the UI check: `0`.
- Temporary `STAGE13%` API and UI verification rows were physically cleaned from `lingnow_erp`; final count: `0`.

## Stage 14 Bill State Machine And Workflow

Status: completed on local workspace.

Scope completed:

- Hardened business bill validation for sales, sales returns, purchases, and purchase returns:
  - Customer or supplier must exist and be enabled.
  - Header warehouse and item warehouse must exist and be enabled.
  - Account must exist and be enabled when supplied.
  - Paid amount requires an account.
  - Discount, other amount, and paid amount cannot be negative.
  - Paid amount cannot exceed payable amount.
  - Repeated audit is rejected with a dedicated business message.
  - Audited bills still cannot be edited or deleted.
- Added bill copy workflow:
  - `POST /erp/{module}/copy/{id}` creates an unaudited copied bill with a new bill number and copied items.
- Improved admin bill pages:
  - List page has a `复制` action.
  - Header audit, unaudit, and delete buttons are disabled according to selected bill state.
  - Delete confirmation explains that audited bills must be unaudited first.
  - Audited bill form shows a read-only warning and disables form controls and item removal.

Evidence:

- Backend build passed:
  - `mvn -q -pl lingnow-admin -am -DskipTests package`
- Admin build passed:
  - `npm run build`
- Real API regression used `/auth/login` with `admin / 123456`, then created temporary `STAGE14_1780302900942` business records through ERP APIs.
- API regression verified:
  - Overpaid bills are rejected.
  - Disabled customer, warehouse, and account are rejected.
  - Paid amount without account is rejected.
  - Purchase audit creates usable stock for the sale verification path.
  - Copying a sale bill returns a new bill ID.
  - Audited sale bill rejects repeat audit, edit, and delete.
  - Unaudit then delete succeeds for sale, copied sale, and purchase test bills.
- External visible Chrome validation was used for:
  - `http://localhost:6100/erp/sale/list`
  - `http://localhost:6100/erp/sale/add`
- External Chrome validation confirmed:
  - Sales list loads without 404 and shows `复制`, `审核`, and `反审核`.
  - Sales form loads without 404 and shows save/add-product workflow.
  - Fresh validation after backend restart produced `0` new console errors.
- Temporary `STAGE14%` verification rows were physically cleaned from `lingnow_erp`; final count: `0`.

## Stage 15 Inventory Module Hardening

Status: API/build completed; external Chrome page verification is pending because the visible Chrome session was logged out by API token rotation and the Chrome extension could not type into the login form.

Scope completed:

- Hardened stock check validation:
  - Stock check warehouse must exist and be enabled.
  - Repeated stock-check audit is rejected with a dedicated business message.
  - Negative stock-check quantity remains rejected.
  - Audited stock checks remain protected from edit/delete.
- Improved stock warning pagination:
  - Warning type filtering now reports totals after filtering instead of returning the current-page filtered count.
- Improved admin stock-check pages:
  - List page audit, unaudit, and delete buttons are disabled according to selected stock-check state.
  - Delete confirmation explains that audited stock checks must be unaudited first.
  - Audited stock-check form shows a read-only warning and disables form controls and item removal.

Evidence:

- Backend build passed:
  - `mvn -q -pl lingnow-admin -am -DskipTests package`
- Admin build passed:
  - `npm run build`
- Real API regression used `/auth/login` with `admin / 123456`, then created temporary `STAGE15_1780303233103` records through ERP APIs.
- API regression verified:
  - Disabled warehouse stock check is rejected.
  - Negative stock-check quantity is rejected.
  - Purchase audit creates stock quantity `3`.
  - Stock-check audit with checked quantity `1` reduces stock to `1`.
  - Stock-check unaudit rolls stock back to `3`.
  - Repeated stock-check audit is rejected.
  - High-stock warning query returns `HIGH` with total `1`.
- Temporary `STAGE15%` verification rows were physically cleaned from `lingnow_erp`; final count: `0`.
- External visible Chrome was attempted for:
  - `http://localhost:6100/erp/stock/check`
  - `http://localhost:6100/erp/stock/check-add`
  - `http://localhost:6100/erp/stock/warning`
  - `http://localhost:6100/erp/stock/balance`
- External Chrome verification status:
  - Pending. API regression rotated the `admin` token and the visible Chrome session was redirected to login.
  - The Chrome extension failed to type/fill the login form due to its virtual clipboard/input limitation.
  - Per project rule, no in-app browser or headless browser was used as a substitute.

## Stage 16 Finance And Partner Account Hardening

Status: API/build completed; external Chrome page verification is pending for the same visible Chrome login-input limitation recorded in Stage 15.

Scope completed:

- Hardened finance bill validation:
  - Receipt customer must exist and be enabled.
  - Payment supplier must exist and be enabled.
  - Account must exist and be enabled for receipt, payment, income, and expense.
  - Amount must be greater than zero.
  - Repeated audit is rejected with a dedicated business message.
  - Audited finance bills remain protected from edit/delete.
- Confirmed finance audit side effects:
  - Receipt audit creates incoming fund flow and customer receive partner flow.
  - Receipt unaudit removes both fund and partner flows.
  - Payment audit/unaudit path remains functional.
- Improved admin finance page:
  - Audited row action says `查看` instead of `修改`.
  - Audited row delete action is disabled.
  - Audited dialog shows a read-only warning and disables form controls and confirm button.
  - Delete confirmation explains that audited bills must be unaudited first.

Evidence:

- Backend build passed:
  - `mvn -q -pl lingnow-admin -am -DskipTests package`
- Admin build passed:
  - `npm run build`
- Real API regression used `/auth/login` with `admin / 123456`, then created temporary `STAGE16_1780303584718` finance records through ERP APIs.
- API regression verified:
  - Disabled customer, supplier, and account are rejected.
  - Negative amount is rejected.
  - Receipt audit creates matching fund flow and partner flow.
  - Receipt repeated audit, edit after audit, and delete after audit are rejected.
  - Receipt unaudit removes generated fund/partner flows.
  - Payment audit and unaudit path succeeds.
- Temporary `STAGE16%` verification rows were physically cleaned from `lingnow_erp`; final count: `0`.

## Stage 17 Dashboard And Reports Delivery Polish

Status: build/API completed; external Chrome page verification still needs a logged-in visible Chrome session.

Scope completed:

- Updated the dashboard wording to `数据看板` and clarified that all displayed metrics come from the current database.
- Data board now combines system user metrics and ERP operating metrics:
  - user count, enabled users, disabled users, online users, today/weekly/monthly new users;
  - product count, customer count, supplier count, today sale amount, today purchase amount, stock amount, receivable, payable, and account balance.
- Generic ERP report pages now render ECharts from real API rows:
  - bar charts for grouped reports;
  - line charts for analysis/trend reports;
  - empty database returns empty tables or zero metrics, not fake chart data.

Evidence:

- Backend build passed:
  - `mvn -q -pl lingnow-admin -am -DskipTests package`
- Admin build passed:
  - `npm run build`
- Real API smoke used `/auth/login` with `admin / 123456`.
- API smoke verified:
  - `/dashboard/user` returns `erpStats`.
  - `/erp/report/summary` returns real database totals.
  - `/erp/report/bill-stat?billType=SALE&groupBy=date` returns real grouped rows.
  - `/erp/report/trend` returns real trend rows.
- Current empty ERP business data produced zero/empty values, which is the correct real-data state.

## Stage 18 Print Import Export

Status: backend/admin/API completed; external Chrome page verification still needs a logged-in visible Chrome session.

Scope completed:

- Added CSV export utility for real database exports.
- Added business bill export endpoints:
  - `GET /erp/sale/export`
  - `GET /erp/sale-return/export`
  - `GET /erp/purchase/export`
  - `GET /erp/purchase-return/export`
- Added business bill print preview endpoint:
  - `GET /erp/{module}/print/{id}`
  - Print preview returns real bill header and item data.
- Added product import/export endpoints:
  - `GET /erp/product/export`
  - `GET /erp/product/import-template`
  - `POST /erp/product/import`
- Product import behavior:
  - Uses uploaded CSV rows.
  - Creates real product records through the ERP product model.
  - Validates required product code/name.
  - Validates referenced category, brand, and unit names if supplied.
  - Returns per-row error messages for invalid rows.
- Added generic report export:
  - `GET /erp/report/export`
  - Uses the same real report query logic as the visible report pages.
- Added admin UI actions:
  - Product: 导入, 模板下载, 导出.
  - Business bill list: 打印, 导出.
  - Generic report page: 导出.
- Added button permissions and synchronized them to the local `lingnow_erp` database:
  - `erp:product:import`
  - `erp:product:export`
  - `erp:sale:export`
  - `erp:sale:print`
  - `erp:sale-return:export`
  - `erp:sale-return:print`
  - `erp:purchase:export`
  - `erp:purchase:print`
  - `erp:purchase-return:export`
  - `erp:purchase-return:print`

Evidence:

- Backend build passed:
  - `mvn -q -pl lingnow-admin -am -DskipTests package`
- Admin build passed:
  - `npm run build`
- Local `lingnow_erp` menu permission sync inserted or confirmed `10` new button permissions and bound them to super admin.
- Real API regression used `/auth/login` with `admin / 123456`, then created temporary `STAGE18_1780304810865` product data through `/erp/product/import`.
- API regression verified:
  - `/admin/menu/tree` includes new import/export/print button permissions.
  - Product import template contains the expected product fields.
  - Invalid product import returns row-level failure information.
  - Valid product import creates a real product.
  - Product export returns the imported real product.
  - Sales export returns real CSV headers from current data.
  - Report exports work for sale statistics, sale analysis, and business profit.
- Temporary `STAGE18_1780304810865` product was physically deleted through `/erp/product/{id}`; follow-up product query returned `0`.

## Stage 19 Permissions System Parameters And Audit Logs

Status: backend/admin/API completed; external Chrome page verification still needs a logged-in visible Chrome session.

Scope completed:

- Added real ERP system parameter defaults to `sys_config`:
  - `erp.allowNegativeStock = N`
  - `erp.auditReadonly = Y`
  - `erp.qtyPrecision = 2`
  - `erp.amountPrecision = 2`
- Added read-only ERP parameter endpoint:
  - `GET /erp/system/params`
  - Protected by `erp:config:params`.
- Added `erp:config:params` button permission to initialization SQL and synchronized it to local `lingnow_erp`.
- Added operation log annotations using the existing system `@Log` pattern for:
  - ERP master data add/edit/delete.
  - ERP product add/edit/delete/import/export.
  - ERP business bill add/edit/delete/copy/audit/unaudit/export/print.
  - ERP finance bill add/edit/delete/audit/unaudit.
  - ERP stock check add/edit/delete/audit/unaudit.
  - ERP bill number rule, field setting, and print template add/edit/delete.

Evidence:

- Backend build passed:
  - `mvn -q -pl lingnow-admin -am -DskipTests package`
- Admin build passed:
  - `npm run build`
- Local `lingnow_erp` has `4` ERP system parameter rows with `config_key LIKE 'erp.%'`.
- Real API regression used `/auth/login` with `admin / 123456`.
- API regression verified:
  - `/erp/system/params` returns all four ERP parameter keys.
  - Calling `/erp/system/params` without a token is rejected.
  - Creating and deleting a real temporary product `STAGE19_1780305136618` writes ERP product operation logs visible through `/sys/log/oper/list`.
  - Temporary `STAGE19_1780305136618` product was physically deleted; follow-up product query returned `0`.

## Stage 20 Mobile Business App Real API Integration

Status: backend/app/uniapp build and API completed; external Chrome H5 page verification still pending.

Scope completed:

- Added real App-side ERP read APIs under `lingnow-app`:
  - `GET /app/erp/dashboard`
  - `GET /app/erp/customers`
  - `GET /app/erp/suppliers`
  - `GET /app/erp/products`
  - `GET /app/erp/products/{id}`
  - `GET /app/erp/bills`
- The mobile ERP APIs read real database records through existing ERP services.
- The APIs require App login through the existing Sa-Token `token-app` chain.
- Added missing App authentication tables to initialization SQL and synchronized them to local `lingnow_erp`:
  - `app_user`
  - `app_user_info`
  - `app_social_user`
- Updated `uniapp` mobile business API calls to always call real `/app/erp/**` endpoints.
- Removed the mobile local empty-data API switch and local fake success fallback.
- Updated product detail to call `GET /app/erp/products/{id}` directly instead of loading all products and filtering locally.
- Updated mobile new-bill page to keep an honest v1 boundary:
  - mobile v1 is read-only for business documents;
  - formal bill creation, audit, and inventory processing remain in the admin UI;
  - no fake draft save or fake success path is exposed.

Evidence:

- App backend build passed:
  - `mvn -q -pl lingnow-app -am -DskipTests package`
- Uniapp checks passed:
  - `npm run type-check`
  - `npm run build:h5`
- App backend started on:
  - `http://localhost:6061`
- Auth and API regression verified:
  - calling `/app/erp/dashboard` without `token-app` returned `401`.
  - a temporary real App user `STAGE20_APP_19920010020` was inserted with a BCrypt password only for API regression.
  - password login through `POST /app/auth/login` returned a real `token-app`.
  - token-authenticated calls returned `200` for dashboard, customers, products, product detail, and bills.
  - current local ERP business rows are logically deleted, so mobile list endpoints correctly returned real empty arrays rather than fake rows.
- Temporary App user and profile rows were physically deleted after testing; follow-up query returned `0`.

## Stage 21 Acceptance And Cleanup

Status: build/API/mobile H5 acceptance completed; admin UI login and route-open acceptance completed in the external visible Chrome window. Exhaustive button-by-button browser acceptance is still pending because the Codex Chrome Extension communication channel timed out before the remaining button batches could finish.

Completed verification:

- Admin backend build passed:
  - `mvn -q -pl lingnow-admin -am -DskipTests package`
- Admin UI build passed:
  - `npm run build`
- App backend build passed:
  - `mvn -q -pl lingnow-app -am -DskipTests package`
- Uniapp checks passed:
  - `npm run type-check`
  - `npm run build:h5`
- 2026-06-02 service recovery and no-login checks:
  - Admin backend restarted on `http://localhost:6060`.
  - App backend restarted on `http://localhost:6061`.
  - Admin UI dev server restarted on `http://localhost:6100`.
  - Uniapp H5 dev server restarted on `http://localhost:6200`.
  - all four ports returned `up` from `nc -z`.
  - `http://localhost:6100/`, `http://localhost:6200/`, `http://localhost:6060/doc.html`, and `http://localhost:6061/doc.html` returned HTTP `200`.
  - `GET http://localhost:6061/app/erp/dashboard` without `token-app` returned business `401 请先登录`, confirming the App auth guard is active.
  - Admin UI production build passed again with `npm run build`.
  - Uniapp type-check passed again with `npm run type-check`.
  - Uniapp H5 production build passed again with `npm run build:h5`.
  - Admin backend current source build passed again with `mvn -q -pl lingnow-admin -am -DskipTests package`.
  - App backend current source build passed again with `mvn -q -pl lingnow-app -am -DskipTests package`.
  - all four service ports remained `up` after the backend builds.
  - Chrome was not running, so browser button acceptance could not resume without starting the external visible Chrome window.
- Running services confirmed:
  - Admin backend: `http://localhost:6060`
  - App backend: `http://localhost:6061`
  - Admin UI: `http://localhost:6100`
  - Uniapp H5: `http://localhost:6200`
- Admin API regression used real `/auth/login` with `admin / 123456`.
- Admin API regression verified:
  - `/admin/menu/tree`
  - `/admin/menu/tree/all`
  - `/admin/menu/list`
  - `/dashboard/user`
  - `/user/list`
  - `/role/list`
  - `/admin/file/page`
  - `/system/dept/list`
  - `/system/post/list`
  - `/system/dict/type/list`
  - `/system/dict/type/optionselect`
  - `/system/config/list`
  - `/system/notice/list`
  - `/system/staff/list`
  - `/sys/log/oper/list`
  - `/sys/log/login/list`
  - `/sys/log/error/list`
  - `/sys/log/slowSql/list`
  - `/erp/report/summary`
  - `/erp/report/trend`
  - `/erp/system/params`
  - `/erp/product/list`
  - `/erp/sale/list`
  - `/erp/stock/check/list`
  - `/erp/finance/receipt/list`
  - `/erp/finance/payment/list`
  - `/erp/finance/income/list`
  - `/erp/finance/expense/list`
  - `/erp/finance/fund-flow/list`
  - `/erp/finance/partner-flow/list`
  - `/monitor/admin/dashboard`
  - `/monitor/cache`
  - `/monitor/job/list`
  - `/monitor/online/list`
- Admin API matrix verification covered `84` frontend-backed endpoints:
  - `78` JSON endpoints returned business success code `200`.
  - `6` CSV endpoints returned HTTP `200` with real CSV headers: product template/export and sale/purchase export endpoints.
  - No remaining `404`, `500`, missing-table, or missing-field responses were found in the matrix.
- During matrix verification, `/system/dict/type/optionselect` was found to be incorrectly handled by the generic `/{dictId}` route and returned a system exception.
- Fixed `/system/dict/type/optionselect` by adding a dedicated dictionary type option endpoint; follow-up verification returned `200` with `13` dictionary type rows.
- Admin menu/permission audit:
  - local database has `70` visible page menus, and every page menu component resolves to an existing Vue file.
  - local database has `219` active menu/button rows.
  - super admin role has `219` menu/button bindings.
  - missing super admin bindings: `0`.
- App API regression verified:
  - `/app/erp/dashboard` without `token-app` returns `401`.
  - authenticated App calls return `200` for dashboard, customers, products, product detail, and bills.
- External Chrome visible-window verification was performed on the user Chrome window, not Codex in-app browser and not headless.
- External Chrome verified admin login and route loading:
  - `http://localhost:6100/login?redirect=/dashboard` accepted `admin / 123456` through the visible login form.
  - successful login landed on `http://localhost:6100/dashboard`.
  - the dashboard showed `数据看板` and real database metrics.
  - all `70` visible admin page routes from `docs/ERP_MANUAL_CHROME_CHECKLIST.md` were opened in the logged-in Chrome session.
  - no route redirected back to login after avoiding additional `/auth/login` calls.
  - no `404`, blank page, or new console error was observed during the route-open pass.
  - `http://localhost:6100/sys/log/login` was separately rechecked after a script false-positive and showed real login log rows.
- Admin token behavior confirmed during browser acceptance:
  - `backend/lingnow-admin/src/main/resources/application.yml` currently sets `is-concurrent: false` and `is-share: false`.
  - by design, a new `admin` login invalidates the previous `admin` token.
  - browser acceptance must not run API smoke scripts that call `/auth/login` with the same account while the visible Chrome session is being tested.
- External Chrome verified admin safe button and form validation batches:
  - system pages: 用户管理, 角色管理, 菜单管理, 文件管理, 部门管理, 岗位管理.
  - base/log pages: 字典管理, 参数配置, 通知公告, 职员管理, 操作日志, 登录日志, 错误日志, 慢SQL日志.
  - monitor pages: 服务监控, 缓存监控, 在线用户, 实时日志, 任务监控.
  - product/settings pages: 商品管理, 商品分类, 单位管理, 商品品牌, 属性设置, 客户管理, 供应商管理, 仓库管理, 账户管理, 代理等级, 单号规则, 字段设置, 打印模板.
  - bill list pages: 销售单, 销售退货单, 进货单, 进货退货单.
  - bill form pages: 新增销售单, 新增销售退货, 新增进货单, 新增进货退货.
  - stock/finance pages: 库存查询, 商品收发明细, 库存盘点, 新增库存盘点, 库存预警, 收款单, 付款单, 其他收入, 其他支出, 资金流水, 往来流水.
  - report pages: 销售统计, 销售明细, 销售利润表（按商品）, 销售利润表（按单据）, 销售利润表（按客户）, 销售分析, 经营利润, 商品热销榜, 进货统计, 进货明细, 库存余额, 应收应付, 账户余额, 员工业绩统计, 员工业绩提成, 商品收发汇总表, 商品进销存变动统计, 经营汇总.
  - tested actions included search, reset, add dialog open/cancel, import dialog open/cancel where present, template download/export where present, add-form save validation, and back/cancel.
  - no page failed, no action click failed, and no new console error was observed in the final passing batches.
- Browser acceptance found and fixed one real UI issue:
  - Finance bill add dialogs originally called the backend before frontend required validation blocked incomplete data, producing console errors such as `账户不能为空`.
  - Fixed `admin-ui/src/views/erp/finance/form.vue` by adding Element Plus form rules, field props, and `formRef.validate()` before save.
  - Re-tested 收款单, 付款单, 其他收入, 其他支出 add dialogs in the external visible Chrome window; incomplete submit is now blocked by frontend validation with `0` new console errors.
- External Chrome verified real-row stateful actions with temporary real business data:
  - Created real temporary records using the `STAGE_UI_1780332784779` marker: unit, customer, supplier, warehouse, account, product, stock buffer purchase, sale bill, sale return bill, purchase bill, purchase return bill, stock check bill, and receipt bill.
  - Re-logged into the external visible Chrome window after API fixture creation because current Sa-Token config intentionally invalidates the previous `admin` token on a new login.
  - Sales, sales return, purchase, and purchase return list rows were searched by bill number and each row was browser-clicked through: 查看/修改, 打印, 复制, 审核, 反审核, 删除.
  - Stock check row was searched by check number and browser-clicked through: 查看/修改, 审核, 反审核, 删除.
  - Receipt row was browser-clicked through: 修改 dialog, 审核, 反审核, 删除.
  - All row-action batches finished with `0` failed pages/actions and `0` new console errors.
  - Temporary records, including copied bills and generated stock/fund/partner flows, were physically cleaned from the local database.
  - Final `STAGE_UI_1780332784779` residual count across temporary ERP tables returned `0`.
- Final acceptance cleanup and API check:
  - `node scripts/acceptance-check.mjs` passed after browser row-action validation and cleanup.
  - Admin API matrix still covered `84` endpoints successfully.
  - `STAGE_UI_%` residual count across temporary ERP business/master tables returned `0`.
  - Services remained online on ports `6060`, `6061`, `6100`, and `6200`.
- External Chrome verified Uniapp H5 routes:
  - `http://localhost:6200/#/pages/business/home/index`
  - `http://localhost:6200/#/pages/business/category/index`
  - `http://localhost:6200/#/pages/business/cart/index`
  - `http://localhost:6200/#/pages/business/order/index`
  - `http://localhost:6200/#/pages/business/checkout/index`
  - `http://localhost:6200/#/pages/business/mine/index`
- Mobile H5 acceptance result:
  - each route opens.
  - unauthenticated business data pages show explicit login/retry failure states instead of fake empty success.
  - bottom tab bar is not duplicated.
  - fresh external Chrome tab showed `0` console error logs on the verified mobile routes.
  - mobile new-bill page clearly states v1 is read-only and points formal bill creation/audit/inventory processing to the admin UI.

Known acceptance limitation:

- Admin login through the external visible Chrome window now works.
- The Chrome Extension channel recovered and safe admin button/form validation batches were completed.
- Row-level destructive/stateful UI actions were also verified with real temporary business data and physically cleaned afterward.
- Remaining risk is not a known failing page; it is that future industry-specific ERP branches may add new row actions or custom fields that need their own pass.

Cleanup evidence:

- Search over source/docs found no remaining mobile local API switch, fake local ERP data fallback, or old placeholder text such as `移动端接口接入后` / `移动端单据接口未接入`.
- Temporary Stage 20 App user data was physically deleted.
- Added repeatable automated acceptance script:
  - `node scripts/acceptance-check.mjs`
  - Latest run passed.
- Added external Chrome manual verification checklist:
  - `docs/ERP_MANUAL_CHROME_CHECKLIST.md`
  - This is the remaining gate for visible admin UI button-by-button acceptance after a manual Chrome login.

## Stage 22 Production Delivery

Status: automated production-delivery acceptance completed locally; Docker runtime verification is pending because Docker is not installed in the current environment.

Scope completed:

- Added single-machine Docker Compose production delivery skeleton:
  - MySQL 8, Redis 7, Admin backend, App backend, and Nginx web gateway.
  - Admin UI and H5 are built into the web image.
  - Admin API, App API, and `/files/` are proxied by Nginx.
- Added guarded production scripts:
  - startup refuses to run with missing `.env` or unchanged `CHANGE_ME` placeholders.
  - backup and restore scripts cover MySQL data and uploaded files.
- Hardened production defaults:
  - API docs are disabled in `prod` by default through `LINGNOW_API_DOC_ENABLED=false`.
  - production logging defaults to `INFO`.
  - file fallback storage path is environment-driven through `LINGNOW_FILE_BASE_PATH`.
- Fixed Warm-Flow approval task reading:
  - approval submit still uses `FlowFactory` and Warm-Flow services.
  - pending task reads now hydrate missing task `businessId` from the real Flow instance when the Warm-Flow task row does not carry it.
  - approval task VO serializes snowflake IDs as strings to avoid browser/JavaScript precision loss.
- Expanded repeatable acceptance script:
  - active menu/button audit now expects at least `219` and verifies super admin binding count against the actual active menu count.
  - API matrix now covers `89` endpoints, including approval todo/mine/done and Warm-Flow plugin endpoints.
  - delivery business acceptance uses real `DELIVERY_%` data only and physically removes it before and after the run.

Evidence:

- Admin backend build passed:
  - `mvn -pl lingnow-admin -am -DskipTests package`
- Latest automated acceptance passed:
  - `node scripts/acceptance-check.mjs`
- Acceptance verified:
  - real admin login returned a token.
  - `74` visible page menus were found and every visible page component exists.
  - `230` active menu/button rows exist.
  - super admin has `230/230` active menu/button bindings.
  - `89` Admin/API/CSV endpoints returned successful responses.
  - App ERP dashboard rejects missing app token.
  - purchase bill submit and Warm-Flow approval audited the bill and increased stock.
  - sale bill submit and Warm-Flow approval audited the bill, decreased stock, and created fund flow.
  - receipt bill submit and Warm-Flow approval audited the finance bill and created fund flow.
  - temporary `DELIVERY_%` rows were physically cleaned; final residual count was `0`.

Production package files:

- `.env.example`
- `.dockerignore`
- `docker-compose.yml`
- `docker/backend/admin.Dockerfile`
- `docker/backend/app.Dockerfile`
- `docker/web/Dockerfile`
- `docker/web/nginx.conf`
- `docker/mysql/conf.d/lingnow.cnf`
- `scripts/prod-up.sh`
- `scripts/prod-down.sh`
- `scripts/prod-logs.sh`
- `scripts/backup-prod.sh`
- `scripts/restore-prod.sh`
- `docs/PRODUCTION_DELIVERY.md`

## Stage 23 Docker Runtime And Delivery Closeout

Status: Docker host dependency runtime, automated API/business acceptance, and final external Chrome visible-window page pass completed locally.

Scope completed:

- Installed and verified Docker Desktop and Docker Compose locally.
- Added host dependency delivery template:
  - `docker-compose.host.example.yml` starts only Admin, App, and Web.
  - MySQL and Redis stay on the host through `host.docker.internal`.
  - local `docker-compose.host.yml` and `.env` remain ignored because they contain machine secrets.
- Expanded `.env.example` with both supported runtime modes:
  - full Compose mode with MySQL/Redis containers.
  - host dependency mode for local validation without MySQL/Redis image pulls.
- Fixed Docker web proxy restart stability:
  - `docker/web/nginx.conf` now uses Docker DNS resolver `127.0.0.11` for dynamic Admin/App upstream resolution.
  - this prevents Nginx from keeping stale Admin/App container IPs after backend restart.
- Fixed Docker-gateway acceptance:
  - `scripts/acceptance-check.mjs` now preserves base URL path prefixes such as `/admin-api` and `/app-api`.
  - no login or authentication behavior was changed.
- Updated production delivery documentation to remove stale Docker-not-installed and container-restart limitation notes.

Evidence:

- Docker host dependency runtime passed:
  - `docker compose --env-file .env -f docker-compose.host.yml up -d --build`
  - `lingnow-erp-admin`, `lingnow-erp-app`, and `lingnow-erp-web` were healthy.
  - `docker ps -a` and `docker images` showed no MySQL/Redis containers or images.
- Gateway/API checks passed:
  - `http://localhost:8090/healthz`
  - `http://localhost:8090/admin-api/welcome`
  - `http://localhost:8090/app-api/welcome`
- Restart checks passed:
  - Admin/App containers restarted successfully.
  - logs did not contain `GracefulShutdownCallback`, `NoClassDefFoundError`, `ClassNotFoundException`, database connection failure, or Redis connection failure.
  - Admin/App gateway endpoints remained `200` after restart.
- Latest automated delivery acceptance passed:
  - `ADMIN_BASE_URL=http://localhost:8090/admin-api APP_BASE_URL=http://localhost:8090/app-api MYSQL_PWD=... node scripts/acceptance-check.mjs`
  - verified `89` Admin/API/CSV endpoints.
  - verified App missing-token guard.
  - verified purchase, sale, and receipt Warm-Flow approval with real temporary `DELIVERY_%` data.
  - physically cleaned temporary verification rows; final residual count was `0`.

External Chrome evidence:

- Used the user's external visible Chrome window only.
- Did not use Codex in-app browser or headless browser.
- Admin login at `http://localhost:8090` succeeded.
- `73` Admin routes opened without 404, blank page, or console error.
- Product list search/reset and add dialog passed.
- Sale add form and receipt add dialog required-field validation passed.
- Sale analysis report rendered without console errors.
- `7` H5 routes under `http://localhost:8090/h5/` opened without console errors.
