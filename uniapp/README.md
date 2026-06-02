# LingNow ERP Uniapp

LingNow ERP Uniapp 是 ERP 移动端/H5 业务壳，基于 uni-app、Vue 3、Vite 构建。

本目录当前作为 `/Users/eric/workspace/cool/lingnow-erp` monorepo 的 `uniapp` 子目录存在；Git 根目录在上一级 `lingnow-erp`。完整后端、管理端、Docker 和交付文档位于仓库根目录。

## 当前定位

移动端 v1 作为 ERP 业务端壳，适合继续扩展为：

- 业务员端
- 代理端
- 客户下单端
- 移动查询端

当前不在移动端直接创建正式库存/财务单据；正式开单、审核、库存处理和财务处理优先在管理端完成。

## 当前页面

- 首页：业务概览、快捷入口、待办提示。
- 客户：客户资料列表入口。
- 商品：商品资料列表入口。
- 商品详情：商品详情查看。
- 新建单据：移动端 v1 只展示业务壳和说明。
- 单据：业务单据列表入口。
- 我的：登录状态、个人资料、我的单据、账号设置。

未登录时，页面显示真实的登录/重试失败状态，不使用假数据填充。

## 技术栈

- uni-app
- Vue 3
- Vite
- TypeScript
- Sass

## 安装依赖

```bash
cd uniapp
npm ci
```

## 本地开发

```bash
cd uniapp
npm run dev:h5
```

开发端口由 uni-app/Vite 决定，通常为 `5173` 或当前可用端口。

## 构建

类型检查：

```bash
cd uniapp
npm run type-check
```

H5 构建：

```bash
cd uniapp
npm run build:h5
```

构建产物：

```text
uniapp/dist/build/h5
```

该目录由 `.gitignore` 忽略，不提交。

## 主要目录

```text
src/pages/business         ERP 移动端页面
src/api/business.ts        ERP 移动端接口封装
src/components/TabBar.vue  自定义底部导航
src/utils/icons.ts         图标配置
src/pages.json             页面和 tabBar 配置
```

## 交付访问地址

Docker Web 网关默认端口为 `8090`：

```text
http://localhost:8090/h5/
```

已验收 H5 路由：

- `/#/pages/business/home/index`
- `/#/pages/business/category/index`
- `/#/pages/business/product/detail?id=1`
- `/#/pages/business/cart/index`
- `/#/pages/business/checkout/index`
- `/#/pages/business/order/index`
- `/#/pages/business/mine/index`

页面级验收必须使用外置 Chrome 可见窗口，不使用 Codex 内置浏览器，不使用 headless。
