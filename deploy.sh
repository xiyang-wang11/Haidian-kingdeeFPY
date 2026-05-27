#!/bin/bash
# 海典-金蝶开票对接程序 一键部署脚本
# 用法：./deploy.sh <服务器IP> <SSH用户> [SSH端口]
# 示例：./deploy.sh 192.168.1.100 root 22
#
# 前置条件：
#   本机已安装 Maven、JDK 11+
#   服务器已安装 JDK 11+、MySQL
#   服务器已创建数据库并执行过 init.sql
#   当前目录存在 application-prod.yml

set -e

SERVER_IP="${1:?请传入服务器IP，例如: ./deploy.sh 192.168.1.100 root}"
SSH_USER="${2:?请传入SSH用户，例如: ./deploy.sh 192.168.1.100 root}"
SSH_PORT="${3:-22}"
DEPLOY_DIR="/opt/invoice-assistant"
APP_NAME="invoice-assistant-1.0.0.jar"
PROD_CONFIG="application-prod.yml"

echo "====== [1/4] 编译打包 ======"
mvn clean package -DskipTests
echo "打包完成：target/${APP_NAME}"

echo "====== [2/4] 上传文件到服务器 ======"
ssh -p "${SSH_PORT}" "${SSH_USER}@${SERVER_IP}" "mkdir -p ${DEPLOY_DIR}"
scp -P "${SSH_PORT}" "target/${APP_NAME}" "${SSH_USER}@${SERVER_IP}:${DEPLOY_DIR}/"
scp -P "${SSH_PORT}" "${PROD_CONFIG}" "${SSH_USER}@${SERVER_IP}:${DEPLOY_DIR}/application.yml"
echo "文件上传完成"

echo "====== [3/4] 停止旧进程 ======"
ssh -p "${SSH_PORT}" "${SSH_USER}@${SERVER_IP}" "
  PID=\$(pgrep -f '${APP_NAME}' || true)
  if [ -n \"\$PID\" ]; then
    echo \"停止旧进程 PID: \$PID\"
    kill \"\$PID\"
    sleep 3
  else
    echo \"无旧进程运行\"
  fi
"

echo "====== [4/4] 启动新版本 ======"
ssh -p "${SSH_PORT}" "${SSH_USER}@${SERVER_IP}" "
  cd ${DEPLOY_DIR}
  nohup java -jar ${APP_NAME} \
    --spring.config.location=./application.yml \
    > app.log 2>&1 &
  echo \"启动中，PID: \$!\"
  sleep 5
  if pgrep -f '${APP_NAME}' > /dev/null; then
    echo '启动成功'
    tail -20 app.log
  else
    echo '启动失败，查看日志：'
    tail -50 app.log
    exit 1
  fi
"

echo ""
echo "====== 部署完成 ======"
echo "服务地址：http://${SERVER_IP}:8080"
echo "查看日志：ssh -p ${SSH_PORT} ${SSH_USER}@${SERVER_IP} 'tail -f ${DEPLOY_DIR}/app.log'"
