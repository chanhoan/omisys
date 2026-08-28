#!/usr/bin/env bash
#
# 원격 의존성 서버(EC2)로 SSH 포트 포워딩을 연다.
# 로컬 IDE에서 local 프로파일로 애플리케이션을 실행하기 전에 먼저 기동한다.
#
# 접속 정보는 저장소 루트의 .env.local 에서 읽는다:
#   OMISYS_DEP_HOST=ubuntu@<ip>          (필수)
# 환경변수로 직접 넘기면 그 값이 우선한다.
#
# SSH 키는 저장소 루트의 omisys.pem 으로 고정한다. 다른 위치는 보지 않는다.
#
# 포워딩 포트: 3306(MySQL, LOCAL_MYSQL_PORT 로 변경 가능) / 6379-6383(Redis 5개) / 29092(Kafka) / 9200(ES)
# 종료: Ctrl+C
#
# WSL 에서 Windows 드라이브(/mnt/c) 위의 키는 권한이 0777 로 고정되어 SSH 가 거부한다.
# 그 경우 ~/.ssh/omisys-tunnel.pem 으로 600 사본을 만들어 자동으로 사용한다.
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

# 키 파일은 저장소 루트로 고정한다. PC 마다 여기에 omisys.pem 만 놓으면 된다.
KEY_PATH="$REPO_ROOT/omisys.pem"
# 로컬에 MySQL 이 이미 3306 을 잡고 있으면 bind 가 실패한다. 그럴 때만 바꾼다.
# 애플리케이션도 같은 값을 받아야 한다 (*-local.yml 의 ${LOCAL_MYSQL_PORT:3306}).
LOCAL_MYSQL_PORT="${LOCAL_MYSQL_PORT:-3306}"

if [ -z "$EC2_HOST" ]; then
  echo "[error] OMISYS_DEP_HOST 가 설정되지 않았습니다." >&2
  echo "        .env.local 에 OMISYS_DEP_HOST=ubuntu@1.2.3.4 를 추가하십시오." >&2
  exit 1
fi

if [ ! -f "$KEY_PATH" ]; then
  echo "[error] SSH 키가 없습니다: $KEY_PATH" >&2
  echo "        omisys.pem 을 저장소 루트에 두십시오. (.gitignore 대상이라 커밋되지 않습니다)" >&2
  exit 1
fi

# WSL 은 /mnt/c 를 metadata 없이 마운트하므로 Windows 드라이브 위의 파일은
# 항상 0777 로 보이고 chmod 도 먹지 않는다. OpenSSH 는 그런 키를 거부한다
# ("UNPROTECTED PRIVATE KEY FILE" / "bad permissions"). 권한을 고칠 수 없으면
# 리눅스 파일시스템에 600 사본을 만들어 그 사본으로 접속한다.
key_mode() {
  stat -c '%a' "$1" 2>/dev/null || stat -f '%Lp' "$1" 2>/dev/null
}

chmod 600 "$KEY_PATH" 2>/dev/null || true
KEY_MODE="$(key_mode "$KEY_PATH" || true)"

if [ -n "$KEY_MODE" ] && [ "$KEY_MODE" != "600" ] && [ "$KEY_MODE" != "400" ]; then
  SAFE_KEY="$HOME/.ssh/omisys-tunnel.pem"
  mkdir -p "$HOME/.ssh"
  chmod 700 "$HOME/.ssh" 2>/dev/null || true

  if ! cmp -s "$KEY_PATH" "$SAFE_KEY" 2>/dev/null; then
    ( umask 077 && cp "$KEY_PATH" "$SAFE_KEY" )
  fi
  chmod 600 "$SAFE_KEY" 2>/dev/null || true

  SAFE_MODE="$(key_mode "$SAFE_KEY" || true)"
  if [ "$SAFE_MODE" != "600" ] && [ "$SAFE_MODE" != "400" ]; then
    echo "[error] 키 권한을 600 으로 만들 수 없습니다: $SAFE_KEY (현재 0${SAFE_MODE:-???})" >&2
    echo "        홈 디렉터리가 Windows 드라이브 위에 있는지 확인하십시오." >&2
    exit 1
  fi

  echo "[info] 키 권한이 0${KEY_MODE} 라 SSH 가 거부합니다. 600 사본을 사용합니다: $SAFE_KEY"
  KEY_PATH="$SAFE_KEY"
fi

echo "[info] 터널 연결: $EC2_HOST"
echo "[info] 로컬 포트 ${LOCAL_MYSQL_PORT} / 6379-6383 / 29092 / 9200 -> 원격 의존성"
echo "[info] 종료하려면 Ctrl+C"

# ExitOnForwardFailure: 포트를 하나라도 못 잡으면 즉시 끝낸다.
# 이게 없으면 절반만 포워딩된 채로 살아남아, 애플리케이션이 엉뚱한 곳에서 죽는다.
STATUS=0
ssh -i "$KEY_PATH" -N \
  -o ServerAliveInterval=30 \
  -o ExitOnForwardFailure=yes \
  -L ${LOCAL_MYSQL_PORT}:localhost:3306 \
  -L 6379:localhost:6379 \
  -L 6380:localhost:6380 \
  -L 6381:localhost:6381 \
  -L 6382:localhost:6382 \
  -L 6383:localhost:6383 \
  -L 29092:localhost:29092 \
  -L 9200:localhost:9200 \
  "$EC2_HOST" || STATUS=$?

# 130 = Ctrl+C. 그 외 실패는 대부분 로컬 포트를 누가 이미 잡고 있는 경우다.
if [ "$STATUS" -ne 0 ] && [ "$STATUS" -ne 130 ]; then
  echo >&2
  echo "[error] 터널이 종료되었습니다 (exit $STATUS)." >&2
  echo "        위에 'bind ... Permission denied' 가 보이면 그 포트를 이미 누가 잡고 있다는 뜻입니다." >&2
  echo "        - 다른 터널이 떠 있는지 확인하십시오. WSL 과 PowerShell 양쪽 다 봐야 합니다." >&2
  echo "        - WSL 이 networkingMode=mirrored 면 두 환경이 포트를 공유하고," >&2
  echo "          종료된 터널의 예약이 남기도 합니다. 그때는 'wsl --shutdown' 으로 정리됩니다." >&2
  echo "        - MySQL 포트만 겹친다면 LOCAL_MYSQL_PORT 로 옮길 수 있습니다." >&2
fi
exit "$STATUS"
