#!/bin/bash

# 配置
SERVER_IP="49.235.64.37"
SERVER_USER="root"
# 部署到业务端 H5 对应的目录
REMOTE_PATH="/www/lingnow/business_h5"

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "=== 开始部署业务端 H5 ==="

# 1. 构建
echo "[1/3] 正在构建项目..."
npm install
npm run build:h5

if [ $? -ne 0 ]; then
    echo "构建失败！"
    exit 1
fi

# 2. 打包
echo "[2/3] 正在打包..."
BUILD_PATH="dist/build/h5"
if [ ! -d "$BUILD_PATH" ]; then
    echo "错误：构建未生成 $BUILD_PATH 目录"
    exit 1
fi

cd $BUILD_PATH
tar -czf ../../../lingnow-business-h5.tar.gz .
cd ../../../

# 3. 部署
echo "[3/3] 正在上传并解压..."
# 确保远程目录存在
ssh $SERVER_USER@$SERVER_IP "mkdir -p $REMOTE_PATH"
# 上传
scp lingnow-business-h5.tar.gz $SERVER_USER@$SERVER_IP:$REMOTE_PATH/
# 解压并清理
ssh $SERVER_USER@$SERVER_IP "cd $REMOTE_PATH && tar -xzf lingnow-business-h5.tar.gz && rm lingnow-business-h5.tar.gz"

# 清理本地压缩包
rm lingnow-business-h5.tar.gz

echo "=== 业务端 H5 部署完成 ==="
