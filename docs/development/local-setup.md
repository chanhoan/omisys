# 로컬 개발 환경 구성 (원격 의존성 + 로컬 애플리케이션)

개발 중에는 **의존성만 원격 EC2에 상주**시키고, **애플리케이션 13개는 로컬 IDE에서 `local` 프로파일로 직접 실행**한다.
로컬에서 Docker를 쓰지 않으므로 Windows 바인드 마운트·I/O 문제가 발생하지 않고, 목데이터를 원격 DB에 한 번만 채우면 여러 PC에서 같은 데이터로 이어서 개발할 수 있다.

```
[로컬 PC]                                  [원격 EC2 t3.medium]
IDE(Spring Boot × N, local 프로파일)
   |  localhost:3306 / 6379 / 29092 / 9200
   |
   +---- SSH 터널 (22번 포트만 사용) -----> MySQL / Redis / Kafka / Elasticsearch
```

---

## 0. 사전 준비

| 항목 | 값 |
|---|---|
| SSH 키 | `.pem` 파일을 로컬에 배치 (예: `C:\keys\omisys.pem`) |
| 환경변수 `OMISYS_DEP_HOST` | `ec2-user@<원격 IP>` |
| 환경변수 `OMISYS_DEP_KEY` | `.pem` 파일 절대경로 |
| 로컬 포트 | 3306 / 6379 / 29092 / 9200 을 아무것도 점유하고 있지 않을 것 |

PowerShell:

```powershell
$env:OMISYS_DEP_HOST = "ec2-user@1.2.3.4"
$env:OMISYS_DEP_KEY  = "C:\keys\omisys.pem"
```

Git Bash / 리눅스:

```bash
export OMISYS_DEP_HOST="ec2-user@1.2.3.4"
export OMISYS_DEP_KEY="$HOME/keys/omisys.pem"
chmod 600 "$OMISYS_DEP_KEY"
```

> `.pem` 파일과 호스트 주소는 **스크립트에 하드코딩하지 않는다.** 키 파일은 저장소 밖에 두고 커밋하지 않는다.

---

## 1. 의존성 서버 기동 (원격 EC2에서 1회)

용도에 따라 프로파일을 골라 기동한다. `profiles`가 붙은 서비스는 **`--profile` 없이는 뜨지 않으며, 다른 서비스의 `depends_on` 대상이어도 자동 기동되지 않는다.**

| 목적 | 명령 | 메모리 |
|---|---|---|
| 개발 (기본) | `docker compose -f docker-compose-dep.yml up -d` | 1.95GB |
| 개발 + 검색 | `docker compose -f docker-compose-dep.yml --profile search up -d` | 3.55GB |
| 부하테스트 | `... --profile search --profile loadtest up -d` + 앱 compose | 8.75GB |

- **기본(1.95GB)**: 통합 MySQL 0.9 + Redis 0.25 + Kafka 0.8. 대부분의 서비스 개발은 여기서 끝난다.
- **`search`(3.55GB)**: Elasticsearch(`setup` + `es01`)가 추가된다. **product · search 서비스를 로컬에서 띄울 때는 이 프로파일이 필요하다.** 그 외 서비스 작업에는 불필요하다.
- **`loadtest`(8.75GB)**: Prometheus · Grafana · Zipkin과 애플리케이션 컨테이너까지 전부 올린 데모/부하테스트 구성. t3 계열은 CPU 크레딧이 소진되면 스로틀되므로 **부하테스트 구간에는 c5/m5 등 고정 성능 인스턴스를 쓴다.**

기동 확인:

```bash
docker compose -f docker-compose-dep.yml ps
docker compose -f docker-compose-dep.yml config --services              # es01 · setup 미포함
docker compose -f docker-compose-dep.yml --profile search config --services  # es01 · setup 포함
```

---

## 2. SSH 터널 기동 (로컬)

터널을 열어 두면 애플리케이션은 의존성이 로컬에 있다고 믿고 동작한다. **터널은 애플리케이션보다 먼저 띄운다.**

PowerShell:

```powershell
.\scripts\tunnel.ps1
```

Git Bash / 리눅스:

```bash
./scripts/tunnel.sh
```

포워딩되는 포트는 다음과 같다. 터널 창은 열어 둔 채로 두고, 종료는 `Ctrl+C`다.

| 로컬 포트 | 원격 대상 | 사용 서비스 |
|---|---|---|
| 3306 | 통합 MySQL (`omisys-mysql`) | user · product · order · payment · promotion · review · notification |
| 6379 | Redis | gateway · product · promotion (Redisson 락 · 캐시) |
| 29092 | Kafka (`OUTSIDE` 리스너) | 이벤트 발행·구독 전 서비스 |
| 9200 | Elasticsearch | product · search (`search` 프로파일 필요) |

연결 확인:

```bash
mysql -h 127.0.0.1 -P 3306 -u omisys_user -p -e "SHOW DATABASES;"
redis-cli -h 127.0.0.1 -p 6379 ping
curl -s http://localhost:9200 -k -u elastic:<password>
```

> `omisys_user` 계정은 `omisys_user` 스키마만 보여야 한다. 다른 스키마가 보이면 `docs/migrations/mysql/init-schemas.sql`의 `GRANT` 범위를 다시 확인한다.

---

## 3. 애플리케이션 실행 (IDE)

기동 순서를 지킨다.

1. **config-server** — `local` 프로파일. `http://localhost:8888/user/local` 이 200을 반환하는지 확인한다.
2. **eureka-service** — `local` 프로파일.
3. **작업 대상 서비스** — 필요한 것만 `local` 프로파일로 실행한다. 13개를 전부 띄울 필요는 없다.

IntelliJ 기준 Run Configuration:

- **Active profiles**: `local`
- **VM options**(로컬 메모리가 부족할 때만): `-Xmx256m -XX:MaxMetaspaceSize=128m`

`*-local.yml`은 이미 전부 `localhost` 기반이므로 터널만 열려 있으면 별도 설정 변경이 필요 없다.
Config 저장소(`chanhoan/omisys_config`)를 수정한 경우 Config Server가 `clone-on-start: true`라 **재시작해야 반영된다.**

---

## 4. 보안 주의

> **보안그룹에는 SSH 22번만 열어 둔다.** 본인 IP(`/32`)로 제한하고, **MySQL 3306 · Redis 6379 · Kafka 29092 · Elasticsearch 9200 은 절대 인바운드로 개방하지 않는다.**

모든 트래픽은 SSH 위로 흐르므로 추가 개방이 필요 없고, 이것이 가장 안전한 구성이다. DB 포트를 공개로 열면 인증이 약한 계정 하나로 전체 데이터가 노출된다.

그 밖에:

- `.pem` 키 파일은 저장소에 커밋하지 않는다. 커밋 전 `.gitignore` 확인.
- `init-schemas.sql`의 `:XXX_PW` 플레이스홀더는 배포 시점에 치환한다. **실제 비밀번호를 커밋하지 않는다.**
- `user-local.yml`의 평문 계정(`chanhoan/chanhoan`)은 원격 DB 계정과 분리한다. 원격용 비밀번호는 환경변수로 뺀다.

---

## 5. 트러블슈팅

| 증상 | 원인 | 조치 |
|---|---|---|
| `bind: Address already in use` | 로컬에 MySQL/Redis가 이미 떠 있음 | 로컬 서비스를 끄거나, 터널 로컬 포트를 `-L 13306:localhost:3306`처럼 바꾸고 `*-local.yml` 포트를 맞춘다 |
| Kafka 컨슈머가 붙지 않음 | `KAFKA_ADVERTISED_LISTENERS`의 `OUTSIDE://localhost:29092`가 변경됨 | 광고 주소를 원복한다. 이 값과 29092 포워딩은 정확히 짝을 이뤄야 한다 |
| 유휴 상태에서 터널이 끊김 | 중간 장비의 유휴 타임아웃 | 스크립트에 `-o ServerAliveInterval=30`이 들어 있다. 그래도 끊기면 터널을 재기동한다 (HikariCP가 재연결하지만 그 순간 요청은 실패한다) |
| product/search 기동 실패 | Elasticsearch 미기동 | 원격에서 `--profile search`로 다시 올린다 |
| `Communications link failure` | 터널이 안 떠 있음 | 터널 창이 살아 있는지 확인 후 3306 접속을 재검증한다 |
| 로컬 PC 메모리 부족 | 서비스를 너무 많이 띄움 | 작업 대상 서비스만 실행하고, VM options에 `-Xmx256m`을 개별 지정한다 |
