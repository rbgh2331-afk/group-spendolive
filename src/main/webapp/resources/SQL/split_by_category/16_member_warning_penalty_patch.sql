-- =========================================================
-- SpendOlive 회원 경고 누적/채팅 제한 정책 패치
-- 1회 경고: 7일 채팅 제한
-- 2회 경고: 14일 채팅 제한
-- 3회 이상: 30일 채팅 제한
-- warning_count는 3에서 멈추지 않고 4, 5, 6... 실제 누적 횟수를 저장한다.
-- 기존 DB에 한 번 실행한다.
-- =========================================================

DECLARE
    v_constraint_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_constraint_count
      FROM user_constraints
     WHERE table_name = 'MEMBER_TB'
       AND constraint_name = 'CK_MEMBER_WARNING_COUNT';

    IF v_constraint_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE member_tb DROP CONSTRAINT ck_member_warning_count';
    END IF;

    EXECUTE IMMEDIATE 'ALTER TABLE member_tb ADD CONSTRAINT ck_member_warning_count CHECK (warning_count >= 0)';
END;
/

COMMIT;
