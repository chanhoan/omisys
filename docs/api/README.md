# API 계약

각 서비스가 내보내는 OpenAPI 3.0.1 문서를 그대로 담아 둔다. 프론트엔드는 별도
저장소(`omisys_frontend`)에 있고 백엔드를 띄울 수 없으므로, 스펙을 파일로 커밋해
서버 없이도 타입을 생성할 수 있게 한다.

| 서비스 | 파일 | 경로 | 오퍼레이션 |
|---|---|---:|---:|
| auth | `auth.json` | 4 | 4 |
| delivery | `delivery.json` | 7 | 7 |
| order | `order.json` | 14 | 16 |
| payment | `payment.json` | 8 | 8 |
| product | `product.json` | 18 | 23 |
| promotion | `promotion.json` | 11 | 16 |
| review | `review.json` | 3 | 5 |
| search | `search.json` | 1 | 1 |
| user | `user.json` | 27 | 35 |

notification 서비스는 컨트롤러가 없다. 이벤트로만 동작하므로 스펙도 없다.

## 갱신

컨트롤러나 DTO 를 고쳤다면 다시 뽑아서 함께 커밋한다.

```bash
python3 scripts/api/dump_openapi.py            # 전체
python3 scripts/api/dump_openapi.py product    # 일부만
python3 scripts/api/dump_openapi.py --check    # 저장하지 않고 최신인지만 확인
```

대상 서비스가 떠 있어야 한다. 기동 절차는 [../development/local-setup.md](../development/local-setup.md) 참조.

> **서비스를 한꺼번에 띄우지 말 것.** `--parallel` 로 여러 개를 동시에 올리면 Config Server
> 응답을 받지 못해 설정이 통째로 빈 채 기동에 실패한다. 그런데 에러 메시지는
> "Failed to determine a suitable driver class" 라 원인이 전혀 드러나지 않는다.
> 하나씩, 혹은 두 개씩 띄운다.

## 프론트엔드에서 쓰기

`omisys_frontend` 에서 타입을 생성한다.

```bash
npx openapi-typescript ../omisys/docs/api/product.json -o src/api/product.d.ts
```

응답은 전부 같은 봉투를 쓴다. 제네릭이 타입별로 전개되므로 `data` 가 `unknown` 이 아니다.

```jsonc
// ApiResponsePageProductResponse
{
  "statusName": "OK",     // "CREATED" | "OK" | 에러 코드 이름
  "message": null,        // 에러일 때만 채워진다
  "data": { /* PageProductResponse */ }
}
```

## 호출 경로

프론트엔드가 닿을 수 있는 것은 **게이트웨이뿐**이다. 서비스 포트(`18081` 등)는 로컬에서
스펙을 뽑을 때만 쓰고, 실제 호출은 전부 게이트웨이를 지난다.

```
브라우저 → 게이트웨이(로컬 19091) → 서비스
```

경로 접두사로 라우팅되므로 스펙에 적힌 경로를 그대로 게이트웨이에 붙이면 된다.
`/api/products/**` → product, `/api/orders/**` · `/api/carts/**` → order 하는 식이다.

## 인증

스펙의 `accessToken` 보안 스킴을 보면 된다. 로그인(`POST /api/auth/sign-in`)이 내려주는
쿠키를 그대로 실어 보내면 되고, 게이트웨이가 검증한 뒤 `X-User-Claims` 헤더로 바꿔 각
서비스에 넘긴다. **브라우저가 그 헤더를 직접 만들 필요는 없다.**

권한이 필요한 엔드포인트는 설명 끝에 `필요 권한: ROLE_ADMIN 또는 ROLE_MANAGER` 처럼
적혀 있다. 오퍼레이션 115개 중 57개가 여기 해당한다.

이 표기는 손으로 단 것이 아니라 컨트롤러의 `@PreAuthorize` 를 읽어서 만든다
(`common:domain` 의 `OmisysRoleDocAutoConfiguration`). 권한 규칙을 바꾸면 스펙을 다시 뽑는
것만으로 따라온다.

## 지금 스펙에 없는 것

- **오퍼레이션 설명이 대체로 비어 있다.** `summary` 가 붙은 것은 상품 등록 · 수정 정도이고,
  나머지는 경로 · 메서드 · 타입과 필요 권한만 알 수 있다.
- **에러 응답이 문서화되어 있지 않다.** 성공 스키마만 있고 실패 시 어떤 `statusName` 이
  오는지는 적혀 있지 않다.
