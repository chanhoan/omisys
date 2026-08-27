#!/usr/bin/env bash
#
# 원격 의존성 서버(EC2)로 SSH 포트 포워딩을 연다.
# 로컬 IDE에서 local 프로파일로 애플리케이션을 실행하기 전에 먼저 기동한다.
#
# 사용 전 환경변수 설정:
#   export OMISYS_DEP_HOST="ec2-user@<ip>"
#   export OMISYS_DEP_KEY="$HOME/keys/omisys.pem"
#
# 포워딩 포트: 3306(MySQL) / 6379(Redis) / 29092(Kafka) / 9200(Elasticsearch)
# 종료: Ctrl+C
#
# 자세한 절차는 docs/development/local-setup.md 참조.

set -euo pipefail

EC2_HOST="${OMISYS_DEP_HOST:-}"
KEY_PATH="${OMISYS_DEP_KEY:-}"

if [ -z "$EC2_HOST" ]; then
  echo "[error] OMISYS_DEP_HOST 가 설정되지 않았습니다." >&2
  echo "        예) export OMISYS_DEP_HOST=\"ec2-user@1.2.3.4\"" >&2
  exit 1
fi

if [ -z "$KEY_PATH" ]; then
  echo "[error] OMISYS_DEP_KEY 가 설정되지 않았습니다." >&2
  echo "        예) export OMISYS_DEP_KEY=\"\$HOME/keys/omisys.pem\"" >&2
  exit 1
fi

if [ ! -f "$KEY_PATH" ]; then
  echo "[error] 키 파일을 찾을 수 없습니다: $KEY_PATH" >&2
  exit 1
fi

echo "[info] 터널 연결: $EC2_HOST"
echo "[info] 로컬 포트 3306 / 6379 / 29092 / 9200 -> 원격 의존성"
echo "[info] 종료하려면 Ctrl+C"

exec ssh -i "$KEY_PATH" -N \
  -o ServerAliveInterval=30 \
  -L 3306:localhost:3306 \
  -L 6379:localhost:6379 \
  -L 29092:localhost:29092 \
  -L 9200:localhost:9200 \
  "$EC2_HOST"
