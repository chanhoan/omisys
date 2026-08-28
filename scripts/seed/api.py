"""product 서비스 HTTP 클라이언트.

게이트웨이를 거치지 않고 product 서비스에 직접 붙는다. 게이트웨이는 JWT 를 검증한
뒤 X-User-Claims 헤더를 넣어 주는 역할을 하고, product 서비스는 그 헤더만 보기
때문이다(SecurityContextFilter). 시더는 그 헤더를 직접 만들어 로그인 절차를 건너뛴다.

표준 라이브러리만 쓴다. requests 를 깔지 않아도 어느 PC 에서나 그대로 돈다.
"""

import json
import urllib.error
import urllib.parse
import urllib.request
import uuid

X_USER_CLAIMS = "X-User-Claims"

# JwtAuthentication 이 role 문자열을 그대로 권한으로 쓰고, 컨트롤러는
# hasAnyRole('ROLE_ADMIN') 을 요구한다. 그래서 role 은 접두사까지 포함해야 한다.
ADMIN_CLAIMS = {"userId": 1, "username": "seeder", "role": "ROLE_ADMIN"}


class ApiError(RuntimeError):
    def __init__(self, status, body, url):
        super().__init__("{} {} -> {}".format(status, url, body[:400]))
        self.status = status
        self.body = body


def discover_base_url(eureka_url, app="PRODUCT", timeout=5):
    """Eureka 에서 product 서비스 포트를 찾는다. 못 찾으면 None.

    로컬 포트는 config 저장소(private)에 있어 저장소만 봐서는 알 수 없다.
    이미 떠 있는 인스턴스에 물어보는 편이 포트를 외우게 하는 것보다 낫다.
    """
    url = "{}/eureka/apps/{}".format(eureka_url.rstrip("/"), app)
    request = urllib.request.Request(url, headers={"Accept": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except (urllib.error.URLError, ValueError, OSError):
        return None

    for instance in payload.get("application", {}).get("instance", []):
        if instance.get("status") != "UP":
            continue
        port = (instance.get("port") or {}).get("$")
        if port:
            # Eureka 에는 컨테이너 내부 IP 가 실릴 수 있다. 로컬 실행이면 localhost 가 맞다.
            return "http://localhost:{}".format(port)
    return None


def encode_multipart(parts, boundary):
    """(name, filename, content_type, bytes) 목록을 multipart 본문으로 만든다."""
    buffer = bytearray()
    for name, filename, content_type, payload in parts:
        buffer += b"--" + boundary.encode() + b"\r\n"
        disposition = 'form-data; name="{}"'.format(name)
        if filename:
            disposition += '; filename="{}"'.format(filename)
        buffer += "Content-Disposition: {}\r\n".format(disposition).encode("utf-8")
        buffer += "Content-Type: {}\r\n\r\n".format(content_type).encode("utf-8")
        buffer += payload + b"\r\n"
    buffer += b"--" + boundary.encode() + b"--\r\n"
    return bytes(buffer)


class ProductApi:
    def __init__(self, base_url, claims=None, timeout=60):
        self.base_url = base_url.rstrip("/")
        self.claims = claims or ADMIN_CLAIMS
        self.timeout = timeout

    def _headers(self):
        encoded = urllib.parse.quote(json.dumps(self.claims, ensure_ascii=False))
        return {X_USER_CLAIMS: encoded}

    def _send(self, method, path, body=None, content_type=None):
        url = self.base_url + path
        headers = self._headers()
        if content_type:
            headers["Content-Type"] = content_type

        request = urllib.request.Request(url, data=body, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                raw = response.read().decode("utf-8")
        except urllib.error.HTTPError as e:
            raise ApiError(e.code, e.read().decode("utf-8", "replace"), url)
        except urllib.error.URLError as e:
            raise RuntimeError("{} 에 연결하지 못했습니다: {}".format(url, e.reason))

        if not raw:
            return None
        return json.loads(raw).get("data")

    def list_categories(self):
        """등록된 카테고리 트리. 인증 없이도 열려 있는 경로다."""
        return self._send("GET", "/api/categories/search") or []

    def create_category(self, name, parent_category_id=None):
        payload = {"name": name, "parentCategoryId": parent_category_id}
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        return self._send("POST", "/api/categories", body, "application/json")

    def create_product(self, request_payload, origin_image, detail_image):
        """multipart 로 상품을 만든다.

        request 파트에 Content-Type: application/json 을 반드시 붙여야 한다.
        없으면 @RequestPart("request") 가 JSON 으로 역직렬화하지 못한다.
        """
        parts = [
            (
                "request",
                None,
                "application/json",
                json.dumps(request_payload, ensure_ascii=False).encode("utf-8"),
            ),
            ("productImg", "product.jpg", "image/jpeg", origin_image),
            ("detailImg", "detail.jpg", "image/jpeg", detail_image),
        ]
        boundary = uuid.uuid4().hex
        body = encode_multipart(parts, boundary)
        return self._send(
            "POST", "/api/products", body, "multipart/form-data; boundary={}".format(boundary)
        )
