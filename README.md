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

- Spring Cloud 기반 MSA 아키텍처
- Config Server / Eureka Discovery / API Gateway
- Kafka 기반 비동기 메시징
- Redis, Cassandra, MySQL 혼합 저장소 구조
- Docker + GitHub Actions 기반 CI/CD

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
- 재고 검증을 통과한 요청만 Kafka 이벤트 발행
- Order Service는 이벤트를 소비하여 **비동기 주문 생성**

이를 통해 **주문 서비스의 동시성 부담을 제거**하고
**재고 정합성을 Product Service 경계 내에서 보장**했습니다.

---

### 2️⃣ 주문 · 결제 · Slack 서비스 분리

#### Order → Payment (동기 호출)

![alt text](/docs/ordertopayment.png)

주문 생성 직후 **결제 세션 생성**은  
다음 단계 진행을 위한 필수 선행 조건이기 때문에  
Order Service는 Payment Service를 **동기로 호출**합니다.

#### 결제 URL 전달 책임 분리

OMISYS에는 별도의 프론트엔드/UI 레이어가 없기 때문에  
사용자가 결제 URL로 이동할 수 있는 **전달 채널**이 필요했습니다.

이를 Payment 또는 Order 서비스에 포함시킬 경우  
해당 도메인이 알림/메시징 책임까지 흡수하게 되어  
도메인 경계가 흐려진다고 판단했습니다.

- **Slack Service를 별도로 분리**
- 결제 URL 전달 책임을 전담하도록 설계했습니다.

이로 인해 Order와 Payment 서비스는  
본래의 비즈니스 로직에만 집중할 수 있습니다.

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

- 서비스 실패 시 보상 트랜잭션 이벤트 발행
- 이전 서비스를 호출한 쪽에서 상태 보정
- 데이터의 **최종적 일관성(Eventual Consistency)** 유지

---

## 🛠 Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.4, Spring Cloud
- **Messaging**: Kafka
- **Database**: MySQL, Cassandra, Redis
- **Search**: ElasticSearch
- **Infra**: Docker, GitHub Actions
- **ETC**: Redisson, FeignClient, Slack API, Toss Payments API

---

## 📌 Repository

👉 https://github.com/chanhoan/omisys
