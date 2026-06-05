# Plan: 프론트엔드 작업 전 백엔드 고도화 (omisys MSA)

## Context

omisys ecommerce MSA 백엔드(14개 서비스, Spring Boot 3.3.4 / Java 17 / Spring Cloud 2023.0.3)의
추가 기능 개발을 일단락하고 **프론트엔드 작업으로 넘어가기 전**, 누적된 버그·기술부채·계약 불일치를
정리한다. 목표는 두 가지다.

1. **프론트가 당장 막히는 요소 제거** — CORS 부재, API 문서 부재, 인증/회원가입 경로 모호, 이미지 서빙 부재
2. **프론트가 의존할 백엔드 계약을 안정화** — 데이터 정합성 버그, 에러코드 규약, 페이지네이션 포맷 통일

탐색 결과 아키텍처(헥사고날, Outbox, Redisson 락, Resilience4j, Kafka SAGA)는 견고하나,
**주문→결제 트랜잭션 경계 / 재고 동시성 / 입력 검증 / 에러 계약** 에 프론트 연동 전 손봐야 할 구멍이 있다.

사용자 선택: 우선 영역 = **4개 모두**(프론트 차단요소·데이터 정합성·API 계약·테스트). 계획 깊이 = 우선순위 로드맵 + 상위(P0/P1) 상세.

---

## 우선순위 로드맵 (요약)

| Tier | 범주 | 항목 | 근거 |
|---|---|---|---|
| **P0** | 프론트 차단 | Gateway CORS 설정 | cors 설정 전무 → 브라우저 요청 전면 차단 |
| **P0** | 정합성 | 주문→결제 보상(SAGA) 원자성 강화 | `OrderRollbackService` 보상 비원자적, payment 실패 시 부분 정합성 위험 |
| **P0** | 정합성 | 일반 주문 경로 재고 동시성 락 | `ProductService.reduceStock`에 락 없음 → 오버셀 가능 |
| **P0** | 정합성 | 핵심 요청 DTO `@Valid` + 제약 추가 | `OrderCreateRequest`/`AuthRequest` 등 검증 부재 → null/음수 유입 |
| **P1** | API 계약 | 에러코드·HTTP status 규약 통일 | 서비스별 예외 핸들러가 `statusName`/status 제각각 |
| **P1** | 프론트 차단 | OpenAPI(springdoc) 도입 | 문서 전무 → 프론트가 컨트롤러 역공학 |
| **P1** | 프론트 차단 | 인증 플로우 정리(회원가입 경로·쿠키 JWT 문서) | `/api/auth/sign-up` 부재, signup은 user 서비스 |
| **P1** | API 계약 | 페이지네이션 응답 포맷 통일 | Search는 `List` 반환, 나머지는 `Page` |
| **P1** | 정합성 | `getAllErrors().get(0)` 경계 / 쿠폰 롤백 TODO | IndexOOB 위험 + 취소 시 쿠폰 미환불 |
| **P2** | 프론트 차단 | 상품 이미지 서빙 경로 | S3 URL 노출 전략 부재 |
| **P2** | 테스트 | 컨트롤러 통합테스트 + Search/Delivery 보강 | MockMvc/@SpringBootTest 부재, 일부 서비스 1~2 테스트 |
| **P2** | 정리 | 소소한 결함(typo, broad catch, Optional) | UserTierController `ROLE_NANAGER` 등 |

> **권장 실행 순서**: P0를 한 PR씩 → P1 계약/문서 → P2 테스트·정리. 각 Tier 종료 시 회귀 확인.

---

## Mandatory Reading (구현 전 필독)

| Priority | File | 이유 |
|---|---|---|
| P0 | `service/order/server/.../application/service/OrderCreateService.java` | 주문 생성 트랜잭션·보상 흐름 핵심 |
| P0 | `service/order/server/.../application/service/OrderRollbackService.java` | 보상 트랜잭션 원자성 점검 대상 |
| P0 | `service/product/server/.../application/product/ProductService.java:67` | `reduceStock` 재고 차감 로직 |
| P0 | `service/gateway/server/.../GatewayApplication.java` 및 `infrastructure/filter/*` | Gateway는 **Spring Cloud Gateway(리액티브/WebFlux)** — CORS는 서블릿 방식이 아닌 `CorsWebFilter`/globalcors |
| P1 | `common/domain/.../response/ApiResponse.java` | 공통 응답 봉투(이미 통일됨) — 에러코드 규약만 손봄 |
| P1 | 각 서비스 `exception/*ExceptionHandler.java`, `exception/*ErrorCode.java` | 통일 대상 |
| P1 | `service/order/server/.../exception/BusinessExceptionHandler.java:24`, `service/delivery/.../exception/BusinessExceptionHandler.java:24` | `.get(0)` 경계 결함 |
| P2 | `service/search/server/.../presentation/controller/SearchController.java` | List→Page 반환 통일 |

---

## 확인된 사실 (탐색+직접 검증)

- **CORS**: gateway 전체에 cors/CorsConfiguration 참조 **0건**. globalcors 설정도 없음 → 프론트 연동 시 즉시 차단.
- **OrderCreateService**: 메서드 전체 `@Transactional`. 마지막에 외부 `payment()`(PaymentClient Feign) 호출.
  catch는 `FeignClientException | OrderException | CallNotPermittedException` 만 잡아
  `orderRollbackService.rollbackTransaction(...)`(원격 재고/쿠폰/포인트 보상) 후 재throw로 로컬 DB 롤백.
  → 수동 SAGA 보상은 **있으나**, 보상 자체가 비원자적이고 그 외 예외는 미보상. payment는 마지막 단계라 흐름상 큰 구멍은 아니나 **보상 견고화 필요**.
- **reduceStock(ProductService:67)**: `@Lock`/PESSIMISTIC/synchronized **없음**. (pre-order 경로는 Redisson 락 사용, 일반 주문 경로는 미보호)
- **ApiResponse**: `{statusName, message, data}` 봉투는 **이미 공통**. 불일치는 봉투가 아니라 각 핸들러가 `statusName`에 넣는 값(`HttpStatus.name()` vs 커스텀 코드)과 HTTP status 코드 사용.

---

## P0 상세 — 구현 가능 수준

### Task 1: Gateway CORS 설정
- **ACTION**: Spring Cloud Gateway(WebFlux)에 글로벌 CORS 허용 추가.
- **IMPLEMENT**: `application.yml`의 `spring.cloud.gateway.globalcors.cors-configurations`로
  `allowedOriginPatterns`(프론트 origin), `allowedMethods`, `allowedHeaders`, `allowCredentials: true`(쿠키 JWT이므로 필수) 설정.
  또는 `CorsWebFilter` Bean 등록. **서블릿용 `CorsConfigurationSource` 사용 금지**(gateway는 리액티브).
- **GOTCHA**: 쿠키 기반 인증이라 `allowCredentials=true` + 와일드카드 origin 동시 사용 불가 → `allowedOriginPatterns` 사용. `InternalPathBlockFilter`와 충돌 없는지 확인.
- **VALIDATE**: 프론트 origin에서 preflight(OPTIONS) 200 + `Access-Control-Allow-Credentials: true` 응답 확인.

### Task 2: 주문→결제 보상(SAGA) 원자성 강화
- **ACTION**: `OrderRollbackService.rollbackTransaction`의 보상 단계(재고/쿠폰/포인트 원복)를 부분 실패에 견디도록 보강.
- **IMPLEMENT**: 각 보상 호출을 개별 try/catch로 감싸 한 보상 실패가 나머지를 막지 않게 하고, 실패한 보상은 로깅+(가능 시)Outbox/DLT로 후속 처리. catch 대상 확대 또는 finally 보상 검토.
- **MIRROR**: 기존 Outbox 패턴(`infrastructure/messaging/OutboxEventPoller`)·Resilience4j 사용 방식.
- **GOTCHA**: 보상 자체에 `@Transactional` 부여 시 원격 호출은 롤백 대상이 아님 — DB 보상과 원격 보상을 분리해 다룰 것.
- **VALIDATE**: payment 단계 강제 실패 주입 테스트 → 재고/쿠폰/포인트 원복 + 주문 미커밋 검증.

### Task 3: 일반 주문 경로 재고 동시성 보호
- **ACTION**: `reduceStock` 저장소 확인 후(MySQL인지 Cassandra인지) 적합한 동시성 제어 적용.
- **IMPLEMENT**: MySQL이면 비관적 락(`@Lock(PESSIMISTIC_WRITE)`) 또는 조건부 UPDATE(`set stock=stock-? where stock>=?`)로 음수 방지. Cassandra/Redis면 pre-order와 동일한 Redisson 락 재사용.
- **MIRROR**: pre-order 경로의 Redisson 락 키 패턴(`preorder:lock:{productId}`).
- **GOTCHA**: 락 범위를 좁혀 처리량 저하 최소화. `productClient.updateStock` 호출과 정합 유지.
- **VALIDATE**: 동시 N요청 부하 테스트로 재고 음수 미발생 확인.

### Task 4: 핵심 요청 DTO 검증 보강
- **ACTION**: 컨트롤러 `@Valid` + DTO 제약 추가.
- **IMPLEMENT**: `OrderCreateRequest`/`OrderProductInfo`(`@NotEmpty` 리스트, `@NotNull addressId`, `@Min(1) quantity`, `@PositiveOrZero pointPrice`), `AuthRequest.SignIn`(`@NotBlank`), `PaymentRequest.Create` 등에 Jakarta Validation 적용. 컨트롤러 파라미터에 `@Valid`.
- **MIRROR**: 이미 `@Valid`를 쓰는 다른 서비스 컨트롤러 패턴.
- **GOTCHA**: 검증 실패 응답이 Task 5 에러 규약과 같은 포맷으로 나가도록 `BusinessExceptionHandler`와 연동.
- **VALIDATE**: 잘못된 입력(null/음수/빈 리스트)에 400 + 표준 에러 응답.

---

## P1 상세 — 구현 가능 수준

### Task 5: 에러코드·HTTP status 규약 통일
- **ACTION**: 서비스별 예외 핸들러의 `statusName` 의미와 HTTP status 매핑을 공통 규약으로 정렬.
- **IMPLEMENT**: 공통 에러코드 네이밍 규칙 정의(예: `ORDER_NOT_FOUND`)하고 각 `*ErrorCode`/`*ExceptionHandler`가 동일 형식의 `ApiResponse.error(code, message)` + 적절한 HTTP status를 반환하도록 정리. 필요 시 `common/domain`에 공통 advice/베이스 클래스 추출.
- **GOTCHA**: gateway의 `GatewayExceptionHandler`(리액티브)는 별도 처리 — 동일 응답 스키마만 맞추기.
- **VALIDATE**: 대표 4xx/5xx 케이스가 모든 서비스에서 동일 스키마(코드 문자열+message+status) 반환.

### Task 6: `getAllErrors().get(0)` 경계 + 쿠폰 롤백 TODO
- **ACTION**: `BusinessExceptionHandler:24`(order, delivery) 빈 리스트 가드, `OrderService.java:79`의 `// TODO 쿠폰 사용 롤백` 구현.
- **IMPLEMENT**: `getAllErrors()` 비었을 때 기본 메시지 fallback. 주문 취소(`cancelOrder`) 시 사용 쿠폰 환불을 `promotionClient` 보상 호출로 추가.
- **VALIDATE**: 검증 0건 케이스 무예외, 쿠폰 사용 주문 취소 시 쿠폰 복구 확인.

### Task 7: OpenAPI(springdoc) 도입
- **ACTION**: 외부 노출 서비스에 `springdoc-openapi-starter-webmvc-ui` 추가(gateway는 webflux용), 핵심 엔드포인트 `@Operation`/`@Schema` 주석.
- **GOTCHA**: gateway 뒤에서 각 서비스 `/v3/api-docs` 라우팅/집계 전략 결정. 내부(`/internal/**`) 엔드포인트는 문서 제외.
- **VALIDATE**: `/swagger-ui.html` 노출, 주요 도메인 스키마 표시.

### Task 8: 인증 플로우 정리
- **ACTION**: 회원가입 경로(`/api/users/sign-up`) 와 로그인/리프레시/로그아웃(쿠키 JWT) 흐름을 프론트용으로 명문화. 필요 시 gateway 라우팅으로 `/api/auth/sign-up` 별칭 제공 여부 결정.
- **VALIDATE**: 가입→로그인→보호 API 호출→리프레시→로그아웃 e2e 시나리오 문서/검증.

### Task 9: 페이지네이션 포맷 통일
- **ACTION**: `SearchController`가 `List` 대신 `Page<T>`(또는 공통 페이지 응답 DTO) 반환하도록 정렬.
- **GOTCHA**: ElasticSearch total count 확보 방식 확인.
- **VALIDATE**: 프론트가 모든 목록 API에서 동일한 페이지 메타(totalElements/totalPages) 소비 가능.

---

## P2 — 요약 (상세는 실행 시 확정)

- **이미지 서빙**: `/api/products/{productId}/images` 또는 S3 presigned URL 전략 도입(현재 업로드만 존재).
- **테스트 보강**: 컨트롤러 통합테스트(@SpringBootTest+MockMvc), Search(1)·Delivery(2)·Review(2)·Auth(3) 취약 서비스 우선. 가능 시 Testcontainers.
- **소소한 정리**: `UserTierController` `ROLE_NANAGER` typo, `AuthControllerAdvice`의 `catch(RuntimeException)` 과도 범위, `CouponInternalService` null 비교→Optional, Kafka consumer DLQ/재시도 일관화, payment secret Base64 매호출 인코딩 캐싱.
  - 주의: 사용자 글로벌 규칙상 **내가 만든 변경으로 생긴 orphan만 제거**, 기존 dead code는 보고만.

---

## NOT Building (범위 외)

- 프론트엔드 코드 자체 (이번 작업은 백엔드 선정리)
- 신규 비즈니스 기능/도메인 추가
- 인프라 재설계(서비스 분리/통합, DB 교체)
- 관측성(Prometheus/Zipkin) 확장
- 대규모 아키텍처 리팩토링(헥사고날 구조 변경 등)

---

## Validation (전체 회귀)

```bash
# 영향 서비스 단위 빌드/테스트 (예: order, product, gateway)
./gradlew :service:order:server:test :service:product:server:test :service:gateway:server:test

# 전체 빌드 회귀
./gradlew build
```
- **로컬 스택 기동**: `./start.sh` (docker-compose-dep → docker-compose) 후 gateway(`:19091`) 경유.
- **수동 검증 체크리스트**:
  - [ ] 프론트 origin preflight CORS 통과(credentials 포함)
  - [ ] 잘못된 주문/로그인 입력 → 400 표준 에러 응답
  - [ ] payment 실패 주입 시 재고/쿠폰/포인트 보상 + 주문 미생성
  - [ ] 동시 주문 부하에서 재고 음수 미발생
  - [ ] 주요 목록 API 페이지 메타 일관
  - [ ] `/swagger-ui.html` 노출
  - [ ] 가입→로그인→리프레시→로그아웃 e2e

## Risks

| Risk | 가능성 | 영향 | 완화 |
|---|---|---|---|
| Gateway 리액티브 CORS를 서블릿 방식으로 잘못 적용 | 중 | 높음 | `CorsWebFilter`/globalcors만 사용, 필터 체인 충돌 점검 |
| 재고 락 적용이 처리량 저하 유발 | 중 | 중 | 조건부 UPDATE 우선, 락 범위 최소화, 부하 테스트 |
| 에러 규약 통일이 기존 응답 깨뜨려 회귀 | 중 | 중 | 봉투 유지·코드 문자열만 정렬, 서비스별 점진 적용 |
| SAGA 보상 변경이 새 정합성 버그 유발 | 중 | 높음 | 실패 주입 테스트 선행, Outbox/DLT로 미보상 캡처 |

## Notes
- 각 Task는 작은 PR 단위로 분리 권장(P0 4건 → P1 5건 → P2). 커밋 타입: `fix`(버그), `refactor`(계약 정리), `feat`(OpenAPI/이미지), `test`(보강).
- 다음 단계로 특정 Task를 구현하려면 `/prp-implement` 또는 해당 서비스 리뷰 스킬과 연계.
