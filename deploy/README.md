# CampusLink 部署说明（腾讯云 CVM 120.53.229.121）

与服务器上既有的 **walissh** 栈完全隔离，独占一套端口与容器。

## 端口规划

| 服务 | 容器 | 宿主机端口 | 访问 |
| --- | --- | --- | --- |
| 前端 Nginx | `campuslink-frontend` | **8000** | http://120.53.229.121:8000 |
| 后端 Spring Boot | `campuslink-backend` | **18080** | http://120.53.229.121:18080/api |
| MySQL 8.0 | `campuslink-mysql` | **23306** | 库 `campuslink`，root/`campuslink123` |

> 前端 Nginx 已把 `/api/` 反代到后端，正常使用只需开放 **8000**。18080/23306 仅调试用，**注意腾讯云安全组不要把 23306 暴露公网**。

## 首次部署

```bash
# 1. 凭证（密码不进 shell 历史）
mkdir -p ~/.claude/skills/tencent-server
printf '你的SSH密码' > ~/.claude/skills/tencent-server/.credentials
chmod 600 ~/.claude/skills/tencent-server/.credentials

# 2. 一键部署
bash deploy/deploy.sh
```

## 数据库初始化

`campuslink-mysql` **首次启动**自动执行 `sql/campuslink.sql`（建库+种子数据）与 `sql/v2_extension.sql`（v2 增量）。
若需重导：`docker compose down -v` 删除数据卷后重新 `up`（会清空数据）。

## 演示账号

`admin / admin123`（管理员）、`alice|bob|carol / admin123`（学生）。
> 注：项目根 README 写的演示密码 `123456` 与实际种子数据不符，真实密码是 `admin123`。

## 常用运维

```bash
cd /opt/campuslink/deploy
docker compose ps                 # 状态
docker compose logs -f campuslink-backend   # 后端日志
docker compose restart campuslink-backend   # 重启后端
docker compose up -d --build      # 改代码后重建
docker compose down               # 停止（保留数据卷）
```
