#!/usr/bin/env bash
# Windows PowerShell users: powershell -NoProfile -ExecutionPolicy Bypass -File .\start.ps1
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Git Bash on Windows uses the native launcher, including process-tree cleanup.
case "${OSTYPE:-}" in
  msys*|cygwin*)
    exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$PROJECT_ROOT/start.ps1")" "$@"
    ;;
esac

if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
for tool in java javac mvn node npm curl; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "[Error] Missing dependency: $tool"
    exit 1
  fi
done
java_major="$(java -version 2>&1 | awk -F '"' '/version/ { split($2, parts, "."); print parts[1] == 1 ? parts[2] : parts[1]; exit }')"
if [[ ! "$java_major" =~ ^[0-9]+$ ]] || [ "$java_major" -lt 25 ]; then
  echo '[Error] JDK 25+ is required. Set JAVA_HOME to your JDK directory.'
  exit 1
fi

# Give each background job a process group so Ctrl+C also stops Java/Node children.
set -m
PIDS=()
cleanup() {
  trap - EXIT
  for service_pid in "${PIDS[@]}"; do
    kill -TERM -- "-$service_pid" 2>/dev/null || true
  done
  for service_pid in "${PIDS[@]}"; do
    wait "$service_pid" 2>/dev/null || true
  done
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

backend_ready() {
  local body
  body="$(curl --max-time 2 -fsS http://127.0.0.1:8080/api/stock/list 2>/dev/null)" || return 1
  [[ "$body" =~ \"code\"[[:space:]]*:[[:space:]]*0 ]] &&
    [[ ! "$body" =~ \"data\"[[:space:]]*:[[:space:]]*\[[[:space:]]*\] ]]
}
frontend_ready() {
  curl --max-time 2 -fsS http://127.0.0.1:5173/ 2>/dev/null | grep -q '/src/main.ts'
}
wait_ready() {
  local service_pid="$1" probe="$2" name="$3"
  for (( attempt=0; attempt<120; attempt++ )); do
    if ! kill -0 "$service_pid" 2>/dev/null; then
      echo "[Error] $name exited before becoming ready."
      exit 1
    fi
    if "$probe"; then return; fi
    sleep 1
  done
  echo "[Error] $name did not become ready within 120 seconds."
  exit 1
}

if [ ! -d "$PROJECT_ROOT/frontend/node_modules" ]; then
  (cd "$PROJECT_ROOT/frontend" && npm ci)
fi
if backend_ready; then
  echo '[1/2] Backend is already running.'
else
  echo '[1/2] Starting backend...'
  (cd "$PROJECT_ROOT/backend" && exec mvn -q spring-boot:run) &
  PIDS+=("$!")
  wait_ready "$!" backend_ready Backend
fi
if frontend_ready; then
  echo '[2/2] Frontend is already running.'
else
  echo '[2/2] Starting frontend...'
  (cd "$PROJECT_ROOT/frontend" && exec npm run dev -- --host 127.0.0.1) &
  PIDS+=("$!")
  wait_ready "$!" frontend_ready Frontend
fi
printf '\nOpen http://127.0.0.1:5173/\n'
if [ "${#PIDS[@]}" -eq 0 ]; then exit 0; fi
echo 'Press Ctrl+C to stop services started by this script.'
while true; do
  for service_pid in "${PIDS[@]}"; do
    if ! kill -0 "$service_pid" 2>/dev/null; then
      echo '[Error] A service exited.'
      exit 1
    fi
  done
  sleep 1
done
