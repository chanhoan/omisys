#!/bin/bash
# 통합 MySQL(omisys-mysql) 최초 기동 시 1회 실행된다.
# docker-compose-dep.yml 에서 /docker-entrypoint-initdb.d/01-init-schemas.sh 로 마운트된다.
#
# 스키마와 계정을 환경변수에서 읽어 생성한다. 비밀번호가 파일에 남지 않으므로
# 저장소에 실제 비밀값을 커밋하지 않는다.
#
# 주의: /docker-entrypoint-initdb.d/ 는 데이터 볼륨이 비어 있을 때만 실행된다.
#       재실행하려면 mysql-data 볼륨을 먼저 삭제해야 한다.
#
# 필요한 환경변수 (서비스마다 3개, docker-compose.yml 의 앱 서비스와 같은 이름):
#   <SVC>_MYSQL_DATABASE  기본값 omisys_<svc>
#   <SVC>_MYSQL_USER      기본값 omisys_<svc>
#   <SVC>_MYSQL_PASSWORD  필수 — 미설정 시 기동을 중단한다
set -euo pipefail

SERVICES="USER PRODUCT ORDER PAYMENT PROMOTION REVIEW NOTIFICATION DELIVERY"

missing=""
for svc in $SERVICES; do
  pw_var="${svc}_MYSQL_PASSWORD"
  if [ -z "${!pw_var:-}" ]; then
    missing="${missing} ${pw_var}"
  fi
done

if [ -n "$missing" ]; then
  echo "[init-schemas] 필수 환경변수가 비어 있습니다:${missing}" >&2
  echo "[init-schemas] .env 를 채운 뒤 mysql-data 볼륨을 삭제하고 다시 기동하십시오." >&2
  exit 1
fi

# 임시 SQL 은 비밀번호를 담으므로 종료 시 반드시 제거한다.
sql_file="$(mktemp)"
trap 'rm -f "$sql_file"' EXIT
chmod 600 "$sql_file"

for svc in $SERVICES; do
  lower="$(echo "$svc" | tr '[:upper:]' '[:lower:]')"

  db_var="${svc}_MYSQL_DATABASE"
  user_var="${svc}_MYSQL_USER"
  pw_var="${svc}_MYSQL_PASSWORD"

  db="${!db_var:-omisys_${lower}}"
  user="${!user_var:-omisys_${lower}}"
  pw="${!pw_var}"

  # 비밀번호 안의 작은따옴표를 이스케이프해 SQL 문법이 깨지지 않게 한다.
  escaped_pw="${pw//\'/\'\'}"

  {
    echo "CREATE DATABASE IF NOT EXISTS \`${db}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    echo "CREATE USER IF NOT EXISTS '${user}'@'%' IDENTIFIED BY '${escaped_pw}';"
    # 각 계정은 자기 스키마에만 권한을 가진다. GRANT 대상을 넓히지 말 것.
    echo "GRANT ALL PRIVILEGES ON \`${db}\`.* TO '${user}'@'%';"
  } >> "$sql_file"
done

echo "FLUSH PRIVILEGES;" >> "$sql_file"

echo "[init-schemas] 스키마/계정 8개를 생성합니다."
mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" < "$sql_file"
echo "[init-schemas] 완료."
