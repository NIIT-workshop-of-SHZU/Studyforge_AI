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

建议准备：

- JDK 17
- Maven 3.9+
- Node.js 20.19+
- MySQL 8 或 MariaDB 10.6+
- Nginx，用于托管两个前端站点并反向代理 API

## 2. 数据库初始化

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

本仓库提供了一个 systemd 示例：

```text
deploy/systemd/studyforge-api.service.example
```

示例用 Maven 启动 Jetty 插件运行 Spring MVC API，适合当前 Maven 多模块工程继续保持前后端分离。

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

构建命令：

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
