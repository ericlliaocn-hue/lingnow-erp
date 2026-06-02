# LingNow ERP

这是基于 `lingnow-admin` 通用基座扩展的 ERP 项目仓库。

`lingnow-admin` 作为上游基座仓库保留在 `upstream` remote，ERP 业务只在本仓库开发。后续同步基座更新时执行：

```bash
git fetch upstream
git merge upstream/main
```

## 目录

- `backend`：Spring Boot 3.2、Java 17、MyBatis-Plus、Sa-Token、Redis、MySQL。
- `admin-ui`：Vue 3、Vite、Element Plus 管理端。
- `uniapp`：移动端基础壳，可继续改成业务员端、代理端或客户下单端。
- `docs`：项目立项、需求、架构、部署、验收、交付说明等标准化文档。

## 已保留基座能力

- 后端基础能力：用户、角色、菜单、权限、文件、日志、字典、部门、岗位、系统配置、通知。
- 管理端基础能力：登录、布局、动态菜单、权限路由、系统管理、文件管理、日志监控、基础表格页面结构。
- 移动端基础能力：登录、注册、忘记密码、商品分类、购物车、确认下单、订单、我的页面壳。
- 通用基础设施：文件上传、操作日志、后台权限、Sa-Token 鉴权、Redis/MySQL 配置。

## ERP 当前目标

- 使用独立数据库 `lingnow_erp`。
- 保留用户、角色、菜单、权限、文件、日志、字典、部门、岗位、系统配置、通知、监控等基座能力。
- ERP 业务从基础资料开始：商品分类、单位、商品品牌、商品属性、客户、供应商、仓库、资金账户、代理等级。
- 所有页面和接口连接真实数据库，不使用假数据。

## 已剔除的原业务

- 内容、小说、短剧、漫画、FM、播放、签到、积分、会员、兑换、支付等原业务模块。
- 后端业务积木模块和 skill registry。
- 管理端旧业务页面、旧业务 API、旧模块自动注入逻辑。
- 移动端旧模块注册、旧 FM/奶茶文案、播放器状态、支付下单流程。
- IDE 配置、构建产物、依赖安装目录。

## ERP 业务入口

- 后端业务表和服务放在 `backend/lingnow-biz/src/main/java/cc/lingnow/biz`。
- 后端管理接口放在 `backend/lingnow-admin/src/main/java/cc/lingnow/admin/controller`。
- 后端移动端接口建议放在 `backend/lingnow-app/src/main/java/cc/lingnow/app/controller`。
- 管理端业务页面建议放在 `admin-ui/src/views/business`。
- 管理端业务 API 建议放在 `admin-ui/src/api/business`。
- 移动端业务页面当前在 `uniapp/src/pages/business`。
- 移动端业务 API 当前在 `uniapp/src/api/business.ts`。

## 验证命令

```bash
cd backend && mvn -q -DskipTests package
cd admin-ui && npm ci && npm run build
cd uniapp && npm ci && npm run type-check && npm run build:h5
```

## 交付文档

完整交付文档位于 `docs/`，入口为：

```text
docs/00-文档总览.md
```

发版验收主入口：

```bash
ADMIN_BASE_URL=http://localhost:8090/admin-api \
APP_BASE_URL=http://localhost:8090/app-api \
DB_NAME=lingnow_erp \
DB_USER=root \
MYSQL_PWD='你的MySQL密码' \
node scripts/release-acceptance.mjs
```
