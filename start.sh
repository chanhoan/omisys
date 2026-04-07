#!/bin/bash
set -e

COMPOSE_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG=/var/log/omisys-start.log

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG"; }

cd "$COMPOSE_DIR"

log "=== omisys startup begin ==="

# 1. 인프라 스택 기동
log "Starting dep stack..."
docker compose -f docker-compose-dep.yml up -d
log "Dep stack started."

# 2. Elasticsearch 헬스체크 대기 (최대 5분)
log "Waiting for Elasticsearch..."
ELASTIC_PW=$(grep '^ELASTIC_PASSWORD=' "$COMPOSE_DIR/.env" | cut -d= -f2)
for i in $(seq 1 60); do
  if docker exec omisys-es01-1 curl -sk https://localhost:9200 \
       -u "elastic:${ELASTIC_PW}" > /dev/null 2>&1; then
    log "Elasticsearch ready (attempt $i)."
    break
  fi
  if [ "$i" -eq 60 ]; then
    log "ERROR: Elasticsearch not ready after 5 minutes. Aborting."
    exit 1
  fi
  sleep 5
done

# 3. ES 서버 인증서 fingerprint 추출 및 .env 업데이트
log "Extracting ES fingerprint..."
FINGERPRINT=$(docker exec omisys-es01-1 bash -c \
  "openssl x509 -noout -fingerprint -sha256 \
   -in /usr/share/elasticsearch/config/certs/es01/es01.crt \
   | cut -d= -f2")

if [ -z "$FINGERPRINT" ]; then
  log "ERROR: Could not extract fingerprint."
  exit 1
fi

sed -i "s|ELASTIC_FINGERPRINT=.*|ELASTIC_FINGERPRINT=${FINGERPRINT}|" "$COMPOSE_DIR/.env"
log "Updated ELASTIC_FINGERPRINT=${FINGERPRINT}"

# 4. 앱 서비스 스택 기동
log "Starting app stack..."
docker compose up -d
log "App stack started."

# 5. Config Server 헬스체크 대기 (최대 3분)
log "Waiting for Config Server..."
for i in $(seq 1 36); do
  if docker exec config-server curl -sf http://localhost:8888/actuator/health > /dev/null 2>&1; then
    log "Config Server ready (attempt $i)."
    break
  fi
  if [ "$i" -eq 36 ]; then
    log "ERROR: Config Server not ready after 3 minutes. Aborting."
    exit 1
  fi
  sleep 5
done

# 6. Eureka Server 헬스체크 대기 (최대 3분)
log "Waiting for Eureka Server..."
for i in $(seq 1 36); do
  if docker exec eureka-service curl -sf http://localhost:19090/actuator/health > /dev/null 2>&1; then
    log "Eureka Server ready (attempt $i)."
    break
  fi
  if [ "$i" -eq 36 ]; then
    log "ERROR: Eureka Server not ready after 3 minutes. Aborting."
    exit 1
  fi
  sleep 5
done

# 7. 모니터링 스택 기동
if [ -f "$COMPOSE_DIR/docker-compose-monitoring.yml" ]; then
  log "Starting monitoring stack..."
  docker compose -f docker-compose-monitoring.yml up -d
  log "Monitoring stack started."
fi

log "=== omisys startup complete ==="
