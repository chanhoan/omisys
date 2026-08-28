"""목상품 카탈로그 생성.

네트워크를 타지 않는 순수 데이터/로직만 둔다. 실제 투입은 seed.py 가 한다.

크롤링이나 외부 API 대신 조합 생성을 쓰는 이유:
P_PRODUCT 의 필수 필드(mainColor, size, limitCountPerUser, tags, description)는
어차피 외부에서 얻을 수 없어 만들어야 하고, 목데이터에 필요한 것은 진짜 상품이
아니라 검색·정렬·페이징이 의미 있게 동작할 만큼의 부피와 분포이기 때문이다.
"""

import random

# 카테고리 트리. (대분류, [중분류...]) — P_CATEGORY 는 self-referencing 이라 2단계로 충분하다.
CATEGORY_TREE = [
    ("패션의류", ["티셔츠", "셔츠", "니트", "아우터", "바지", "원피스"]),
    ("패션잡화", ["운동화", "구두", "가방", "지갑", "모자", "벨트"]),
    ("디지털/가전", ["노트북", "키보드", "마우스", "모니터", "이어폰", "충전기"]),
    ("홈/리빙", ["침구", "조명", "수납", "주방용품", "커튼", "러그"]),
    ("스포츠/레저", ["요가매트", "덤벨", "등산화", "텐트", "자전거용품", "수영복"]),
    ("뷰티", ["스킨케어", "클렌징", "선케어", "헤어케어", "향수", "바디케어"]),
]

BRANDS = [
    "노르딕랩", "밀레니엄", "코튼하우스", "어반스텝", "베이직핏", "레이어드",
    "포레스트", "블루문", "스튜디오온", "메이커스", "하루상점", "글로우업",
]

COLORS = ["블랙", "화이트", "그레이", "네이비", "베이지", "카키", "브라운", "아이보리", "버건디"]

# 카테고리군마다 사이즈 체계가 다르다.
SIZE_SETS = {
    "패션의류": ["XS", "S", "M", "L", "XL", "2XL"],
    "패션잡화": ["FREE", "230", "240", "250", "260", "270", "280"],
    "디지털/가전": ["단일"],
    "홈/리빙": ["S", "M", "L", "단일"],
    "스포츠/레저": ["S", "M", "L", "FREE"],
    "뷰티": ["30ml", "50ml", "100ml", "200ml"],
}

MODIFIERS = [
    "베이직", "오버핏", "슬림", "데일리", "프리미엄", "라이트", "클래식",
    "미니멀", "소프트", "스탠다드", "컴팩트", "와이드",
]

DESCRIPTION_TEMPLATES = [
    "{brand}의 {modifier} {sub}. {color} 컬러로 어디에나 무난하게 어울립니다.",
    "매일 손이 가는 {modifier} {sub}입니다. {size} 사이즈, {color} 컬러.",
    "{sub} 본연에 충실한 구성. {brand}가 {color} 톤으로 정리했습니다.",
    "{modifier}한 실루엣의 {sub}. 계절을 타지 않아 오래 씁니다.",
]

TAG_POOL = [
    "신상", "베스트", "무료배송", "당일발송", "리뷰이벤트", "한정수량",
    "선물추천", "가성비", "시즌오프", "재입고",
]

# 할인율 분포. 0% 를 가장 흔하게 둬 할인 없는 상품이 다수가 되게 한다.
DISCOUNT_WEIGHTS = [(0.0, 45), (5.0, 10), (10.0, 15), (15.0, 10), (20.0, 10), (30.0, 7), (50.0, 3)]

# 재고 분포. 품절(0)과 1개 남음을 섞어 엣지 케이스를 만든다.
STOCK_WEIGHTS = [(0, 5), (1, 5), (10, 20), (50, 30), (200, 25), (999, 15)]

# 카테고리군별 가격대(원).
PRICE_RANGES = {
    "패션의류": (9_000, 150_000),
    "패션잡화": (15_000, 300_000),
    "디지털/가전": (20_000, 2_500_000),
    "홈/리빙": (8_000, 400_000),
    "스포츠/레저": (10_000, 800_000),
    "뷰티": (7_000, 120_000),
}


def _weighted(rng, pairs):
    """(값, 가중치) 목록에서 하나 고른다."""
    values = [v for v, _ in pairs]
    weights = [w for _, w in pairs]
    return rng.choices(values, weights=weights, k=1)[0]


def _price_for(top_name, rng):
    """990 원 단위로 끊어 쇼핑몰 가격표처럼 보이게 한다."""
    low, high = PRICE_RANGES.get(top_name, (10_000, 100_000))
    raw = rng.randint(low // 1000, high // 1000) * 1000
    return raw - 10 if raw > 1000 else raw


def generate_products(count, seed=None):
    """count 개의 상품 dict 를 만든다. categoryId 는 아직 채우지 않는다.

    같은 seed 면 같은 결과가 나오므로 다시 돌려도 동일한 카탈로그를 얻는다.
    반환 dict 의 'top'/'sub' 은 카테고리 매핑과 이미지 선택에 쓰이는 내부 필드다.
    """
    rng = random.Random(seed)
    pairs = [(top, sub) for top, subs in CATEGORY_TREE for sub in subs]
    products = []

    for i in range(count):
        # 순환 배정이라 개수가 적어도 모든 카테고리에 최소 한 건씩 들어간다.
        top, sub = pairs[i % len(pairs)]
        brand = rng.choice(BRANDS)
        color = rng.choice(COLORS)
        size = rng.choice(SIZE_SETS.get(top, ["FREE"]))
        modifier = rng.choice(MODIFIERS)

        products.append(
            {
                "top": top,
                "sub": sub,
                "productName": f"[{brand}] {modifier} {sub} {color}",
                "brandName": brand,
                "mainColor": color,
                "size": size,
                "originalPrice": _price_for(top, rng),
                "discountPercent": _weighted(rng, DISCOUNT_WEIGHTS),
                "stock": _weighted(rng, STOCK_WEIGHTS),
                "description": rng.choice(DESCRIPTION_TEMPLATES).format(
                    brand=brand, modifier=modifier, sub=sub, color=color, size=size
                ),
                "limitCountPerUser": _weighted(rng, [(0, 70), (1, 10), (2, 10), (5, 10)]),
                "tags": rng.sample(TAG_POOL, rng.randint(1, 3)),
            }
        )

    return products


def to_create_request(product, category_id):
    """생성된 dict 를 ProductRequest.Create 형태로 좁힌다."""
    return {
        "categoryId": category_id,
        "productName": product["productName"],
        "brandName": product["brandName"],
        "mainColor": product["mainColor"],
        "size": product["size"],
        "originalPrice": product["originalPrice"],
        "discountPercent": product["discountPercent"],
        "stock": product["stock"],
        "description": product["description"],
        "limitCountPerUser": product["limitCountPerUser"],
        "tags": product["tags"],
    }
