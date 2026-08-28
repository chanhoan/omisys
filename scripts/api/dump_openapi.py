#!/usr/bin/env python3
"""각 서비스의 OpenAPI 문서를 받아 docs/api/ 에 저장한다.

프론트엔드는 별도 저장소(omisys_frontend)에 있고 백엔드를 띄울 수 없다. 스펙을 파일로
커밋해 두면 서버 없이도 타입을 생성할 수 있다.

사용법:
    python3 scripts/api/dump_openapi.py            # 전체
    python3 scripts/api/dump_openapi.py product    # 일부만
    python3 scripts/api/dump_openapi.py --check    # 저장하지 않고 최신인지만 확인

서비스가 떠 있어야 한다. Eureka 등록은 죽은 인스턴스도 한동안 남으므로 믿지 않고
포트에 직접 물어본다.
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.request

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUTPUT_DIR = os.path.join(REPO_ROOT, "docs", "api")

# 포트는 설정 저장소(private)에 있어 여기서 읽을 수 없다. 로컬 기동 시 고정값이라 적어 둔다.
SERVICES = {
    "auth": 18082,
    "user": 18081,
    "product": 18083,
    "order": 18084,
    "payment": 18085,
    "promotion": 18087,
    "search": 18088,
    "review": 18089,
    "delivery": 18091,
}

DOCS_PATH = "/v3/api-docs"
TIMEOUT = 20


def fetch(port):
    url = "http://localhost:{}{}".format(port, DOCS_PATH)
    with urllib.request.urlopen(url, timeout=TIMEOUT) as response:
        return json.loads(response.read().decode("utf-8"))


def normalize(document, service):
    """저장 전에 다듬는다.

    springdoc 은 기동할 때마다 servers 에 그때의 주소를 넣는다. 그대로 두면 내용이 같아도
    매번 diff 가 나므로 지운다. title 은 전부 "OpenAPI definition" 이라 서비스 구분이 안 된다.
    """
    document.pop("servers", None)
    info = document.setdefault("info", {})
    if info.get("title") in (None, "", "OpenAPI definition"):
        info["title"] = "omisys {} service".format(service)
    return document


def write(document, path):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(document, f, ensure_ascii=False, indent=2, sort_keys=True)
        f.write("\n")


def summarize(document):
    paths = document.get("paths", {})
    methods = ("get", "post", "put", "patch", "delete")
    operations = sum(len([m for m in ops if m in methods]) for ops in paths.values())
    schemas = len(document.get("components", {}).get("schemas", {}))
    return operations, len(paths), schemas


def main(argv):
    parser = argparse.ArgumentParser(description="OpenAPI 문서 덤프")
    parser.add_argument("services", nargs="*", help="대상 서비스. 생략하면 전체")
    parser.add_argument("--check", action="store_true", help="저장하지 않고 최신 여부만 확인")
    args = parser.parse_args(argv)

    targets = args.services or sorted(SERVICES)
    unknown = [s for s in targets if s not in SERVICES]
    if unknown:
        print("[error] 알 수 없는 서비스: {}".format(", ".join(unknown)), file=sys.stderr)
        print("        가능한 값: {}".format(", ".join(sorted(SERVICES))), file=sys.stderr)
        return 2

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    unreachable, stale, written = [], [], 0

    for service in targets:
        path = os.path.join(OUTPUT_DIR, "{}.json".format(service))
        try:
            document = normalize(fetch(SERVICES[service]), service)
        except (urllib.error.URLError, OSError, ValueError) as e:
            unreachable.append((service, SERVICES[service], e))
            continue

        operations, path_count, schemas = summarize(document)

        if args.check:
            current = None
            if os.path.exists(path):
                with open(path, encoding="utf-8") as f:
                    current = json.load(f)
            if current != document:
                stale.append(service)
                print("[stale] {:<10} 저장된 내용과 다릅니다".format(service))
            else:
                print("[ok]    {:<10} 최신".format(service))
            continue

        write(document, path)
        written += 1
        print(
            "[ok]    {:<10} 경로 {:>3} · 오퍼레이션 {:>3} · 스키마 {:>3}".format(
                service, path_count, operations, schemas
            )
        )

    for service, port, error in unreachable:
        print(
            "[skip]  {:<10} localhost:{} 에 연결하지 못했습니다 ({})".format(service, port, error),
            file=sys.stderr,
        )

    if args.check:
        if stale:
            print(
                "\n[error] 최신이 아닌 스펙 {}개: {}".format(len(stale), ", ".join(stale)),
                file=sys.stderr,
            )
            print("        python3 scripts/api/dump_openapi.py 로 갱신하십시오.", file=sys.stderr)
            return 1
        print("\n[info] 확인한 스펙이 모두 최신입니다.")
    else:
        print("\n[info] {}개 저장: {}".format(written, OUTPUT_DIR))

    # 일부만 못 받았으면 스펙이 불완전하다는 사실이 종료 코드에 드러나야 한다.
    if unreachable:
        print(
            "[error] {}개 서비스를 건너뛰었습니다. 스펙이 불완전합니다.".format(len(unreachable)),
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
