# 로컬 개발 환경 구성 (원격 의존성 + 로컬 애플리케이션)

개발 중에는 **의존성만 원격 EC2에 상주**시키고, **애플리케이션 13개는 로컬 IDE에서 `local` 프로파일로 직접 실행**한다.
로컬에서 Docker를 쓰지 않으므로 Windows 바인드 마운트·I/O 문제가 발생하지 않고, 목데이터를 원격 DB에 한 번만 채우면 여러 PC에서 같은 데이터로 이어서 개발할 수 있다.

```
[로컬 PC]                                  [원격 EC2 t3.large]
IDE(Spring Boot × N, local 프로파일)
   |  localhost:3306 / 6379 / 29092 / 9200
   |
   +---- SSH 터널 (22번 포트만 사용) -----> MySQL / Redis / Kafka / Elasticsearch
```

---

## 0. 사전 준비

| 항목 | 값 |
|---|---|
| SSH 키 | 저장소 루트에 `omisys.pem` 이름으로 배치 |
| `.env.local` 의 `OMISYS_DEP_HOST` | `ubuntu@<Elastic IP>` |
| 로컬 포트 | 6379 / 29092 / 9200 이 비어 있을 것 (MySQL 포트는 LOCAL_MYSQL_PORT 로 우회 가능) |

`.env.local` 에 호스트를 적는다. 이 파일은 어차피 DB 비밀번호 때문에 PC 마다 만들어야 하므로 추가 부담이 아니다.

```bash
OMISYS_DEP_HOST=ubuntu@1.2.3.4
```

키는 저장소 루트에 `omisys.pem` 이름으로 둔다. 경로는 여기로 고정이라 설정할 값이 없다.

```
<repo>/omisys.pem
```

키 권한은 스크립트가 실행할 때 알아서 맞춘다. 직접 `chmod` 할 필요가 없다.

- WSL 에서 Windows 드라이브(`/mnt/c/...`) 위의 키는 항상 `0777` 로 보이고 `chmod` 도 먹지 않는다.
  이때는 `~/.ssh/omisys-tunnel.pem` 으로 600 사본을 만들어 그 사본으로 접속한다.
- PowerShell 에서는 키 파일 ACL 을 소유자 전용으로 정리한 뒤 접속한다.

> `.pem` 과 `.env.local` 은 `.gitignore` 대상이다. 저장소에 커밋하지 않는다.

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
  - t3.large(7.6GiB)에서 실측하면 **1.9GiB / 25%** 로 여유가 있다. ES 컨테이너 하나가 1.45GiB를 쓰므로 `mem_limit`이 힙(1g)의 2.5배인 2560m로 잡혀 있다. **4GB 인스턴스로는 이 프로파일이 뜨지 않는다.**
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
| 3306 | 통합 MySQL (`omisys-mysql`) | user · product · order · payment · promotion · review · notification · delivery |
| 6379 | `cart-cache` | order (장바구니) |
| 6380 | `product-cache` | product (상품 캐시 · 재고 락) |
| 6381 | `coupon-cache` | promotion (쿠폰 발행 락) |
| 6382 | `gateway-cache` | gateway (대기열) |
| 6383 | `auth-cache` | auth (리프레시 토큰) |
| 29092 | Kafka (`OUTSIDE` 리스너) | 이벤트 발행·구독 전 서비스 |
| 9200 | Elasticsearch | product · search (`search` 프로파일 필요) |

### 로컬에 MySQL이 이미 떠 있다면

3306을 다른 프로세스가 잡고 있으면 터널이 `bind: Permission denied`로 **기동 자체에 실패한다.**
로컬 MySQL을 끄는 대신 다른 포트로 포워딩하면 된다.

```bash
LOCAL_MYSQL_PORT=13306 ./scripts/tunnel.sh
```

```powershell
$env:LOCAL_MYSQL_PORT = "13306"; .\scripts\tunnel.ps1
```

애플리케이션 쪽도 **같은 값**을 환경변수로 받아야 한다 — `*-local.yml`이 `localhost:${LOCAL_MYSQL_PORT:3306}`을 쓴다.
기본값이 3306이므로 **충돌이 없는 PC는 아무것도 설정하지 않아도 된다.**

점유 중인 프로세스 확인:

```powershell
Get-NetTCPConnection -LocalPort 3306 -State Listen |
  Select-Object OwningProcess, @{n='Name';e={(Get-Process -Id $_.OwningProcess).ProcessName}}
```

연결 확인:

```bash
mysql -h 127.0.0.1 -P "${LOCAL_MYSQL_PORT:-3306}" -u omisys_user -p -e "SHOW DATABASES;"
redis-cli -h 127.0.0.1 -p 6379 ping
curl -s http://localhost:9200 -k -u elastic:<password>
```

> `omisys_user` 계정은 `omisys_user` 스키마만 보여야 한다. 다른 스키마가 보이면 `docs/migrations/mysql/01-init-schemas.sh`의 `GRANT` 범위를 다시 확인한다.

### `bind: Permission denied` 가 3306 이외의 포트에서 날 때

권한 문제가 아니라 **그 포트를 이미 누가 잡고 있다는 뜻이다.** 원인은 대개 터널이 두 개 떠 있는 것이다.

WSL 을 `networkingMode=mirrored` 로 쓰면(`%USERPROFILE%\.wslconfig`) WSL 과 Windows 가 **포트를 공유한다.**
그래서 WSL 에서 `tunnel.sh` 를 띄운 채 PowerShell 에서 `tunnel.ps1` 을 띄우면 뒤에 뜬 쪽이 포트를 못 잡는다.
종료된 터널의 예약이 잠시 남아 `netstat` 에는 안 보이는데도 bind 가 막히는 경우도 있다.

- **터널은 한 셸에서만 띄운다.** WSL 이면 `tunnel.sh`, PowerShell 이면 `tunnel.ps1` 중 하나로 고정한다.
- 떠 있는 터널 확인: `Get-Process ssh` (Windows), `pgrep -af "ssh -i"` (WSL)
- 예약이 남아 안 풀리면 `wsl --shutdown` 으로 정리한다.

두 스크립트 모두 `ExitOnForwardFailure=yes` 로 뜨기 때문에, 포트를 하나라도 못 잡으면 **절반만 연결된 채 살아남지 않고 즉시 종료된다.**
절반만 포워딩된 터널은 애플리케이션이 엉뚱한 곳에서 죽게 만들기 때문이다.

---

## 3. 애플리케이션 실행 (IDE)

### 환경변수 준비 (1회)

```bash
cp .env.local.example .env.local
```

값을 채운다. 비밀번호는 EC2에서 가져온다.

```bash
ssh -i <key.pem> ubuntu@<host> "cat /opt/omisys/.env"
```

`.env.local` 과 `*.pem` 은 `.gitignore` 대상이다. 커밋하지 않는다.

IntelliJ는 **EnvFile 플러그인**으로 `.env.local` 을 Run Configuration에 걸면 서비스마다 다시 입력할 필요가 없다.
셸에서 `./gradlew bootRun` 으로 띄운다면 이렇게 읽는다.

```bash
set -a; . ./.env.local; set +a
```

### 기동 순서

1. **config-server** — `local` 프로파일. `GIT_NAME` · `GIT_TOKEN` 이 필요하다(설정 저장소가 private).
   `curl http://localhost:8888/user/local` 이 JSON을 돌려주면 정상이다.
2. **eureka-service** — `local` 프로파일. 서비스 간 Feign 호출을 쓰지 않는 작업이라면 건너뛰어도 되지만,
   그 경우 애플리케이션 로그에 Eureka 연결 실패 경고가 반복된다.
3. **작업 대상 서비스** — 필요한 것만 `local` 프로파일로 띄운다. 13개를 전부 올릴 필요는 없다.

IntelliJ 기준 Run Configuration:

- **Active profiles**: `local`
- **Environment variables**: `.env.local` (EnvFile 플러그인)
- **VM options**(로컬 메모리가 부족할 때만): `-Xmx256m -XX:MaxMetaspaceSize=128m`

### 왜 CONFIG_SERVER_URI 와 EUREKA_URI 가 필요한가

각 서비스의 `application.yml` 은 기본값으로 **Docker 호스트명**(`config-server:8888`, `eureka-service:19090`)을 쓴다.
컨테이너 안에서는 맞지만 로컬 IDE에서는 해석되지 않아, 터널까지 가지도 못하고 **부트스트랩 단계에서 기동이 실패한다.**

```yaml
uri: ${CONFIG_SERVER_URI:http://config-server:8888}
defaultZone: ${EUREKA_URI:http://eureka-service:19090/eureka/}
```

기본값이 기존 값이라 **컨테이너 배포는 아무것도 바뀌지 않는다.** 로컬에서만 두 변수를 `localhost` 로 덮어쓴다.

Config Server 주소만은 환경변수여야 한다 — 설정을 받아오기 *전에* 필요한 값이라 설정 저장소에 둘 수 없다.

> Config 저장소(`chanhoan/omisys_config`)를 수정했다면 Config Server가 `clone-on-start: true` 라 **재시작해야 반영된다.**

### 연결 확인

애플리케이션 로그에서 HikariPool이 커넥션을 잡았는지 본다.

```
HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@...
Started UserApplication in 12.51 seconds
```

원격 DB에 테이블이 실제로 생겼는지도 확인할 수 있다(`ddl-auto: update`).

```bash
ssh -i <key.pem> ubuntu@<host> \
  'cd /opt/omisys; set -a; . ./.env; set +a; \
   docker exec -e MYSQL_PWD="$USER_MYSQL_PASSWORD" omisys-mysql \
     mysql -uomisys_user -N -e "USE omisys_user; SHOW TABLES;"'
```

### 필수 환경변수

`local` 프로파일은 터널 너머의 **공유 MySQL**에 붙는다. 그 인스턴스에는 스키마별 계정만 있고 예전의 개인 계정은 없으므로,
실행하려는 서비스의 자격증명을 환경변수로 넘겨야 한다. 이름은 `prod`·`docker-compose.yml`·`01-init-schemas.sh`와 같아서 **`.env` 한 벌로 전부 맞춰진다.**

| 변수 | 기본값 | 비고 |
|---|---|---|
| `<SVC>_MYSQL_DATABASE` | `omisys_<svc>` | 스키마명을 바꾸지 않았다면 생략 가능 |
| `<SVC>_MYSQL_USER` | `omisys_<svc>` | 위와 동일 |
| `<SVC>_MYSQL_PASSWORD` | **없음** | 미설정 시 기동이 실패한다 (잘못된 계정으로 붙는 것보다 낫다) |

`<SVC>`는 `USER` · `PRODUCT` · `ORDER` · `PAYMENT` · `PROMOTION` · `REVIEW` · `NOTIFICATION` · `DELIVERY` 중 하나다.
**`search-service`는 product 스키마를 공유하므로 `PRODUCT_*` 를 쓴다.**

Elasticsearch를 쓰는 서비스(`product` · `search`)는 두 개가 더 필요하다.

| 변수 | 기본값 | 비고 |
|---|---|---|
| `ELASTIC_PASSWORD` | **없음** | `search` 프로파일로 ES를 띄운 경우 필수 |
| `ELASTIC_FINGERPRINT` | 빈 값 | HTTPS 인증서 지문. 평문 HTTP면 비워 둔다 |

예시 — order-service 하나만 띄울 때:

```bash
ORDER_MYSQL_PASSWORD=<원격 omisys_order 계정 비밀번호>
```

여러 서비스를 자주 띄운다면 IDE의 EnvFile 플러그인으로 `.env` 하나를 공유하는 편이 낫다.
`.env`와 `.pem`은 커밋하지 않는다.

---

## 4. 보안 주의

> **보안그룹에는 SSH 22번만 열어 둔다.** 본인 IP(`/32`)로 제한하고, **MySQL 3306 · Redis 6379 · Kafka 29092 · Elasticsearch 9200 은 절대 인바운드로 개방하지 않는다.**

모든 트래픽은 SSH 위로 흐르므로 추가 개방이 필요 없고, 이것이 가장 안전한 구성이다. DB 포트를 공개로 열면 인증이 약한 계정 하나로 전체 데이터가 노출된다.

그 밖에:

- `.pem` 키 파일과 `.env`는 저장소에 커밋하지 않는다. 커밋 전 `.gitignore` 확인.
- 스키마 계정 비밀번호는 `01-init-schemas.sh`가 환경변수에서 읽는다. 파일에 적어 두지 않는다.
- `*-local.yml`도 자격증명을 환경변수로 받는다. 평문 계정을 되돌리지 않는다 — 그 계정은 공유 인스턴스에 존재하지 않는다.

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
