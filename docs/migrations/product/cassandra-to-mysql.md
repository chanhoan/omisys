# Product 데이터 이관: Cassandra → MySQL

product 서비스의 `Product` 엔티티가 Cassandra(`product` 키스페이스, `P_PRODUCT`)에서 통합 MySQL(`omisys_product` 스키마)로 이동한다.
`ddl-auto: update`이므로 **테이블은 애플리케이션 기동 시 자동 생성**되지만 **데이터는 옮겨지지 않는다.** 아래 절차로 수동 이관한다.

> **먼저 확인할 것**: 목데이터를 새로 시딩할 예정이라면 **이 절차 전체를 생략하고 빈 테이블에서 시작하는 편이 합리적이다.**
> 보존해야 할 실데이터가 있을 때만 진행한다.

명령에 등장하는 `:MYSQL_PASSWORD` 등 `:` 접두 값은 **플레이스홀더**다. 실행 시점에 실제 값으로 치환하고, 실제 비밀번호를 커밋하지 않는다
(관례는 `docs/migrations/README.md` 참조).

---

## 1. 사전 준비

- product 서비스를 **중단**한다. 이관 중 쓰기가 들어오면 유실된다.
- Cassandra 스냅샷을 확보한다 (롤백 근거).

  ```bash
  docker exec cassandra nodetool snapshot -t pre-mysql-migration product
  docker exec cassandra nodetool listsnapshots
  ```

- MySQL 측 대상 스키마와 테이블이 준비됐는지 확인한다. 테이블이 없으면 product 서비스를 한 번 기동해 Hibernate가 `P_PRODUCT` · `P_PRODUCT_TAG`를 생성하게 한 뒤 다시 내린다.

  ```bash
  docker exec -e MYSQL_PWD="$PRODUCT_MYSQL_PASSWORD" omisys-mysql \
    mysql -uomisys_product -e "USE omisys_product; SHOW TABLES;"
  ```

## 2. 추출

```bash
docker exec -it cassandra cqlsh -e \
  "COPY product.\"P_PRODUCT\" TO '/tmp/products.csv' WITH HEADER=true;"
docker cp cassandra:/tmp/products.csv ./products.csv
```

헤더 행의 컬럼 순서를 그대로 기록해 둔다. 다음 단계의 변환 스크립트가 이 순서에 의존한다.

## 3. 변환

CSV를 MySQL 스키마에 맞게 바꾼다. 손대야 할 지점은 두 가지다.

| 대상 | Cassandra | MySQL | 처리 |
|---|---|---|---|
| `product_id` | UUID 문자열 (`8-4-4-4-12`) | `BINARY(16)` | 하이픈 제거 후 `UNHEX()` |
| `tags` | 컬렉션 → CSV에 `['a','b']` 문자열 | `P_PRODUCT_TAG` 별도 행 | 문자열 파싱 후 행 분해 |
| `size` | `size` | `product_size` | 컬럼명 변경 (`@Column(name = "product_size")`) |

- **`tags` 파싱 주의**: `['casual','summer']` 형태의 리터럴이라 그대로 적재하면 안 된다. 대괄호·작은따옴표를 제거하고 쉼표로 분리해 `(product_id, tag)` 행으로 펼친다. 빈 컬렉션은 `[]` 또는 빈 값으로 나오므로 행을 만들지 않는다.
- **UUID 주의**: MySQL의 `UUID_TO_BIN()`은 기본적으로 바이트 재배치를 하지 않지만(`swap_flag=0`), Hibernate가 쓰는 표현과 어긋나지 않도록 **하이픈 제거 + `UNHEX()`** 로 통일한다. 한쪽만 `UUID_TO_BIN(..., 1)`을 쓰면 조회가 전부 빗나간다.
- 변환 스크립트는 일회성이므로 저장소에 남기지 않아도 되지만, 재실행 가능하도록 이관 기간 동안은 보관한다.

## 4. 적재

변환 결과를 두 파일(`products_rows.csv`, `product_tags_rows.csv`)로 만든 뒤 적재한다.

```sql
-- P_PRODUCT
LOAD DATA LOCAL INFILE 'products_rows.csv'
INTO TABLE omisys_product.P_PRODUCT
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@product_id, category_id, name, brand_name, original_price, /* ... 이하 컬럼 ... */)
SET product_id = UNHEX(REPLACE(@product_id, '-', ''));

-- P_PRODUCT_TAG
LOAD DATA LOCAL INFILE 'product_tags_rows.csv'
INTO TABLE omisys_product.P_PRODUCT_TAG
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@product_id, tag)
SET product_id = UNHEX(REPLACE(@product_id, '-', ''));
```

`LOAD DATA LOCAL INFILE`이 서버에서 막혀 있으면(`local_infile=OFF`) 변환 스크립트가 직접 배치 `INSERT`를 수행하도록 한다.

## 5. 검증

```sql
-- (1) 전체 건수 일치
SELECT COUNT(*) FROM omisys_product.P_PRODUCT;
-- Cassandra 측: SELECT COUNT(*) FROM product."P_PRODUCT";

-- (2) 삭제되지 않은 건수 일치
SELECT COUNT(*) FROM omisys_product.P_PRODUCT WHERE is_deleted = false;

-- (3) 태그 총 개수 (분해 결과 검증)
SELECT COUNT(*) FROM omisys_product.P_PRODUCT_TAG;

-- (4) 샘플 대조
SELECT LOWER(HEX(product_id)), name, brand_name, original_price, stock
FROM omisys_product.P_PRODUCT LIMIT 10;
```

- 건수 3종이 모두 원본과 일치해야 한다.
- 샘플 10건은 Cassandra 원본과 **필드 단위로 대조**한다. 특히 가격(소수점)·재고·`is_deleted`·`product_size`를 확인한다.
- 불일치가 있으면 여기서 멈추고 7번 롤백으로 되돌린다. 다음 단계로 넘어가면 원인 격리가 어려워진다.

## 6. Elasticsearch 재색인

product · search 서비스의 검색은 Elasticsearch가 담당하므로 MySQL과 정합성을 맞춘다.

- ES 문서 수와 `P_PRODUCT`의 `is_deleted = false` 건수를 비교한다.

  ```bash
  curl -s "http://localhost:9200/products/_count"
  ```

- 불일치하면 **부분 보정하지 말고 전량 재색인**한다. 인덱스를 삭제하고 MySQL을 원본으로 다시 색인한다.
- 재색인 후 대표 검색어 몇 건으로 결과가 정상인지 확인한다.

## 7. 롤백

검증 실패 시 되돌린다.

1. product 서비스를 다시 중단한다.
2. MySQL 측 데이터를 비운다.

   ```sql
   DROP TABLE omisys_product.P_PRODUCT_TAG;
   DROP TABLE omisys_product.P_PRODUCT;
   ```

   (스키마 전체를 되돌려야 하면 `DROP DATABASE omisys_product;` 후 재생성 — `01-init-schemas.sh`가 만드는 스키마와 같은 이름·charset 으로 재생성)
3. 1번에서 확보한 스냅샷으로 Cassandra를 복구한다 (`nodetool refresh` 또는 `sstableloader`).
4. 애플리케이션 코드를 이관 이전 커밋으로 되돌린 뒤 재기동한다.
5. ES는 복구된 Cassandra 기준으로 다시 재색인한다.

---

## 체크리스트

- [ ] product 서비스 중단 확인
- [ ] Cassandra 스냅샷 확보
- [ ] CSV 추출 및 헤더 순서 기록
- [ ] UUID `BINARY(16)` 변환
- [ ] `tags` 리스트 → `P_PRODUCT_TAG` 행 분해
- [ ] 건수 3종 일치
- [ ] 샘플 10건 필드 대조
- [ ] ES 재색인 및 문서 수 일치
- [ ] 롤백 경로 확인 (스냅샷 유효성)
