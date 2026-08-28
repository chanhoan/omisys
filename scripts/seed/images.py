"""상품 이미지 확보.

picsum.photos 에서 이미지 몇 쌍을 받아 로컬에 캐시하고 상품마다 돌려 쓴다.
사진이 그 상품일 필요는 없다. 중요한 것은 실제 바이너리가 S3 로 올라가
originImgUrl / detailImgUrl / thumbnailImgUrl 파이프라인이 도는지다.

picsum 이 돌려주는 것은 어차피 카테고리와 무관한 사진이므로 카테고리별로 따로
받지 않는다. 공용 풀 하나를 순번으로 돌려 쓰는 편이 다운로드를 수십 배 줄인다.

picsum 을 쓰는 이유는 저작권 부담이 없고 API 키가 필요 없기 때문이다.
seed 를 고정하면 같은 URL 이 같은 이미지를 돌려주므로 캐시가 안정적이다.
"""

import os
import sys
import time
import urllib.error
import urllib.request

CACHE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".cache")

PICSUM_URL = "https://picsum.photos/seed/{seed}/{size}/{size}.jpg"

# 풀 크기. 상품이 몇 개든 다운로드는 이 값의 2배(origin + detail)로 고정된다.
DEFAULT_POOL_SIZE = 24

ORIGIN_SIZE = 800
DETAIL_SIZE = 1200


def _download(url, dest, retries=3):
    last_error = None
    for attempt in range(retries):
        try:
            with urllib.request.urlopen(url, timeout=30) as response:
                data = response.read()
            if not data:
                raise IOError("빈 응답")
            with open(dest, "wb") as f:
                f.write(data)
            return
        except (urllib.error.URLError, IOError, OSError) as e:
            last_error = e
            # picsum 은 몰아치면 간헐적으로 5xx 를 낸다. 조금 쉬고 다시 시도한다.
            time.sleep(1.5 * (attempt + 1))
    raise RuntimeError("이미지 내려받기 실패: {} ({})".format(url, last_error))


def ensure_cache(pool_size=DEFAULT_POOL_SIZE, log=print):
    """이미지 풀을 채우고 [(origin_path, detail_path), ...] 를 돌려준다.

    이미 받아 둔 파일은 건너뛰므로 두 번째 실행부터는 네트워크를 타지 않는다.
    """
    os.makedirs(CACHE_DIR, exist_ok=True)
    pairs = []
    downloaded = 0

    for index in range(pool_size):
        paths = []
        for kind, size in (("origin", ORIGIN_SIZE), ("detail", DETAIL_SIZE)):
            seed = "omisys{:03d}{}".format(index, kind)
            path = os.path.join(CACHE_DIR, "{}.jpg".format(seed))
            if not os.path.exists(path):
                if downloaded == 0:
                    log("[info] 이미지 캐시를 채웁니다. 최대 {}장, 처음 한 번만 걸립니다.".format(pool_size * 2))
                _download(PICSUM_URL.format(seed=seed, size=size), path)
                downloaded += 1
                if downloaded % 8 == 0:
                    log("[info]   {}장 완료".format(downloaded))
                    sys.stdout.flush()
            paths.append(path)
        pairs.append(tuple(paths))

    if downloaded:
        log("[info] 이미지 {}장을 캐시했습니다: {}".format(downloaded, CACHE_DIR))
    else:
        log("[info] 이미지 캐시를 그대로 사용합니다: {}".format(CACHE_DIR))

    return pairs


def pick(pool, index):
    """상품 순번으로 캐시된 이미지 한 쌍(origin, detail)을 고른다."""
    return pool[index % len(pool)]


def read(path):
    with open(path, "rb") as f:
        return f.read()
