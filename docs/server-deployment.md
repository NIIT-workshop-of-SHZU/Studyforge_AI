# StudyForge AI 服务器部署准备

本文档对应当前项目形态：

```text
Vue 3 前端静态站点
    -> Axios / Fetch
Spring MVC Controller 返回 JSON
    -> Service
    -> MyBatis Mapper
    -> MySQL / MariaDB
```

用户端和管理端都是独立 Vue 应用，后端只提供 `/api/v1/**` JSON 接口。

## 1. 服务器依赖

当前推荐的自动部署方式是 GitHub Actions 构建发布包，再通过 SSH 上传到服务器。服务器不需要稳定访问 GitHub，也不需要在服务器上安装 Maven、Node.js 或 clone 仓库。

服务器运行时依赖：

- JDK 17
- Tomcat 10.1.x，用于运行 `studyforge-webapi.war`
- MySQL 8.x；本地开发可用 MariaDB，但服务器以 MySQL 8 为准
- MySQL 客户端命令 `mysql`，用于自动部署时执行非破坏性迁移
- Nginx，用于托管两个前端站点并反向代理 API
- rsync、curl、OpenSSH Server

GitHub Actions / 本机构建环境依赖：

- Maven 3.9+
- Node.js 20.19+

Tomcat 不是 Spring MVC 代码本身唯一能用的运行方式，但当前 `scripts/deploy_staging.sh` 明确按 WAR 包部署：它会把 `studyforge-webapi.war` 放到 `/opt/tomcat-staging/webapps/ROOT.war`，然后重启 `tomcat-staging.service`。如果不用 Tomcat，就需要同步改部署脚本和 systemd 服务。

## 2. 数据库初始化

数据库版本标准：

```text
服务器 / staging / production: MySQL 8.x，目前服务器已验证 MySQL 8.0.45
当前本机开发环境: MariaDB 12.x，目前本机已验证 MariaDB 12.2.2
```

仓库 SQL 必须以 MySQL 8 为基准，同时保持 MariaDB 开发环境可导入。不要使用 MariaDB-only 语法，例如 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`。需要幂等加列时，使用 `information_schema + PREPARE/EXECUTE` 这种 MySQL 8 与 MariaDB 都支持的写法。

新服务器先创建数据库账号，再导入结构和非破坏性迁移：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS studyforge_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p -e "CREATE USER IF NOT EXISTS 'studyforge'@'%' IDENTIFIED BY 'change-this-password';"
mysql -uroot -p -e "GRANT ALL PRIVILEGES ON studyforge_ai.* TO 'studyforge'@'%'; FLUSH PRIVILEGES;"

DB_NAME=studyforge_ai \
DB_USER=studyforge \
DB_PASSWORD=change-this-password \
DB_HOST=127.0.0.1 \
./scripts/import_local_db.sh
```

`scripts/import_local_db.sh` 会导入 `001_schema.sql`，并自动执行 `003_*.sql`、`004_*.sql` 这类非破坏性迁移。不要在已有业务数据的服务器上执行 `RESET_SEED=1`。

SQL 脚本不再内置 `CREATE DATABASE` 或 `USE`，必须由命令行显式选择数据库。这样同一套脚本可以稳定用于本地 `test_studyforge_ai_v2`、staging `studyforge_ai` 或 production 数据库，不会在导入过程中切换到错误库。

自动部署时，发布包会包含 `sql/` 和 `scripts/import_local_db.sh`。如果服务器上配置了 `DB_MIGRATE=1`，`scripts/deploy_staging.sh` 会在替换 WAR 和前端文件前自动执行非破坏性迁移：

```bash
sudo mkdir -p /etc/studyforge
sudo vim /etc/studyforge/staging.env
```

示例内容：

```bash
DB_MIGRATE=1
DB_CLIENT=mysql
DB_NAME=studyforge_ai
DB_USER=studyforge
DB_PASSWORD='change-this-password'
DB_HOST=127.0.0.1
DB_PORT=3306
CREATE_DATABASE=0
HEALTH_URL=http://127.0.0.1:8080/api/v1/health
```

这个文件需要让执行部署脚本的用户可读，例如只允许 `deploy` 用户或部署用户组读取，不要设成全局可写。

`CREATE_DATABASE=0` 表示数据库由 root 一次性创建，部署账号只负责在已存在的库里建表和迁移字段，避免给应用数据库账号过大的全局权限。

## 3. 后端运行配置

后端默认仍可读取 `studyforge-webapi/src/main/resources/jdbc.properties`，服务器上建议用环境变量覆盖：

```text
JDBC_URL=jdbc:mysql://127.0.0.1:3306/studyforge_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
JDBC_USERNAME=studyforge
JDBC_PASSWORD=change-this-password
JDBC_MAXIMUM_POOL_SIZE=20
JDBC_MINIMUM_IDLE=4
STUDYFORGE_UPLOAD_DIR=/var/lib/studyforge/uploads
```

`STUDYFORGE_UPLOAD_DIR` 会保存用户上传图片和 AI 生成封面，实际图片目录为：

```text
/var/lib/studyforge/uploads/images
```

这个目录需要持久化和备份，不能随着前端静态文件发布一起清空。

当前 staging 自动部署默认重启服务器上的 `tomcat-staging.service`。这个服务需要提前在服务器上配置好 Tomcat 10.1.x，并把 `BACKEND_WAR_DIR` 指向 Tomcat 的 `webapps` 目录。

仓库中还有一个 Jetty/Maven 运行方式的 systemd 示例：

```text
deploy/systemd/studyforge-api.service.example
```

这个示例适合本地或手动服务器构建场景，不是当前 GitHub Actions staging 自动部署的默认路径。

## 4. 前端构建

两个前端都通过 `VITE_API_BASE_URL` 指向 API。生产环境如果用 Nginx 同域反代，保持默认即可：

```text
VITE_API_BASE_URL=/api/v1
```

示例文件：

```text
studyforge-frontend/apps/knowledge-web/.env.production.example
studyforge-frontend/apps/portal-web/.env.production.example
```

GitHub Actions 会执行下面的构建命令并上传构建产物，服务器只接收 `dist` 目录：

```bash
cd studyforge-frontend
npm ci
npm run build
```

构建产物：

```text
studyforge-frontend/apps/knowledge-web/dist
studyforge-frontend/apps/portal-web/dist
```

## 5. 一键打包发布物

可以用仓库脚本生成发布包：

```bash
./scripts/build_release.sh
```

发布包会包含：

```text
backend/studyforge-webapi.war
frontend/knowledge
frontend/portal
config/jdbc.properties.example
```

## 6. Nginx

Nginx 示例在：

```text
deploy/nginx/studyforge.conf.example
```

建议用两个域名：

```text
studyforge.example.com        用户端知识平台
admin.studyforge.example.com  管理端
```

两个站点都把 `/api/v1/` 反向代理到后端 `127.0.0.1:8080`。示例里 API 代理超时设置为 220 秒，和当前 AI 功能 200 秒超时相匹配。

## 7. 管理端模型设置

部署完成后登录管理端，进入：

```text
AI 与模型设置
```

可以维护三组配置：

- 文本 AI：摘要、复习卡片、问答、AI 排版
- 语音服务：语音输入和转写
- 封面生图：用户发布文章时的“生成封面”

后端读取的配置键：

```text
ai.base_url
ai.api_key
ai.chat_model
voice.base_url
voice.api_key
voice.model
voice.name
image.base_url
image.api_key
image.model
image.size
```

这些配置保存到 `integration_settings` 表。API Key 在管理端读取时会被遮罩，保存遮罩值不会覆盖数据库里的真实密钥。

## 8. 部署后检查

后端：

```bash
curl -fsS http://127.0.0.1:8080/api/v1/health
```

前端经过 Nginx：

```bash
curl -I http://studyforge.example.com/
curl -I http://admin.studyforge.example.com/
curl -fsS http://studyforge.example.com/api/v1/health
```

重点确认：

- 管理端可以登录并打开“社区管理”
- 管理端“AI 与模型设置”能读取和保存配置
- 用户端可以发帖、上传图片、生成封面
- `/var/lib/studyforge/uploads/images` 中能看到新上传或生成的图片文件
