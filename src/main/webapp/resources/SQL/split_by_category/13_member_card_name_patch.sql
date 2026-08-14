-- [마이페이지 카드 이름 수정]
-- 기존 DB를 유지한 상태에서 카드 표시 이름 기능만 추가할 때 1회 실행한다.
ALTER TABLE MEMBER_CARD_TB ADD CARD_NAME VARCHAR2(30);

-- 기존 카드에는 카드사명을 기본 표시 이름으로 채운다.
UPDATE MEMBER_CARD_TB
SET CARD_NAME = NVL(CARD_NAME, CARD_COMPANY);

COMMIT;
