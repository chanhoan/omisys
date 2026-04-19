#!/bin/bash

COMPOSE_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG=/var/log/omisys-start.log

log()  { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG"; }
warn() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] WARN: $*" | tee -a "$LOG"; }
err()  { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $*" | tee -a "$LOG"; }

cd "$COMPOSE_DIR"

log "=== omisys startup begin ==="

# 0. config 파일 소유권 보정 (filebeat/metricbeat는 root 소유 요구)
chown root:root "$COMPOSE_DIR/filebeat.yml" "$COMPOSE_DIR/metricbeat.yml" 2>/dev/null || true

# 1. 인프라 스택 기동
log "Starting dep stack..."
if ! docker compose -f docker-compose-dep.yml up -d; then
  err "Dep stack failed to start. Continuing anyway..."
fi
log "Dep stack started."

# 2. Elasticsearch 헬스체크 대기 (최대 5분)
log "Waiting for Elasticsearch..."
ELASTIC_PW=$(grep '^ELASTIC_PASSWORD=' "$COMPOSE_DIR/.env" | cut -d= -f2)
ES_READY=false
for i in $(seq 1 60); do
  if docker exec omisys-es01-1 curl -sk https://localhost:9200 \
       -u "elastic:${ELASTIC_PW}" > /dev/null 2>&1; then
    log "Elasticsearch ready (attempt $i)."
    ES_READY=true
    break
  fi
  sleep 5
done

if [ "$ES_READY" = false ]; then
  err "Elasticsearch not ready after 5 minutes. Services needing ES may fail."
fi

# 3. ES 서버 인증서 fingerprint 추출 및 .env 업데이트
log "Extracting ES fingerprint..."
FINGERPRINT=$(docker exec omisys-es01-1 bash -c \
  "openssl x509 -noout -fingerprint -sha256 \
   -in /usr/share/elasticsearch/config/certs/es01/es01.crt \
   | cut -d= -f2" 2>/dev/null)

if [ -z "$FINGERPRINT" ]; then
  warn "Could not extract ES fingerprint. Using existing value in .env."
else
  sed -i "s|ELASTIC_FINGERPRINT=.*|ELASTIC_FINGERPRINT=${FINGERPRINT}|" "$COMPOSE_DIR/.env"
  log "Updated ELASTIC_FINGERPRINT=${FINGERPRINT}"
fi

# 4. Kafka 헬스체크 대기 (최대 3분)
log "Waiting for Kafka..."
KAFKA_READY=false
for i in $(seq 1 36); do
  if docker exec kafka kafka-topics.sh --bootstrap-server kafka:9092 --list > /dev/null 2>&1; then
    log "Kafka ready (attempt $i)."
    KAFKA_READY=true
    break
  fi
  sleep 5
done

if [ "$KAFKA_READY" = false ]; then
  warn "Kafka not ready after 3 minutes. Kafka-dependent services will retry automatically."
fi

# 5. 앱 서비스 스택 기동
log "Starting app stack..."
if ! docker compose up -d; then
  err "App stack failed to start cleanly. Containers with restart:always will self-heal."
fi
log "App stack started."

# 6. Config Server 헬스체크 대기 (최대 3분)
log "Waiting for Config Server..."
CONFIG_READY=false
for i in $(seq 1 36); do
  if docker exec config-server curl -sf http://localhost:8888/actuator/health > /dev/null 2>&1; then
    log "Config Server ready (attempt $i)."
    CONFIG_READY=true
    break
  fi
  sleep 5
done

if [ "$CONFIG_READY" = false ]; then
  warn "Config Server not ready after 3 minutes. Dependent services will retry automatically."
fi

# 7. Eureka Server 헬스체크 대기 (최대 5분)
log "Waiting for Eureka Server..."
EUREKA_READY=false
for i in $(seq 1 60); do
  if docker exec eureka-service curl -sf http://localhost:19090/actuator/health > /dev/null 2>&1; then
    log "Eureka Server ready (attempt $i)."
    EUREKA_READY=true
    break
  fi
  sleep 5
done

if [ "$EUREKA_READY" = false ]; then
  warn "Eureka Server not ready after 5 minutes. App services will retry automatically."
fi

# 8. 모니터링 스택 기동
if [ -f "$COMPOSE_DIR/docker-compose-monitoring.yml" ]; then
  log "Starting monitoring stack..."
  if ! docker compose -f docker-compose-monitoring.yml up -d; then
    warn "Monitoring stack failed to start cleanly."
  else
    log "Monitoring stack started."
  fi
fi

log "=== omisys startup complete ==="
