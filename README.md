# OMISYS 🏬

이커머스 핵심 도메인을 MSA 기반으로 구현한 백엔드 시스템

## 📌 프로젝트 개요

**OMISYS**는 상품 조회부터 장바구니, 주문, 결제, 재고 관리까지  
이커머스의 전 과정을 **Spring Boot 기반 MSA 아키텍처**로 설계한 백엔드 시스템입니다.

Kafka 기반 비동기 이벤트 처리, 동시성 제어, Redis 캐싱, API Gateway 구성, Docker 환경 등을 포함하여  
**현업 수준의 백엔드 시스템 구조와 기술 스택을 학습하고자 기획된 토이 프로젝트**입니다.

> 백엔드 기술의 모든 길은 결국 이커머스로 통한다.

---

## 🛠️ 기술 스택

| 분류           | 기술                      |
| -------------- | ------------------------- |
| Language       | Java 21                   |
| Framework      | Spring Boot, Spring Cloud |
| API Gateway    | Spring Cloud Gateway      |
| Database       | MySQL, MongoDB, Redis     |
| Message Broker | Kafka                     |
| Auth           | Spring Security, JWT      |
| Infra          | Docker, Docker Compose    |
| CI/CD          | GitHub Actions            |

---

## 🧩 아키텍처 구성

- **상품 서비스 (Product Service)**

  - 상품 등록, 수정, 삭제, 상세 조회

- **장바구니 서비스 (Cart Service)**

  - Redis 기반 캐싱 구조 적용
  - 사용자 장바구니 CRUD 처리

- **주문 서비스 (Order Service)**

  - 주문 생성, 재고 감소 로직
  - Kafka 기반 비동기 주문 흐름 처리

- **결제 서비스 (Payment Service)**

  - 결제 요청 및 상태 변경 처리
  - 실패 및 예외 상황 대응 설계

- **인증/인가 서비스 (Auth Service)**

  - 회원가입, 로그인, JWT 발급 및 검증

- **API Gateway**

  - 인증 포함 전 라우팅 처리
  - 서비스 간 API 호출 분산 처리

- **공통 라이브러리 (Common Library)**
  - DTO, 예외, 유틸, 메시지 포맷 등 공유 모듈

---

## 📁 프로젝트 구조

```

OMISYS
├── gateway-service
├── product-service
├── cart-service
├── order-service
├── payment-service
├── auth-service
└── common-library

```

---

## ⚙️ 실행 방법

```bash
# 모든 서비스 및 인프라를 Docker로 실행
$ docker-compose up --build
```

---

## 🚧 개발 예정 기능

- [ ] Kafka 기반 주문-결제 이벤트 연동
- [ ] SAGA 패턴 적용으로 분산 트랜잭션 보장
- [ ] GitHub Actions 기반 CI/CD 파이프라인 구축
- [ ] 사용자 포인트 및 보상 확장 시스템 설계

---

## 📝 목표

- MSA 구조의 실전 경험 습득
- Kafka, Redis 등 비동기 인프라 학습
- 실무 수준의 서비스 간 연동 및 트랜잭션 흐름 구현
- 기술 블로그를 통해 학습한 내용 지속 기록
  → [Velog TIL 모음](https://velog.io/@chanhoan/posts)

---

## 👤 개발자 정보

|                                                                개발자                                                                 |
| :-----------------------------------------------------------------------------------------------------------------------------------: |
| <img src="https://avatars.githubusercontent.com/chanhoan" width="100"/> <br> **정찬환** <br> [@chanhoan](https://github.com/chanhoan) |

---
