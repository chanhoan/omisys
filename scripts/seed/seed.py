#!/usr/bin/env python3
"""쇼핑몰 목데이터를 product 서비스 API 로 밀어 넣는다.

API 를 거치는 이유는 SQL 로 직접 INSERT 하면 S3 업로드도, outbox -> Kafka ->
Elasticsearch 색인도 타지 않아 이미지와 검색이 통째로 비기 때문이다.

사용법:
    python3 scripts/seed/seed.py                  # 300건, Eureka 로 포트 자동 탐색
    python3 scripts/seed/seed.py --count 50
    python3 scripts/seed/seed.py --base-url http://localhost:19093
    python3 scripts/seed/seed.py --dry-run        # 아무것도 보내지 않고 미리보기

먼저 SSH 터널과 config-server / eureka-service / product-service 가 떠 있어야 한다.
자세한 것은 docs/development/local-setup.md 참조.
"""

import argparse
import sys
import time

import api
import catalog
import images

# Windows 콘솔이 cp949 면 일부 문자에서 죽는다. 출력 때문에 시딩이 멈추면 안 된다.
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(errors="replace")
    except AttributeError:
        pass

DEFAULT_COUNT = 300
DEFAULT_EUREKA = "http://localhost:19090"
PROGRESS_EVERY = 25


def parse_args(argv):
    parser = argparse.ArgumentParser(description="쇼핑몰 목데이터 시더")
    parser.add_argument("--count", type=int, default=DEFAULT_COUNT, help="생성할 상품 수")
    parser.add_argument("--base-url", help="product 서비스 주소. 생략하면 Eureka 로 찾는다")
    parser.add_argument("--eureka-url", default=DEFAULT_EUREKA, help="Eureka 주소")
    parser.add_argument("--seed", type=int, default=20260828, help="난수 시드. 같으면 같은 카탈로그")
    parser.add_argument("--dry-run", action="store_true", help="전송 없이 생성 결과만 본다")
    parser.add_argument("--stop-on-error", action="store_true", help="첫 실패에서 멈춘다")
    return parser.parse_args(argv)


def resolve_base_url(args):
    if args.base_url:
        return args.base_url

    found = api.discover_base_url(args.eureka_url)
    if found:
        print("[info] Eureka 에서 product 서비스를 찾았습니다: {}".format(found))
        return found

    print("[error] product 서비스를 찾지 못했습니다.", file=sys.stderr)
    print("        eureka-service 가 떠 있는지 확인하거나 --base-url 로 직접 주십시오.", file=sys.stderr)
    return None


def ensure_categories(client):
    """카테고리 트리를 등록하고 {중분류명: categoryId} 를 돌려준다.

    이미 있는 이름은 건너뛰므로 여러 번 돌려도 중복이 쌓이지 않는다.
    """
    existing = {}
    for top in client.list_categories():
        existing[top["name"]] = top["categoryId"]
        for sub in top.get("subCategories") or []:
            existing[sub["name"]] = sub["categoryId"]

    leaf_ids = {}
    created = 0

    for top_name, sub_names in catalog.CATEGORY_TREE:
        top_id = existing.get(top_name)
        if top_id is None:
            top_id = client.create_category(top_name)
            existing[top_name] = top_id
            created += 1

        for sub_name in sub_names:
            sub_id = existing.get(sub_name)
            if sub_id is None:
                sub_id = client.create_category(sub_name, top_id)
                existing[sub_name] = sub_id
                created += 1
            leaf_ids[sub_name] = sub_id

    print("[info] 카테고리 준비 완료 - 신규 {}개, 전체 리프 {}개".format(created, len(leaf_ids)))
    return leaf_ids


def seed_products(client, products, category_ids, image_pool, stop_on_error):
    succeeded = 0
    failures = []
    started = time.time()

    for index, product in enumerate(products):
        category_id = category_ids[product["sub"]]
        payload = catalog.to_create_request(product, category_id)
        origin_path, detail_path = images.pick(image_pool, index)

        try:
            client.create_product(payload, images.read(origin_path), images.read(detail_path))
            succeeded += 1
        except (api.ApiError, RuntimeError) as e:
            failures.append((product["productName"], str(e)))
            if stop_on_error:
                break

        if (index + 1) % PROGRESS_EVERY == 0:
            elapsed = time.time() - started
            print("[info] {}/{} 건 ({:.1f}초)".format(index + 1, len(products), elapsed))

    return succeeded, failures


def print_preview(products):
    print("[info] --dry-run 이라 아무것도 보내지 않습니다. 앞 3건만 보입니다.\n")
    for product in products[:3]:
        for key, value in catalog.to_create_request(product, 0).items():
            print("  {:<18} {}".format(key, value))
        print()


def main(argv):
    args = parse_args(argv)

    products = catalog.generate_products(args.count, seed=args.seed)
    print("[info] 상품 {}건 생성 (seed={})".format(len(products), args.seed))

    if args.dry_run:
        print_preview(products)
        return 0

    base_url = resolve_base_url(args)
    if not base_url:
        return 1

    image_pool = images.ensure_cache()

    client = api.ProductApi(base_url)
    try:
        category_ids = ensure_categories(client)
    except (api.ApiError, RuntimeError) as e:
        print("[error] 카테고리 등록에 실패했습니다: {}".format(e), file=sys.stderr)
        return 1

    succeeded, failures = seed_products(
        client, products, category_ids, image_pool, args.stop_on_error
    )

    print("\n[info] 완료 - 성공 {}건 / 실패 {}건".format(succeeded, len(failures)))
    if failures:
        print("[error] 실패 목록 (앞 5건):", file=sys.stderr)
        for name, reason in failures[:5]:
            print("  - {}: {}".format(name, reason), file=sys.stderr)
        return 1

    print("[info] 검색 색인은 outbox -> Kafka 를 거치므로 몇 초 뒤에 반영됩니다.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
