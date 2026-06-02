# LingNow ERP Admin UI

LingNow ERP Admin UI 是 ERP 管理端前端，基于 Vue 3、Vite、TypeScript、Element Plus、Pinia、Vue Router 构建。

本目录当前作为 `/Users/eric/workspace/cool/lingnow-erp` monorepo 的 `admin-ui` 子目录存在；Git 根目录在上一级 `lingnow-erp`。完整 Docker、后端、移动端和交付文档位于仓库根目录。

## 当前能力

- 登录、退出、动态路由、菜单权限、按钮权限。
- 数据看板、系统管理、基础设置、日志管理、系统监控。
- 商品、ERP 设置、销售、进货、库存、财务、报表。
- Warm-Flow 审批中心：待我审批、我的审批、已办审批、流程设计入口。
- 任务监控、实时日志、缓存监控、在线用户、服务监控。
- ERP 单据导入模板、导出、打印入口、审核/反审核、提交审批。

## 技术栈

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts / ApexCharts

## 安装依赖

```bash
cd admin-ui
npm ci
```

## 本地开发

```bash
cd admin-ui
npm run dev -- --host 0.0.0.0
```

开发端口由 Vite 决定，通常为 `5173` 或当前可用端口。开发代理配置在 `vite.config.ts`，默认把 `/admin-api` 代理到后端 Admin 服务。

本地后端默认：

- Admin API：`http://localhost:6060`
- 网关交付地址：`http://localhost:8090`

## 构建

```bash
cd admin-ui
npm run build
```

构建产物：

```text
admin-ui/dist
```

该目录由 `.gitignore` 忽略，不提交。

## 主要目录

```text
src/api/erp               ERP 接口封装
src/views/erp             ERP 页面
src/views/home            数据看板
src/views/monitor         监控页面
src/views/system          基础设置
src/views/sys             系统管理、日志
src/store                 Pinia 状态
src/router                路由
src/utils/request.ts      请求封装和 token 注入
```

## 验收入口

Docker 交付模式下，管理端入口：

```text
http://localhost:8090
```

默认账号：

```text
admin / 123456
```

页面级验收必须使用外置 Chrome 可见窗口，不使用 Codex 内置浏览器，不使用 headless。

最新验收覆盖：

- 73 个 Admin 路由打开。
- 商品管理搜索/重置/新增弹窗。
- 销售单新增必填校验。
- 收款单新增必填校验。
- 销售分析图表渲染。
- 控制台无 error。
