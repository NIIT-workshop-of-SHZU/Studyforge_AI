# Staging 自动审查与部署

本仓库推荐使用 `staging` 作为测试集成分支：

```text
feature/* -> pull request -> staging -> 自动审查 -> 合并后自动部署 staging
staging -> main -> 稳定版本
```

## GitHub Secrets

在 GitHub 仓库中创建 Environment：

```text
Settings -> Environments -> New environment -> staging
```

然后在 `staging` Environment 里配置：

```text
STAGING_DEPLOY_HOST       Staging 服务器 IP 或域名
STAGING_DEPLOY_PORT       SSH 端口，例如 22
STAGING_DEPLOY_USER       SSH 用户，例如 deploy
STAGING_DEPLOY_SSH_KEY    SSH 私钥内容
STAGING_KNOWN_HOSTS       ssh-keyscan 得到的 known_hosts 内容
```

如果这些 Secrets 尚未配置，workflow 会完成自动审查并跳过部署。

## 服务器默认路径

`scripts/deploy_staging.sh` 默认使用：

```text
/opt/studyforge-staging
/var/www/studyforge-staging/knowledge
/var/www/studyforge-staging/portal
/opt/tomcat-staging/webapps/ROOT.war
tomcat-staging.service
```

如果服务器路径不同，可以修改 `scripts/deploy_staging.sh`，或在远程 shell 中设置这些变量：

```text
APP_ROOT
FRONTEND_KNOWLEDGE_DIR
FRONTEND_PORTAL_DIR
BACKEND_WAR_DIR
BACKEND_CONTEXT
BACKEND_SERVICE
HEALTH_URL
SUDO
```

默认脚本会使用 `sudo -n` 执行需要权限的命令。部署用户需要具备免密码 sudo 权限，或者将目标目录授权给部署用户并设置：

```text
SUDO=
```

发布包会包含后端 WAR、两个前端 dist、`sql/` 和数据库导入脚本。服务器如需每次 staging 部署自动执行非破坏性数据库迁移，在 `/etc/studyforge/staging.env` 中配置：

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

自动迁移不会导入 `002_seed_data.sql`，也不会清空业务数据。

## 分支保护建议

建议给 `staging` 分支开启：

```text
Require a pull request before merging
Require status checks to pass before merging
Require branches to be up to date before merging
```

必需检查选择：

```text
Automated Review
SQL MySQL 8 Import
```

## 重要安全说明

真实 API Key 不应提交到仓库。仓库只保留 `api配置.example.md`，本地真实配置请放在被 `.gitignore` 忽略的 `api配置.md` 中。
