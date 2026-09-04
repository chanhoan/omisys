# Plan: 주소 검색 연동 — Backend (user 서비스 Address 구조화 + 검증)

## Summary
프론트는 Daum(Kakao) 우편번호 위젯(키·쿼터 없는 클라이언트 스크립트)으로 주소를 고른다.
백엔드는 외부 API 를 호출하지 않는다. 대신 `user` 서비스의 `Address` 를 단일 `address`
문자열(현재 프론트가 두 칸 공백 `"  "` 로 base/detail 을 이어 붙이는 취약한 방식)에서
**구조화 필드**(`roadAddress` / `jibunAddress` / `detailAddress` / `sido` / `sigungu`)로
확장하고, 우편번호·필수값에 **Bean Validation** 을 건다. 기존 `address` 컬럼은
파생·하위호환용으로 유지한다.

## User Story
As a 백엔드 개발자,
I want 배송지 API 가 위젯이 주는 구조화 주소 컴포넌트를 그대로 받아 저장·검증하고,
So that 주소 형식이 보장되고, 지역(시/도·시/군/구) 기반 배송비·통계가 가능해지며,
   프론트의 `"  "` 구분자 해킹을 제거할 수 있다.

## Problem → Solution
현재:
- `Address.address` 단일 `String` 컬럼. base/detail 구분은 프론트가 `"  "` 로 이어붙임 → 주소에 두 칸 공백이 들어가면 파싱 붕괴.
- `AddressRequest.Create/Update` 는 `@NotNull` 만. 우편번호 형식·빈 문자열 무검증.
- 시/도·시/군/구 등 지역 정보 없음.

목표:
- `Address` + 요청/응답 DTO 에 구조화 필드 추가(전부 nullable, 하위호환).
- `zipcode` `@Pattern("\\d{5}")`, 문자열 `@NotBlank`, `detailAddress` `@Size`.
- `address` 는 요청에 없으면 `roadAddress + " " + detailAddress` 로 서버가 합성.
- OpenAPI(`docs/api/user.json`) 재생성 → 프론트 계약 대조 통과.

## Metadata
- **Complexity**: Medium (DB 컬럼 추가 + 계약 변경, 수동 운영 마이그레이션)
- **Source PRD**: N/A (대화 기반)
- **PRD Phase**: N/A
- **Estimated Files**: 6 (수정 5, 테스트 1) + OpenAPI 산출물 1 + 마이그레이션 SQL 1
- **Repo**: `E:\refactoring\omisys` · module `service/user`
- **관련 계획서**: `E:\refactoring\omisys_frontend\docs\plans\address-search-frontend.plan.md`

---

## UX Design
Internal change — 사용자 대면 UX 변화 없음. 프론트 위젯 UX 는 프론트 계획서 참조.

### Interaction Changes (API 계약)
| Touchpoint | Before | After | Notes |
|---|---|---|---|
| `POST /api/address` body | `alias,recipient,phoneNumber,zipcode,address,isDefault` (전부 `@NotNull`) | + `roadAddress?,jibunAddress?,detailAddress?,sido?,sigungu?`; `address` 는 optional(없으면 서버 합성); `zipcode` 는 `\d{5}` | 신규 필드 미전송해도 동작(하위호환) |
| `PATCH /api/address/{id}` body | 동일 | 동일 확장 | `AddressCard.patchDefault()` 처럼 `address` 만 재전송해도 통과해야 함 |
| `GET /api/address/me` 응답 item | `id,userId,alias,recipient,phoneNumber,zipcode,address,isDefault` | + 구조화 필드(값 있으면) | 프론트 `addressSchema` 확장과 짝 |
| DB `p_address` | `address VARCHAR NOT NULL` | + `road_address,jibun_address,detail_address,sido,sigungu` (전부 NULL 허용) | 기존 행 그대로, `address` 유지 |

---

## Mandatory Reading

| Priority | File | Lines | Why |
|---|---|---|---|
| P0 | `service/user/server/src/main/java/com/omisys/user/domain/model/Address.java` | 1-67 | 엔티티 + `create`/`update` 팩토리. 컬럼·빌더 패턴 |
| P0 | `service/user/server/src/main/java/com/omisys/user/presentation/request/AddressRequest.java` | 1-52 | `Create`/`Update` 정적 클래스. `@NotNull` 만 사용 중 |
| P0 | `service/user/server/src/main/java/com/omisys/user/application/dto/AddressResponse.java` | 1-45 | `Get.of(Address)` 매핑. 응답 필드 |
| P0 | `service/user/server/src/main/java/com/omisys/user/application/service/AddressService.java` | all | `createAddress`/`updateAddress`/검증 흐름, 예외 방식(`UserException`+`UserErrorCode`) |
| P1 | `service/user/server/src/test/java/com/omisys/user/application/service/AddressServiceTest.java` | 1-90 | 테스트 패턴: `@ExtendWith(MockitoExtension)`, `@Mock`/`@InjectMocks`, `@DisplayName`, AssertJ |
| P1 | `service/user/server/src/main/java/com/omisys/user/presentation/controller/AddressController.java` | 1-67 | `@Valid` 위치, `@AuthenticationPrincipal JwtClaim`, `ApiResponse` |
| P1 | `service/user/user_dto/src/main/java/com/omisys/user_dto/infrastructure/AddressDto.java` | 1-20 | 서비스 간 공유 DTO — 다른 서비스가 주소를 읽는 경로. 필드 추가 시 동기화 |
| P1 | `service/user/server/build.gradle` | 26-50 | `spring-boot-starter-validation` 이미 포함 확인 |
| P2 | `service/user/server/src/main/resources/application.yml` | all | 설정이 **config server** 에서 옴 → `ddl-auto`/datasource 는 이 repo 에 없음 |
| P2 | `docs/api/user.json` | address 경로 | OpenAPI 스펙 — 재생성 대상 |
| P2 | `scripts/api/dump_openapi.py` | all | `python3 scripts/api/dump_openapi.py user` 로 스펙 재추출 |
| P1 | `docs/migrations/README.md` | all | 수동 마이그레이션 적용 순서·규칙. 신규 SQL 을 여기 등재 |
| P2 | `docs/migrations/user/20260610-user-devices.sql` | all | 파일명·DDL 형식 참고 예시 |

## External Documentation
No external research needed — feature uses established internal patterns (JPA entity, Bean Validation, Mockito service test). juso.go.kr / Kakao API 는 사용하지 않음(위젯이 클라이언트에서 해결).

```
KEY_INSIGHT: Flyway/Liquibase 자동 실행은 없지만, docs/migrations/<service>/YYYYMMDD-<name>.sql
             수동 운영 마이그레이션 규칙이 있다 (docs/migrations/README.md 가 적용 순서를 관리).
             예: docs/migrations/user/20260610-user-devices.sql
APPLIES_TO: Task 5 — 신규 nullable 컬럼용 ALTER 스크립트를 이 규칙대로 추가하고 README 에 등재.
           dev 는 ddl-auto=update 면 자동 반영되나, 스크립트는 prod/재현용으로 커밋한다.
           NOT NULL 승격은 이번 스코프 아님
GOTCHA: 기존 행에는 road/jibun/detail 이 NULL. 응답 매핑에서 NPE 안 나게 방어
```

---

## Patterns to Mirror

### ENTITY_COLUMN_AND_BUILDER
```java
// SOURCE: service/user/server/src/main/java/com/omisys/user/domain/model/Address.java:27-46
@Column(nullable = false)
private String zipcode;

@Column(nullable = false)
private String address;

public static Address create(User user, AddressRequest.Create request) {
    return Address.builder()
            .user(user)
            .alias(request.getAlias())
            .recipient(request.getRecipient())
            .zipcode(request.getZipcode())
            .address(request.getAddress())
            .isDefault(request.getIsDefault())
            .build();
}
```

### ENTITY_UPDATE
```java
// SOURCE: service/user/server/src/main/java/com/omisys/user/domain/model/Address.java:48-56
public void update(AddressRequest.Update request) {
    this.alias = request.getAlias();
    this.zipcode = request.getZipcode();
    this.address = request.getAddress();
    this.isDefault = request.getIsDefault();
}
```

### REQUEST_VALIDATION
```java
// SOURCE: service/user/server/src/main/java/com/omisys/user/presentation/request/AddressRequest.java:14-27
@Getter
@NoArgsConstructor
@AllArgsConstructor
public static class Create {
    @NotNull
    private String recipient;
    @NotNull
    private String zipcode;
    @NotNull
    private String address;
    @NotNull
    private Boolean isDefault;
}
```

### RESPONSE_MAPPING
```java
// SOURCE: service/user/server/src/main/java/com/omisys/user/application/dto/AddressResponse.java:24-36
public static AddressResponse.Get of(Address address) {
    return Get.builder()
            .id(address.getId())
            .userId(address.getUser().getId())
            .zipcode(address.getZipcode())
            .address(address.getAddress())
            .isDefault(address.getIsDefault())
            .build();
}
```

### CONTROLLER_VALID
```java
// SOURCE: service/user/server/src/main/java/com/omisys/user/presentation/controller/AddressController.java:23-30, 46-55
@PostMapping
public ApiResponse<Void> createAddress(
        @RequestBody AddressRequest.Create request,           // ← @Valid 없음. 추가 필요
        @AuthenticationPrincipal JwtClaim claim) { ... }

@PatchMapping("/{addressId}")
public ApiResponse<Void> updateAddress(
        @PathVariable Long addressId,
        @RequestBody @Valid AddressRequest.Update request,     // ← Update 는 이미 @Valid
        @AuthenticationPrincipal JwtClaim claim) { ... }
```

### SERVICE_TEST
```java
// SOURCE: service/user/server/src/test/java/com/omisys/user/application/service/AddressServiceTest.java:21-45
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @InjectMocks private AddressService addressService;

    @Test
    @DisplayName("createAddress 성공: user 존재 → addressRepository.save 호출")
    void createAddress_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));
        addressService.createAddress(userId, request);
        verify(addressRepository).save(any(Address.class));
    }
}
```

---

## Files to Change

| File | Action | Justification |
|---|---|---|
| `domain/model/Address.java` | UPDATE | nullable 컬럼 `roadAddress,jibunAddress,detailAddress,sido,sigungu` 추가 + `create`/`update` 에 세팅 + `address` 미제공 시 합성 헬퍼 |
| `presentation/request/AddressRequest.java` | UPDATE | `Create`/`Update` 에 신규 optional 필드 + `zipcode` `@Pattern("\\d{5}")` + 문자열 `@NotBlank` + `detailAddress` `@Size(max=100)`; `address` 를 `@NotNull` → optional |
| `presentation/controller/AddressController.java` | UPDATE | `createAddress` 파라미터에 `@Valid` 추가(현재 누락) |
| `application/dto/AddressResponse.java` | UPDATE | `Get` 에 신규 필드 + `of()` 매핑 |
| `user_dto/.../AddressDto.java` | UPDATE | 신규 필드 추가(서비스 간 공유 — order/delivery 가 읽으면 동기화). **소비처 grep 후 판단** |
| `test/.../AddressServiceTest.java` | UPDATE | 구조화 필드 저장/합성/검증 케이스 추가 |
| `docs/migrations/user/20260903-address-structured-columns.sql` | CREATE | `p_address` 에 nullable 컬럼 5개 추가 ALTER (수동 운영 마이그레이션 규칙) |
| `docs/migrations/README.md` | UPDATE | 신규 SQL 을 적용 순서 목록에 등재 |
| `docs/api/user.json` | REGEN | `python3 scripts/api/dump_openapi.py user` 산출물 |

> `AddressDto` 변경 전 `grep -rn "AddressDto\|getAddress\b" service --include=*.java` 로 소비처 확인.
> 소비처가 없거나 user 서비스 내부뿐이면 이번에 같이, 다수면 별도 티켓으로 분리.

## NOT Building
- juso.go.kr / Kakao Local REST 프록시 엔드포인트 (`/api/address/search`) — 위젯이 클라이언트에서 해결
- 외부 API 대조·주소 정규화 서비스
- Flyway/Liquibase 도입 — 스키마는 기존대로 ddl-auto + `docs/migrations/` 수동 스크립트
- 신규 컬럼 `NOT NULL` 승격 / 기존 행 백필 마이그레이션
- `alias` 필수화 (엔티티는 `nullable=false` 인데 프론트가 안 보냄 — 기존 드리프트, 별도)
- 시/도 기준 배송비 로직 (필드만 저장, 활용은 후속)
- 게이트웨이 `PublicPathPolicy` / user `SecurityConfig` 변경 — `/api/address/**` 는 이미 인증 경로이고 그대로 둠

---

## Step-by-Step Tasks

### Task 1: `Address` 엔티티 구조화 필드
- **ACTION**: `domain/model/Address.java` 수정
- **IMPLEMENT**:
  - 필드 추가 (모두 `@Column` 기본 = nullable):
    `private String roadAddress;` `private String jibunAddress;` `private String detailAddress;` `private String sido;` `private String sigungu;`
  - `create(User, AddressRequest.Create)` 빌더에 `.roadAddress(request.getRoadAddress())` … 5개 추가
  - `.address(resolveAddress(request.getAddress(), request.getRoadAddress(), request.getDetailAddress()))` 로 교체
  - `update(AddressRequest.Update)` 에도 동일 5개 대입 + `this.address = resolveAddress(...)`
  - `private static String resolveAddress(String explicit, String road, String detail)`:
    `if (explicit != null && !explicit.isBlank()) return explicit;`
    `return (road == null ? "" : road) + (detail == null || detail.isBlank() ? "" : " " + detail);`
- **MIRROR**: `ENTITY_COLUMN_AND_BUILDER`, `ENTITY_UPDATE`
- **IMPORTS**: 없음(기존 `jakarta.persistence.*`)
- **GOTCHA**: `@Builder(access = PRIVATE)` 라 외부에서 못 만듦 — `create` 팩토리만 사용. `address` 컬럼 `nullable=false` 유지되므로 `resolveAddress` 가 절대 null 반환하면 안 됨(빈 문자열까진 허용되나 요청 검증에서 걸러짐)
- **VALIDATE**: 컴파일. `./gradlew :service:user:server:compileJava`

### Task 2: `AddressRequest` 필드 + 검증
- **ACTION**: `presentation/request/AddressRequest.java` 수정 (`Create`, `Update` 둘 다)
- **IMPLEMENT**:
  - 신규 필드(검증 없음, optional): `private String roadAddress;` `private String jibunAddress;` `private String detailAddress;` `private String sido;` `private String sigungu;`
  - `zipcode`: `@NotNull` → `@NotBlank @Pattern(regexp = "\\d{5}", message = "우편번호는 5자리 숫자입니다.")`
  - `recipient`, `phoneNumber`, `alias`: `@NotNull` → `@NotBlank`
  - `phoneNumber`: `@Pattern(regexp = "^01[016-9]-?\\d{3,4}-?\\d{4}$", message = "연락처 형식이 올바르지 않습니다.")` (기존 데이터 형식 확인 후 완화 가능)
  - `address`: `@NotNull` 제거 → optional (서버 합성). 주석으로 "roadAddress 미제공 시 필수" 명시
  - `detailAddress`: `@Size(max = 100)`
  - `isDefault`: `@NotNull` 유지
- **MIRROR**: `REQUEST_VALIDATION`
- **IMPORTS**: `import jakarta.validation.constraints.NotBlank;` `import jakarta.validation.constraints.Pattern;` `import jakarta.validation.constraints.Size;`
- **GOTCHA**: `@NotBlank` 는 `String` 전용 — `Boolean isDefault` 엔 못 씀. Lombok `@AllArgsConstructor` 필드 순서 바뀌면 기존 테스트의 위치기반 생성자 호출 깨짐 → 신규 필드는 **뒤에** 추가
- **VALIDATE**: `./gradlew :service:user:server:compileJava`

### Task 3: 컨트롤러 `@Valid`
- **ACTION**: `presentation/controller/AddressController.java` — `createAddress` 의 `@RequestBody AddressRequest.Create request` 를 `@RequestBody @Valid AddressRequest.Create request` 로
- **IMPLEMENT**: 한 줄. `updateAddress` 는 이미 `@Valid`
- **MIRROR**: `CONTROLLER_VALID`
- **IMPORTS**: `jakarta.validation.Valid` (이미 import 되어 있음 — `Update` 서명에서 사용 중)
- **GOTCHA**: 전역 예외 핸들러가 `MethodArgumentNotValidException` 을 `ApiResponse` 형태로 변환하는지 확인 (`grep -rn "MethodArgumentNotValidException\|@RestControllerAdvice" service/user service/common`). 없으면 400 바디가 프론트 계약과 안 맞음 → 핸들러 추가를 이 Task 에 포함
- **VALIDATE**: 잘못된 `zipcode="abc"` 로 `POST` → HTTP 400 + `message` 있는 `ApiResponse`

### Task 4: 응답 DTO + 공유 DTO
- **ACTION**: `application/dto/AddressResponse.java` 의 `Get` + `AddressDto` 수정
- **IMPLEMENT**:
  - `AddressResponse.Get` 에 5개 필드 추가, `of(Address)` 빌더에 `.roadAddress(address.getRoadAddress())` … 매핑
  - `AddressDto` 는 **소비처 grep 결과에 따라**: user 서비스 내부 전용이면 5개 필드 추가; 타 서비스가 위치기반 생성/역직렬화하면 이번 스코프에서 빼고 Notes 에 후속 티켓 기록
- **MIRROR**: `RESPONSE_MAPPING`
- **IMPORTS**: 없음
- **GOTCHA**: 기존 행은 신규 필드가 NULL → 응답 JSON 에 `null` 로 나감. 프론트 `addressSchema` 는 `.nullable().optional()` 로 받아야 함(프론트 계획서). `@Builder` 라 NULL 대입은 안전
- **VALIDATE**: `GET /api/address/me` → 기존 주소는 신규 필드 `null`, 신규 저장 주소는 값 채워짐

### Task 5: 마이그레이션 SQL + README 등재
- **ACTION**: `docs/migrations/user/20260903-address-structured-columns.sql` 생성, `docs/migrations/README.md` 에 등재
- **IMPLEMENT**:
  - SQL (`20260610-user-devices.sql` 형식 미러):
    ```sql
    ALTER TABLE p_address
      ADD COLUMN road_address   VARCHAR(255) NULL,
      ADD COLUMN jibun_address  VARCHAR(255) NULL,
      ADD COLUMN detail_address VARCHAR(100) NULL,
      ADD COLUMN sido           VARCHAR(40)  NULL,
      ADD COLUMN sigungu        VARCHAR(40)  NULL;
    ```
  - `README.md` 의 번호 목록에 "`user/20260903-address-structured-columns.sql` to the user-service MySQL database — adds structured road-address columns to `p_address` (all nullable, safe to run before code deploy)." 추가
  - dev/local: `ddl-auto=update` 면 기동 시 자동 반영되지만, 스크립트는 prod/재현용으로 커밋
- **MIRROR**: `docs/migrations/user/20260610-user-devices.sql` 파일명·DDL 스타일, `docs/migrations/README.md` 항목 서술 방식
- **GOTCHA**: `ddl-auto=validate` 환경이면 이 SQL 을 배포 **전** 실행해야 앱이 기동됨. 컬럼 길이는 기존 `p_address` 정의 확인 후 조정
- **VALIDATE**: 스크립트 적용 후 `SHOW COLUMNS FROM p_address;` 에 5개 컬럼(전부 NULL 허용) 존재

### Task 6: 서비스 테스트
- **ACTION**: `test/.../AddressServiceTest.java` 케이스 추가
- **IMPLEMENT** (실제 `AddressRequest.Create` 인스턴스 사용 — `mock()` 말고 `new`):
  1. `createAddress_structuredFields_persisted`: road/jibun/detail/sido/sigungu 채운 요청 → `save` 로 넘어온 `Address` 캡처(`ArgumentCaptor<Address>`)해 필드 검증
  2. `createAddress_composesAddressWhenAbsent`: `address=null`, `roadAddress="서울 성동구 왕십리로 222"`, `detailAddress="101동 1001호"` → 저장된 `address == "서울 성동구 왕십리로 222 101동 1001호"`
  3. `createAddress_keepsExplicitAddress`: `address="레거시 문자열"` 도 함께 오면 그대로 저장(하위호환 — `AddressCard.patchDefault` 시나리오)
  4. `updateAddress_structuredFields_updated`: 기존 `Address` mock/spy 에 `update()` 호출 후 필드 반영
  - 검증(`@Pattern` 등)은 컨트롤러 레이어라 서비스 단위 테스트 대상 아님 → 필요 시 `@WebMvcTest(AddressController.class)` + `@MockBean AddressService` 슬라이스 테스트를 **별 파일** `AddressControllerValidationTest` 로 추가(선택)
- **MIRROR**: `SERVICE_TEST`
- **IMPORTS**: `import org.mockito.ArgumentCaptor;`
- **GOTCHA**: 기존 테스트는 `AddressRequest.Create request = mock(...)` 를 씀 → 새 케이스는 실제 객체 필요. Lombok `@AllArgsConstructor` 인자 순서 주의(Task 2 에서 신규 필드 뒤에 붙였으면 순서 안전)
- **VALIDATE**: `./gradlew :service:user:server:test --tests '*AddressServiceTest'`

### Task 7: OpenAPI 재생성
- **ACTION**: `python3 scripts/api/dump_openapi.py user` → `docs/api/user.json` 갱신, 커밋
- **IMPLEMENT**: 스크립트 실행. diff 에 address 요청/응답 스키마 신규 필드 확인
- **GOTCHA**: 프론트 repo 에서 `npm run contracts:sync` (또는 `contracts:check` 로 확인) 후 `addressSchema` 확장 — 이는 **프론트 계획서** 작업. 백엔드는 `user.json` 커밋까지
- **VALIDATE**: `git diff docs/api/user.json` 에 `roadAddress` 등 등장. 프론트 `npm run contracts:check` 가 최신 아님을 감지

---

## Testing Strategy

### Unit Tests
| Test | Input | Expected Output | Edge Case? |
|---|---|---|---|
| 구조화 필드 저장 | road/jibun/detail/sido/sigungu 채운 Create | 저장된 Address 에 5개 필드 동일 | |
| address 합성 | `address=null`, road + detail | `address == road + " " + detail` | ✅ |
| detail 없이 합성 | `address=null`, road only | `address == road` (뒤 공백 없음) | ✅ |
| 명시 address 우선 | `address="X"` + road | 저장 `address == "X"` | ✅ (하위호환) |
| update 반영 | Update 로 구조화 필드 변경 | 엔티티 필드 갱신 | |
| zipcode 형식 거부 | `zipcode="1234"` (컨트롤러 슬라이스) | HTTP 400, `ApiResponse.message` | ✅ |
| 빈 recipient 거부 | `recipient="  "` | HTTP 400 | ✅ |
| 레거시 요청(신규 필드 전무) | 기존 body 그대로 | 200, 기존과 동일 저장 | ✅ (하위호환) |

### Edge Cases Checklist
- [x] 신규 필드 전무한 레거시 요청 → 정상
- [x] `address` 명시 + 구조화 필드 동시 → 명시 우선
- [x] road 만, detail 없음 → 합성 시 trailing space 없음
- [x] 잘못된 우편번호/빈 문자열 → 400
- [ ] 기존 DB 행 조회 시 신규 필드 NULL → 응답 `null`, NPE 없음 (수동/통합)
- [ ] `ddl-auto=validate` 환경에서 컬럼 선행 필요 (배포 런북)

---

## Validation Commands

### Static Analysis / Build
```bash
./gradlew :service:user:server:compileJava
```
EXPECT: 컴파일 성공

### Unit Tests
```bash
./gradlew :service:user:server:test --tests '*AddressServiceTest'
```
EXPECT: 신규 포함 전부 통과

### Full Module Test
```bash
./gradlew :service:user:server:test
```
EXPECT: 회귀 없음

### DB Validation
```bash
# 로컬 기동 후
mysql -e "SHOW COLUMNS FROM p_address;" <db>
```
EXPECT: road_address, jibun_address, detail_address, sido, sigungu 존재 (전부 NULL 허용)

### Contract Validation
```bash
python3 scripts/api/dump_openapi.py user
git diff --stat docs/api/user.json
# 프론트 repo:  npm run contracts:check   → "최신 아님" 이 정상(프론트가 sync 예정)
```

### Manual Validation
- [ ] `POST /api/address` 구조화 필드 포함 → 201, `GET /api/address/me` 에 필드 반영
- [ ] `POST /api/address` 레거시 body(신규 필드 없음) → 201, 기존과 동일
- [ ] `PATCH /api/address/{id}` 로 `AddressCard.patchDefault` 식 `address` 만 재전송 → 200
- [ ] `zipcode="abc"` → 400 + 메시지

---

## Acceptance Criteria
- [ ] 모든 Task 완료
- [ ] `./gradlew :service:user:server:test` 통과
- [ ] `AddressServiceTest` 신규 케이스 작성·통과
- [ ] `docs/api/user.json` 재생성·커밋
- [ ] 레거시 요청 하위호환 확인(신규 필드 없이 201/200)
- [ ] 잘못된 입력 400 + `ApiResponse` 형태

## Completion Checklist
- [ ] 신규 컬럼 전부 nullable, 기존 `address` 컬럼·의미 유지
- [ ] `create`/`update` 팩토리 패턴 준수(`@Builder` 우회 안 함)
- [ ] 예외는 `UserException`+`UserErrorCode` / 검증 400 은 전역 핸들러 경유
- [ ] 테스트는 `@ExtendWith(MockitoExtension)` + `@DisplayName` + AssertJ
- [ ] `AddressDto` 소비처 grep 후 반영 범위 결정(같이 or 후속)
- [ ] Lombok 생성자 인자 순서: 신규 필드는 뒤에 추가
- [ ] 스코프 밖(외부 API 프록시, Flyway, NOT NULL 승격, alias 필수화) 손대지 않음

## Risks
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| 전역 검증 예외 핸들러 부재로 400 바디가 계약과 불일치 | 중간 | 중간 | Task 3 에서 `@RestControllerAdvice` 존재 확인, 없으면 추가 |
| `ddl-auto=validate` 환경에서 기동 실패 | 중간 | 높음 | prod ALTER 5줄을 배포 전 선행(런북) |
| `AddressDto` 변경이 order/delivery 역직렬화 깨뜨림 | 중간 | 높음 | 소비처 grep 우선, 불확실하면 DTO 변경 분리 |
| Lombok `@AllArgsConstructor` 인자 순서 변화로 기존 테스트 깨짐 | 낮음 | 낮음 | 신규 필드 뒤에 추가, 위치기반 호출 지양 |
| phoneNumber 정규식이 기존 데이터와 불일치 | 중간 | 중간 | 우선 `@NotBlank` 만, 정규식은 기존 값 샘플 확인 후 |

## Notes
- **왜 이 스코프(구조화 + 검증)인가**: Daum 위젯은 키 없는 클라이언트 완결형이라 백엔드가 외부 API 를 부를 이유가 없다. 대신 위젯이 주는 구조화 payload 를 평평한 문자열로 뭉개면(현재 프론트 `"  "` 해킹) 데이터 손실·파싱 취약이 생긴다. 위젯 도입과 동시에 모델을 구조화하는 게 정합적이고, `address` 유지로 하위호환도 확보된다.
- **더 가벼운 대안(참고)**: "입력 검증만" — 스키마 그대로 두고 `zipcode`/필수값 Bean Validation 만. 마이그레이션·계약변경 없음. 빠르게 가려면 Task 2·3·6 만 수행. 단 `"  "` 해킹은 남는다.
- **프론트 연동**: 이 계획 머지 → `docs/api/user.json` 커밋 → 프론트 `npm run contracts:sync` + `addressSchema` 확장 + 프론트 계획서 Task 4-10 의 구조화 필드 전송 활성화.
- `application.yml` 이 `configserver:` 만 import 하므로 datasource/JPA 설정은 이 repo 밖. `ddl-auto` 실제 값은 config 담당 확인 필요.
- 마이그레이션은 `docs/migrations/README.md` 가 관리하는 수동 운영 스크립트 방식. 자동 실행(Flyway/Liquibase)은 없음.
