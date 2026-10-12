# OMISYS

![alt text](/docs/image.png)

**대규모 트래픽 처리 기반 MSA 온라인 쇼핑몰 백엔드**

OMISYS는 블랙프라이데이, 사전예약 판매와 같이  
짧은 시간에 트래픽이 집중되는 상황에서도  
주문 · 결제 · 재고 · 쿠폰과 같은 핵심 이커머스 도메인이  
안정적으로 동작하도록 설계된 **MSA 기반 쇼핑몰 백엔드 시스템**입니다.

이 프로젝트는  
단순히 MSA 기술을 나열하는 것이 아니라,  
**이커머스라는 도메인이 왜 MSA를 필요로 하는가**를  
구조적으로 이해하고 설계하는 것을 목표로 진행되었습니다.

---

## 🧩 Why MSA?

이커머스 도메인은 기능별로 트래픽 특성과 실패 허용 범위가 크게 다릅니다.

- **상품 조회**
  - 높은 조회 트래픽
  - 읽기 성능과 확장성이 중요
- **주문 / 결제**
  - 실패 허용 범위가 매우 낮음
  - 데이터 정합성이 최우선
- **사전예약 / 쿠폰 발급**
  - 짧은 시간에 폭발적인 트래픽
  - 강한 동시성 제어 필요

이러한 특성을 단일 서비스에서 처리할 경우  
트랜잭션 결합, 장애 전파, 성능 병목이 발생하게 됩니다.

OMISYS는 도메인 책임을 기준으로 서비스를 분리하여  
각 서비스가 **자신의 트래픽 특성과 정합성 요구에 맞게**  
독립적으로 확장·보호될 수 있도록 설계되었습니다.

---

## 🏗 Architecture Overview

![alt text](/docs/architecture.png)

- Spring Cloud 기반 MSA 아키텍처 (Config Server / Eureka Discovery / API Gateway)
- Gateway → Auth(JWT 검증) → 9개 애플리케이션 서비스로 라우팅
- 서비스 간 동기 호출은 OpenFeign + Resilience4j Circuit Breaker(fallback)
- **DB per service**: MySQL 컨테이너 1개 안에 서비스별 DB 분리
- 용도별 Redis 분리: gateway(대기열) · auth · product(사전예약) · cart · coupon
- Kafka 기반 비동기 이벤트 + **Transactional Outbox** 패턴
- Elasticsearch 상품 검색, AWS S3 상품 이미지, Firebase FCM 푸시 알림
- Prometheus · Grafana · Zipkin 모니터링 / 분산 추적
- GitHub Actions → GHCR 이미지 빌드 → EC2 Docker Compose 배포

| 서비스 | 역할 | 저장소 |
|---|---|---|
| Gateway | 라우팅, 대기열(트래픽 제어) | Redis |
| Auth | 인증 · JWT 발급/검증 | Redis |
| User | 회원 · 등급 | MySQL |
| Product | 상품 · 재고 · 사전예약 | MySQL, Redis, Elasticsearch, S3 |
| Order | 주문 · 장바구니 · 보상 트랜잭션 | MySQL, Redis |
| Payment | 결제 세션 · 결제 완료 처리 | MySQL |
| Promotion | 쿠폰 발급 | MySQL, Redis |
| Search | 상품 검색 | Elasticsearch |
| Review | 리뷰 · 평점 | MySQL |
| Delivery | 배송 | MySQL |
| Notification | 주문 상태 알림 (FCM) | MySQL |

#### Kafka 토픽

| Producer | Topic | Consumer |
|---|---|---|
| Product | `process-preorder` | Order |
| Payment | `payment-completed` | Order, Delivery |
| Order, Delivery | `order-status-changed` | Order, Notification |
| Review | `review-rating` | Product |
| Promotion | `provide-event-coupon` | Promotion |

---

## 🔑 Core Domain Design

### 1️⃣ 사전예약 상품 처리 (Pre-order Flow)

사전예약 상품 주문은 일반 주문과 달리  
**“주문 생성”보다 “재고에 진입할 수 있는가”가 핵심 도메인**이라고 판단했습니다.

따라서 OMISYS에서는  
주문서 생성 이전 단계에서 **Product Service가 책임을 소유**합니다.

![alt text](/docs/preorder.png)

#### 설계 포인트

- 관리자가 사전예약 시작 시 Redis에 상품 정보 캐싱
- 고트래픽 상황에서 DB 조회 최소화
- Redisson 분산 락으로 예약 수량 제어
- 예약 수량 초과 요청은 Redis 레벨에서 선제 차단
- 재고 검증을 통과한 요청만 예약 처리와 같은 트랜잭션에서 **Outbox 이벤트로 저장**
- Outbox Poller가 Kafka(`process-preorder`)로 발행해 이벤트 유실 방지
- Order Service는 이벤트를 소비하여 **비동기 주문 생성** (중복 소비에도 멱등 처리)

이를 통해 **주문 서비스의 동시성 부담을 제거**하고
**재고 정합성을 Product Service 경계 내에서 보장**했습니다.

---

### 2️⃣ 주문 · 결제 책임 분리

#### Order → Payment (동기 호출)

![alt text](/docs/ordertopayment.png)

주문 생성 직후 **결제 세션 생성**은  
다음 단계 진행을 위한 필수 선행 조건이기 때문에  
Order Service는 Payment Service를 **동기로 호출**합니다.

#### 결제 URL 응답

Payment Service가 생성한 checkout URL은 결제 도메인의 산출물로 취급합니다.
Order Service는 결제 세션 생성 결과를 받아 주문 ID와 checkout URL을
HTTP 응답으로 반환하고, 프론트엔드는 해당 URL로 사용자를 이동시킵니다.

---

### 3️⃣ Gateway 대기열 (Traffic Control)

트래픽 급증 시 코어 서비스 보호를 위해  
Gateway 단에서 **활성 사용자 수를 선제적으로 제어**합니다.

![alt text](/docs/gatewayqueue.png)

#### 동작 방식

- Redis 기반 `wait / proceed` 큐 구조
- 수용 불가 시: 대기 번호 반환
- 수용 가능 시: 즉시 API 처리
- 메모리 기반 구조로 초당 수만 건 요청 대응

Gateway에서 1차적으로 트래픽을 제어함으로써  
**코어 서비스는 과도한 부하 상황에서도 비즈니스 로직과 데이터 정합성에 집중**할 수 있습니다.

---

### 4️⃣ 쿠폰 발급 처리

쿠폰 발급은 **정합성이 핵심인 도메인**이기 때문에  
다른 서비스에 섞지 않고 **Promotion Service가 단독 책임**을 가집니다.

![alt text](/docs/coupons.png)

#### 설계 포인트

- 쿠폰 발급 요청을 Kafka 메시지로 적재
- Consumer가 순차 처리하여 DB 과부하 방지
- Redis 분산 락으로 중복 발급 방지
- 락 획득 실패 요청은 즉시 실패 처리

이를 통해 순간적인 트래픽 폭주 상황에서도 쿠폰 중복 발급 없이 안정적인 처리가 가능합니다.

---

## 🔄 Distributed Transaction (SAGA)

MSA 환경에서는  
서비스별 DB 트랜잭션이 분리되어 있어  
전체 Rollback이 불가능합니다.

OMISYS는 Kafka 기반 **SAGA 패턴**을 적용하여  
분산 트랜잭션을 Application 레벨에서 관리합니다.

- 상태 변경과 이벤트 저장을 한 트랜잭션으로 묶는 **Transactional Outbox** (Product · Order · Payment · Delivery)
- Outbox Poller가 Kafka로 발행해 DB 커밋과 메시지 발행 간 불일치 방지
- 서비스 실패 시 보상 트랜잭션 수행 (예: Order의 재고 · 쿠폰 롤백)
- 결제 완료 → 주문 상태 변경 → 배송 생성 → 알림 순으로 이벤트 체이닝
- 데이터의 **최종적 일관성(Eventual Consistency)** 유지

---

## 🛠 Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.3.4, Spring Cloud 2023.0.3
- **Messaging**: Kafka 3.8 (Transactional Outbox)
- **Database**: MySQL 8.0, Redis 7.4
- **Search**: Elasticsearch
- **Infra**: AWS EC2, Docker Compose, GitHub Actions, GHCR, AWS S3
- **Monitoring**: Prometheus, Grafana, Zipkin
- **ETC**: Redisson, OpenFeign, Resilience4j, Toss Payments API, Firebase FCM

---

## 📌 Repository

👉 https://github.com/chanhoan/omisys
