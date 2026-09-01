-- 회원 등급(p_tier) 초기 시드.
-- p_tier 가 비어 있으면 회원가입이 TIER_NOT_FOUND("등급을 찾을 수 없습니다.")로 실패한다.
-- UserService.createUser 가 기본 등급을 이름('아이언')으로 조회하므로 이 행은 필수다.
-- p_tier.name 에 UNIQUE 제약이 없어, 재실행 안전성은 WHERE NOT EXISTS 로 확보한다.
--
-- threshold_price(누적 결제금액 임계값)를 읽어 자동 승급하는 로직은 아직 없다.
-- 등급 변경은 관리자 API(PATCH /api/users/tier/{userId})로만 이뤄지므로,
-- 아래 임계값은 표시/향후 승급 로직용 기준값이며 UPDATE 로 조정해도 안전하다.
INSERT INTO p_tier (name, threshold_price, created_at, updated_at)
SELECT s.name, s.threshold_price, NOW(), NOW()
FROM (
              SELECT '아이언'   AS name,        0 AS threshold_price
    UNION ALL SELECT '브론즈',            100000
    UNION ALL SELECT '실버',              300000
    UNION ALL SELECT '골드',             1000000
    UNION ALL SELECT '플래티넘',          3000000
    UNION ALL SELECT '다이아몬드',       10000000
) AS s
WHERE NOT EXISTS (SELECT 1 FROM p_tier t WHERE t.name = s.name);
