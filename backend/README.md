# LingNow ERP Backend

LingNow ERP 后端是 ERP 项目的服务端部分，基于 Spring Boot 3.2、Java 17、MyBatis-Plus、Sa-Token、Redis、MySQL 构建。

本目录是仓库根目录下的 `backend` 子目录。如果在 IDE 中只打开 `backend`，只能看到后端文件视图；完整交付文档和 Docker 文件在仓库根目录。

## 服务模块

- `lingnow-admin`：管理端 API，默认端口 `6060`。
- `lingnow-app`：移动端/业务端 API，默认端口 `6061`。
- `lingnow-biz`：业务实体、Mapper、Service，包含 ERP 基础资料、单据、库存、财务、通知等共享业务能力。
- `lingnow-core`：核心基础能力。
- `lingnow-common`：通用返回、异常、工具、MyBatis-Plus、Redis 等基础设施。
- `framework/*`：框架级扩展模块。

## 当前能力

- 基座能力：用户、角色、菜单、权限、文件、日志、字典、部门、岗位、系统参数、通知公告、监控。
- ERP 基础资料：商品分类、单位、品牌、属性、商品、客户、供应商、仓库、账户、代理等级。
- ERP 单据：销售单、销售退货、进货单、进货退货。
- 库存：库存查询、库存流水、库存盘点、库存预警。
- 财务：收款、付款、其他收入、其他支出、资金流水、往来流水。
- 报表：销售/进货统计、利润、经营汇总、热销榜、库存收发、账户/往来余额等。
- 审批：已接入 Warm-Flow，支持单据提交、通过、驳回、撤回、转交和审批记录。
- 任务与监控：Quartz 任务监控、实时日志、缓存监控、在线用户、服务监控。

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8+
- Redis 7+

默认数据库名：

```text
lingnow_erp
```

## 初始化数据库

在仓库根目录或 `backend` 目录执行均可：

```bash
mysql -u root -p lingnow_erp < sql/init.sql
```

初始化脚本只写入系统必需的菜单、权限、字典和基础配置，不写入假 ERP 业务数据。验收脚本使用 `DELIVERY_%` 临时数据，并会在测试前后物理清理。

## 本地启动

启动 Admin：

```bash
cd backend
mvn -pl lingnow-admin -am spring-boot:run
```

启动 App：

```bash
cd backend
mvn -pl lingnow-app -am spring-boot:run
```

默认地址：

- Admin API：`http://localhost:6060`
- App API：`http://localhost:6061`
- Admin API 文档：`http://localhost:6060/doc.html`
- App API 文档：`http://localhost:6061/doc.html`

## 配置

开发环境默认读取 `application-dev.yml`，可通过环境变量覆盖：

```bash
export LINGNOW_DB_URL='jdbc:mysql://localhost:3306/lingnow_erp?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
export LINGNOW_DB_USERNAME=root
export LINGNOW_DB_PASSWORD='your-password'
export LINGNOW_REDIS_HOST=localhost
export LINGNOW_REDIS_PORT=9786
export LINGNOW_REDIS_PASSWORD='your-redis-password'
```

生产环境通过 `application-prod.yml` 强制使用环境变量注入数据库、Redis、日志和 API 文档开关。

## 构建

```bash
cd backend
mvn -q -DskipTests package
```

单独构建 Admin：

```bash
cd backend
mvn -q -pl lingnow-admin -am -DskipTests package
```

单独构建 App：

```bash
cd backend
mvn -q -pl lingnow-app -am -DskipTests package
```

构建产物：

- `lingnow-admin/target/lingnow-admin.jar`
- `lingnow-app/target/lingnow-app.jar`

## Docker 交付

Docker 编排文件在仓库根目录：

- `docker-compose.yml`：完整单机模式，包含 MySQL、Redis、Admin、App、Web。
- `docker-compose.host.example.yml`：本机依赖模式，只启动 Admin、App、Web，MySQL/Redis 使用宿主机服务。

默认 Web 端口为 `8090`。

本机依赖模式示例：

```bash
cd ..
cp .env.example .env
cp docker-compose.host.example.yml docker-compose.host.yml
docker compose --env-file .env -f docker-compose.host.yml up -d --build
```

## 验收

仓库根目录提供自动验收脚本：

```bash
cd ..
ADMIN_BASE_URL=http://localhost:8090/admin-api \
APP_BASE_URL=http://localhost:8090/app-api \
DB_NAME=lingnow_erp \
DB_USER=root \
MYSQL_PWD='<db-password>' \
node scripts/release-acceptance.mjs
```

验收覆盖：

- 管理员真实登录。
- 菜单组件和权限绑定。
- 19 条发版验收流程。
- Admin/API/CSV/App 接口。
- Warm-Flow 审批。
- 库存、资金、往来、报表、通知、权限和数据授权。
- `ACCEPT_%` 临时数据清理。
