#!/usr/bin/env bash
# 模拟股票交易系统一键启动脚本
# 用法：bash start.sh        （Ctrl+C 会同时停掉前后端）
#
# 依赖：JDK 25+ 与 Maven。若未加入系统 PATH，可在下面两行按需覆盖。

set -u

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---- 环境定位：系统 PATH 找不到就回退到本机已安装的位置 ----
export JAVA_HOME="${JAVA_HOME:-/d/jdk-25/jdk-25.0.2}"
export PATH="$JAVA_HOME/bin:$PATH"

if ! command -v mvn >/dev/null 2>&1; then
  if [ -x "/d/apache-maven-3.9.9/bin/mvn" ]; then
    export PATH="/d/apache-maven-3.9.9/bin:$PATH"
    export M2_HOME="/d/apache-maven-3.9.9"
  fi
fi

echo "=========================================="
echo " 模拟股票交易系统 启动中"
echo "=========================================="
echo " JAVA_HOME : $JAVA_HOME"
echo " Maven     : $(command -v mvn || echo '未找到')"
echo " Node      : $(command -v node || echo '未找到')"
echo "------------------------------------------"

# ---- 依赖检查 ----
missing=0
for cmd in java mvn node npm; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[错误] 未找到命令: $cmd"
    missing=1
  fi
done
if [ "$missing" -ne 0 ]; then
  echo "请先安装缺失的依赖后重试。"
  exit 1
fi

PIDS=()
cleanup() {
  echo ""
  echo "正在停止服务..."
  for pid in "${PIDS[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
  # 兜底：Maven 会 fork 出 Java 子进程，一并清理
  wait 2>/dev/null || true
  echo "已停止。"
}
trap cleanup EXIT INT TERM

# ---- 启动后端 ----
echo "[1/2] 启动后端 (Spring Boot, http://localhost:8080) ..."
( cd "$PROJECT_ROOT/backend" && mvn -q spring-boot:run ) &
PIDS+=($!)

# 等后端就绪，最多 120 秒。
# 注意：Spring Boot 的 Web 容器会先于 DataInitializer 就绪，此时接口能通但股票列表还是空的，
# 所以这里要等到真的能读到股票数据，而不只是端口通了。
echo -n "      等待后端就绪"
for _ in $(seq 1 120); do
  body=$(curl -sf "http://localhost:8080/api/stock/list" 2>/dev/null || true)
  if echo "$body" | grep -q '"code":0' && ! echo "$body" | grep -q '"data":\[\]'; then
    echo " -> 就绪"
    break
  fi
  echo -n "."
  sleep 1
done

# ---- 启动前端 ----
echo "[2/2] 启动前端 (Vite) ..."
if [ ! -d "$PROJECT_ROOT/frontend/node_modules" ]; then
  echo "      首次运行，安装前端依赖 (npm install) ..."
  ( cd "$PROJECT_ROOT/frontend" && npm install ) || { echo "[错误] npm install 失败"; exit 1; }
fi
( cd "$PROJECT_ROOT/frontend" && npm run dev ) &
PIDS+=($!)

echo ""
echo "=========================================="
echo " 服务已启动"
echo "------------------------------------------"
echo " 前端页面   : http://localhost:5173"
echo " 登录页     : http://localhost:5173/login"
echo " 后端 API   : http://localhost:8080/api"
echo " H2 控制台  : http://localhost:8080/h2-console"
echo "              JDBC URL: jdbc:h2:mem:stockdb"
echo "              用户名: sa    密码: (留空)"
echo "------------------------------------------"
echo " 默认账号   : user001 / 123456"
echo "               admin  / admin123"
echo "------------------------------------------"
echo " 按 Ctrl+C 停止全部服务"
echo "=========================================="

wait
