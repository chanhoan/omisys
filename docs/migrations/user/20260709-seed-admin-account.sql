-- 초기 관리자(ROLE_ADMIN) 계정 시드.
-- :BCRYPT_HASH 는 배포 시 별도로 생성한 BCrypt 해시로 치환한다. 평문 비밀번호/실제 해시를 커밋하지 말 것.
INSERT INTO p_user (username, password, nickname, email, point, role, is_deleted, created_at, updated_at)
SELECT 'admin', ':BCRYPT_HASH', 'admin', 'admin@omisys.local', 0, 'ROLE_ADMIN', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM p_user WHERE username = 'admin');

INSERT INTO p_user_tier (user_id, tier_id, cumulative_amount, created_at, updated_at)
SELECT u.user_id, t.tier_id, 0, NOW(), NOW()
FROM p_user u
JOIN p_tier t ON t.name = '아이언'
WHERE u.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM p_user_tier ut WHERE ut.user_id = u.user_id);
