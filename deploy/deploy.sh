#!/usr/bin/env bash
# CampusLink 一键部署到腾讯云 CVM（120.53.229.121）
# 用法：bash deploy/deploy.sh
# 前提：~/.claude/skills/tencent-server/.credentials 内含 SSH 密码（chmod 600）
set -euo pipefail

HOST="120.53.229.121"
USER="root"
REMOTE_DIR="/opt/campuslink"
CRED="$HOME/.claude/skills/tencent-server/.credentials"

cd "$(dirname "$0")/.."   # 切到仓库根目录

if [[ ! -f "$CRED" ]]; then
  echo "❌ 未找到凭证文件 $CRED，请先写入 SSH 密码并 chmod 600" >&2
  exit 1
fi
PASS="$(cat "$CRED")"

SSH="sshpass -p $PASS ssh -o StrictHostKeyChecking=no -o ConnectTimeout=15 $USER@$HOST"

echo "==> 1/4 打包源码（排除 node_modules/target/.git/dist）"
TARBALL="/tmp/campuslink-src.tar.gz"
tar --exclude='./.git' \
    --exclude='./frontend/node_modules' \
    --exclude='./frontend/dist' \
    --exclude='./backend/target' \
    -czf "$TARBALL" .

echo "==> 2/4 上传到 $HOST:$REMOTE_DIR"
$SSH "mkdir -p $REMOTE_DIR"
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$TARBALL" "$USER@$HOST:/tmp/campuslink-src.tar.gz"
$SSH "rm -rf $REMOTE_DIR/* && tar -xzf /tmp/campuslink-src.tar.gz -C $REMOTE_DIR"

echo "==> 3/4 在服务器上构建并启动容器"
$SSH "cd $REMOTE_DIR/deploy && docker compose build && docker compose up -d"

echo "==> 4/4 状态检查"
$SSH "docker ps --filter name=campuslink --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'"

echo "✅ 完成：前端 http://$HOST:8000  后端 http://$HOST:18080/api"
