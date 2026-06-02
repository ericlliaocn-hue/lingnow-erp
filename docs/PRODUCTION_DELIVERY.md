# LingNow ERP Production Delivery

## Scope

This delivery package targets a single-machine Docker Compose deployment for LingNow ERP.

Included services:

- MySQL 8
- Redis 7
- Admin backend
- App backend
- Nginx web gateway for admin UI, H5, API proxy, and file access

For local validation, a host dependency mode is also provided. It runs only Admin, App, and Web containers while using the host machine's MySQL and Redis, avoiding MySQL/Redis image pulls and data volumes.

No demo ERP business data is inserted. Verification data must use a temporary `DELIVERY_%` prefix and must be physically cleaned after acceptance.

## Production Files

- `.env.example`: production environment template.
- `docker-compose.yml`: single-machine service topology.
- `docker-compose.host.example.yml`: local host dependency topology; copy to ignored `docker-compose.host.yml` before use.
- `docker/backend/admin.Dockerfile`: Admin backend image.
- `docker/backend/app.Dockerfile`: App backend image.
- `docker/web/Dockerfile`: admin UI and H5 web image.
- `docker/web/nginx.conf`: static web and API proxy config.
- `docker/mysql/conf.d/lingnow.cnf`: MySQL charset/timezone tuning.
- `scripts/prod-up.sh`: guarded production startup.
- `scripts/prod-down.sh`: production shutdown.
- `scripts/prod-logs.sh`: production log tail.
- `scripts/backup-prod.sh`: MySQL and file backup.
- `scripts/restore-prod.sh`: MySQL and file restore.

## Startup

Full Compose mode starts MySQL, Redis, Admin, App, and Web:

```bash
cp .env.example .env
```

Edit `.env` and replace every `CHANGE_ME` value.

```bash
./scripts/prod-up.sh
```

The startup script refuses to run when `.env` is missing or still contains `CHANGE_ME`.

Local host dependency mode starts only Admin, App, and Web. MySQL and Redis must already be running on the host:

```bash
cp .env.example .env
cp docker-compose.host.example.yml docker-compose.host.yml
docker compose --env-file .env -f docker-compose.host.yml up -d --build
```

Keep `docker-compose.host.yml` and `.env` local because they contain machine-specific ports and secrets.

## Health Checks

Expected default endpoints after Compose startup:

- Web gateway: `http://localhost:8090/healthz`
- Admin UI: `http://localhost:8090/admin/`
- H5: `http://localhost:8090/h5/`
- Admin API proxy: `http://localhost:8090/admin-api/welcome`
- App API proxy: `http://localhost:8090/app-api/welcome`

Backend containers also expose internal health checks against `/welcome`.

## Acceptance

Local non-Docker acceptance:

```bash
node scripts/acceptance-check.mjs
```

Docker host dependency acceptance:

```bash
ADMIN_BASE_URL=http://localhost:8090/admin-api \
APP_BASE_URL=http://localhost:8090/app-api \
MYSQL_PWD=<host-db-password> \
node scripts/acceptance-check.mjs
```

Latest local result on 2026-06-02:

- Passed admin login.
- Passed menu/component audit.
- Passed `230` active menu/button permission binding audit.
- Passed `89` Admin/API/CSV endpoints.
- Passed app unauthenticated auth guard.
- Passed real delivery business flow:
  - create product category, unit, brand, customer, supplier, warehouse, account, product.
  - create purchase bill, submit approval, approve through Warm-Flow, verify stock increases.
  - create sale bill, submit approval, approve through Warm-Flow, verify stock decreases and fund flow is created.
  - create receipt bill, submit approval, approve through Warm-Flow, verify finance audit and fund flow.
  - unaudit/delete temporary bills.
  - physically clean all `DELIVERY_%` verification data.
- Passed Docker host dependency runtime verification:
  - Docker Desktop and Docker Compose are installed and usable.
  - Only `lingnow-erp-admin`, `lingnow-erp-app`, and `lingnow-erp-web` containers were started.
  - No MySQL or Redis containers or images were present after local validation.
  - `http://localhost:8090/healthz`, `/admin-api/welcome`, and `/app-api/welcome` returned `200`.
  - Admin/App container restart completed without `GracefulShutdownCallback`, `NoClassDefFoundError`, or `ClassNotFoundException`.
  - Nginx proxy remained healthy after Admin/App restart.

## Data Policy

- Do not insert fake or demo ERP rows into initialization SQL.
- Temporary acceptance data must use `DELIVERY_%`.
- Temporary acceptance data must be physically removed after the test.
- The acceptance script performs a cleanup before and after the run.

## Security Defaults

- Production API docs are disabled by default with `LINGNOW_API_DOC_ENABLED=false`.
- Production log level defaults to `INFO`.
- File fallback path is environment driven through `LINGNOW_FILE_BASE_PATH`.
- `.env` is intentionally not committed.

## Browser Acceptance

- External visible Chrome is the only valid browser verification path for this project.
- Do not use Codex in-app browser.
- Do not use headless browser.
- API acceptance may rotate the `admin` token, so do not run the same-account API login while a visible Chrome login session is being browser-tested.
- Latest external Chrome visible-window pass on 2026-06-02:
  - Admin login at `http://localhost:8090` passed.
  - `73` Admin routes opened without 404, blank page, or console error.
  - Product list search/reset and add dialog passed.
  - Sale add form and receipt add dialog required-field validation passed.
  - Sale analysis report rendered without console errors.
  - `7` H5 routes under `http://localhost:8090/h5/` opened without console errors.
