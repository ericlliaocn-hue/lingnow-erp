#!/bin/bash

# 配置
SERVER_IP="49.235.64.37"
SERVER_USER="root"
REMOTE_PATH="/www/lingnow/dist"
# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "=== 开始部署 Admin UI ==="

# 1. 构建
echo "[1/3] 正在构建项目..."
# 已经在当前目录了
npm install
npm run build

if [ $? -ne 0 ]; then
    echo "构建失败！"
    exit 1
fi

# 2. 打包
echo "[2/3] 正在打包..."
# 进入 dist 目录打包，这样解压时直接就是文件，不包含 dist 目录本身
if [ ! -d "dist" ]; then
    echo "错误：构建未生成 dist 目录"
    exit 1
fi

cd dist
tar -czf ../admin-ui.tar.gz .
cd ..

# 3. 部署
echo "[3/3] 正在上传并解压..."
# 确保远程目录存在
ssh $SERVER_USER@$SERVER_IP "mkdir -p $REMOTE_PATH"
# 上传
scp admin-ui.tar.gz $SERVER_USER@$SERVER_IP:$REMOTE_PATH/
# 解压并清理
ssh $SERVER_USER@$SERVER_IP "cd $REMOTE_PATH && tar -xzf admin-ui.tar.gz && rm admin-ui.tar.gz"

# 清理本地压缩包
rm admin-ui.tar.gz

echo "=== Admin UI 部署完成 ==="
