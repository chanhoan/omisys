#!/usr/bin/env bash
#
# 원격 의존성 서버(EC2)로 SSH 포트 포워딩을 연다.
# 로컬 IDE에서 local 프로파일로 애플리케이션을 실행하기 전에 먼저 기동한다.
#
# 접속 정보는 저장소 루트의 .env.local 에서 읽는다:
#   OMISYS_DEP_HOST=ubuntu@<ip>          (필수)
#   OMISYS_DEP_KEY=<키 파일 절대경로>     (생략 시 아래 위치를 자동 탐색)
#     <repo>/omisys.pem | ~/.ssh/omisys.pem | ~/Downloads/omisys.pem | ~/keys/omisys.pem
# 환경변수로 직접 넘기면 그 값이 우선한다.
#
# 포워딩 포트: 3306(MySQL, LOCAL_MYSQL_PORT 로 변경 가능) / 6379-6383(Redis 5개) / 29092(Kafka) / 9200(ES)
# 종료: Ctrl+C
#
# 자세한 절차는 docs/development/local-setup.md 참조.

set -euo pipefail

# 저장소 루트의 .env.local 을 먼저 읽는다. 환경변수로 직접 넘긴 값이 우선한다.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [ -f "$REPO_ROOT/.env.local" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$REPO_ROOT/.env.local"
  set +a
fi

EC2_HOST="${OMISYS_DEP_HOST:-}"

# 키 파일: 명시된 경로가 없으면 흔한 위치를 순서대로 찾는다.
# PC 마다 경로가 달라도 .pem 만 놓아두면 그대로 동작한다.
KEY_PATH="${OMISYS_DEP_KEY:-}"
if [ -z "$KEY_PATH" ]; then
  for candidate in     "$REPO_ROOT/omisys.pem"     "$HOME/.ssh/omisys.pem"     "$HOME/Downloads/omisys.pem"     "$HOME/keys/omisys.pem"
  do
    if [ -f "$candidate" ]; then
      KEY_PATH="$candidate"
      echo "[info] 키 파일 자동 탐색: $KEY_PATH"
      break
    fi
  done
fi
# 로컬에 MySQL 이 이미 3306 을 잡고 있으면 bind 가 실패한다. 그럴 때만 바꾼다.
# 애플리케이션도 같은 값을 받아야 한다 (*-local.yml 의 ${LOCAL_MYSQL_PORT:3306}).
LOCAL_MYSQL_PORT="${LOCAL_MYSQL_PORT:-3306}"

if [ -z "$EC2_HOST" ]; then
  echo "[error] OMISYS_DEP_HOST 가 설정되지 않았습니다." >&2
  echo "        .env.local 에 OMISYS_DEP_HOST=ubuntu@1.2.3.4 를 추가하십시오." >&2
  exit 1
fi

if [ -z "$KEY_PATH" ]; then
  echo "[error] OMISYS_DEP_KEY 가 설정되지 않았습니다." >&2
  echo "        아래 중 한 곳에 omisys.pem 을 두거나 .env.local 에 경로를 적으십시오:" >&2
  echo "          <repo>/omisys.pem  |  ~/.ssh/omisys.pem  |  ~/Downloads/omisys.pem" >&2
  exit 1
fi

if [ ! -f "$KEY_PATH" ]; then
  echo "[error] 키 파일을 찾을 수 없습니다: $KEY_PATH" >&2
  exit 1
fi

echo "[info] 터널 연결: $EC2_HOST"
echo "[info] 로컬 포트 ${LOCAL_MYSQL_PORT} / 6379-6383 / 29092 / 9200 -> 원격 의존성"
echo "[info] 종료하려면 Ctrl+C"

exec ssh -i "$KEY_PATH" -N \
  -o ServerAliveInterval=30 \
  -L ${LOCAL_MYSQL_PORT}:localhost:3306 \
  -L 6379:localhost:6379 \
  -L 6380:localhost:6380 \
  -L 6381:localhost:6381 \
  -L 6382:localhost:6382 \
  -L 6383:localhost:6383 \
  -L 29092:localhost:29092 \
  -L 9200:localhost:9200 \
  "$EC2_HOST"
