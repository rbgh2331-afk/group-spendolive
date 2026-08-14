/* =========================================================
   10. 전체 기본 데이터 한 번에 입력
   =========================================================
   실행 안내: 01~05번 스키마를 모두 실행한 뒤 실행. 카테고리, OTT 서비스, 테스트 회원, 지출, 알림을 한 번에 넣음.
   ========================================================= */

SET DEFINE OFF;

/* =========================================================
   24. [팀 원본 사용 + 오류 수정] 기본 데이터
   =========================================================
   [팀 원본 오류 수정]
   - 카테고리 중복 INSERT 정리
   - member_tb 필수 컬럼 id, verify_type 누락 수정
   - ott_service_tb INSERT의 risk_level/block_reason 값 오류 수정
   - notification_tb 기준으로 테스트 OTT 알림 INSERT 정리
*/

/* 지출 카테고리 */
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('월세', 'FIXED', 1);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('통신비', 'FIXED', 2);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('보험료', 'FIXED', 3);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('관리비', 'FIXED', 4);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('공과금', 'FIXED', 5);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('식비', 'VARIABLE', 1);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('교통비', 'VARIABLE', 2);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('생활비', 'VARIABLE', 3);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('쇼핑', 'VARIABLE', 4);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('문화, 취미생활', 'VARIABLE', 5);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('기타', 'VARIABLE', 99);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Netflix', 'OTT', 1);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Disney+', 'OTT', 2);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('TVING', 'OTT', 3);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Wavve', 'OTT', 4);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Watcha', 'OTT', 5);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Laftel', 'OTT', 6);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Coupang Play', 'OTT', 7);

/* OTT 서비스
   컬럼 순서:
   service_name, default_price, fixed_plan_name, base_price,
   extra_member_fee, extra_member_count, max_member_limit,
   platform_fee_rate, share_yn, risk_level, block_reason
*/
INSERT INTO ott_service_tb(
    service_name, default_price, fixed_plan_name, base_price,
    extra_member_fee, extra_member_count, max_member_limit,
    platform_fee_rate, share_yn, risk_level, block_reason
) VALUES (
    'Netflix', 27000, '프리미엄', 17000,
    5000, 2, 4,
    3, 'Y', 'LOW', NULL
);

INSERT INTO ott_service_tb(
    service_name, default_price, fixed_plan_name, base_price,
    extra_member_fee, extra_member_count, max_member_limit,
    platform_fee_rate, share_yn, risk_level, block_reason
) VALUES (
    'TVING', 22000, '프리미엄', 17000,
    5000, 1, 4,
    3, 'Y', 'LOW', NULL
);

INSERT INTO ott_service_tb(
    service_name, default_price, fixed_plan_name, base_price,
    extra_member_fee, extra_member_count, max_member_limit,
    platform_fee_rate, share_yn, risk_level, block_reason
) VALUES (
    'Wavve', 13900, '프리미엄', 13900,
    0, 0, 4,
    3, 'Y', 'LOW', NULL
);

INSERT INTO ott_service_tb(
    service_name, default_price, fixed_plan_name, base_price,
    extra_member_fee, extra_member_count, max_member_limit,
    platform_fee_rate, share_yn, risk_level, block_reason
) VALUES (
    'Watcha', 12900, '프리미엄', 12900,
    0, 0, 4,
    3, 'Y', 'LOW', NULL
);

INSERT INTO ott_service_tb(
    service_name, default_price, fixed_plan_name, base_price,
    extra_member_fee, extra_member_count, max_member_limit,
    platform_fee_rate, share_yn, risk_level, block_reason
) VALUES (
    'Disney+', 17900, '프리미엄', 13900,
    4000, 1, 4,
    3, 'Y', 'LOW', NULL
);

INSERT INTO ott_service_tb(
    service_name, default_price, fixed_plan_name, base_price,
    extra_member_fee, extra_member_count, max_member_limit,
    platform_fee_rate, share_yn, risk_level, block_reason
) VALUES (
    'Laftel', 14900, '프리미엄', 14900,
    0, 0, 4,
    3, 'Y', 'LOW', NULL
);

INSERT INTO ott_service_tb(
    service_name, default_price, fixed_plan_name, base_price,
    extra_member_fee, extra_member_count, max_member_limit,
    platform_fee_rate, share_yn, risk_level, block_reason
) VALUES (
    '쿠팡플레이', 7890, '단일 멤버십', 7890,
    0, 0, 1,
    3, 'N', 'HIGH', '쿠팡 계정과 연결되어 결제/배송/주문정보 노출 위험'
);

INSERT INTO ott_service_tb(
    service_name, default_price, fixed_plan_name, base_price,
    extra_member_fee, extra_member_count, max_member_limit,
    platform_fee_rate, share_yn, risk_level, block_reason
) VALUES (
    '애플TV+', 6500, '단일 멤버십', 6500,
    0, 0, 1,
    3, 'N', 'HIGH', 'Apple ID 직접 공유 위험, 가족 공유 방식 권장'
);


/* 테스트 회원
   LOCAL 계정의 화면 입력 비밀번호는 1234이며, DB에는 BCrypt 해시로 저장한다. */
INSERT INTO member_tb(id, email, password, member_name, nickname, login_type, verify_type, role)
VALUES ('admin', 'admin@spendolive.com', '$2a$10$pOfMsCN7jqLG1AuHoistSOaVyKM8qIZS0.Ze2x6nKEj4RIKKxL0s2', '관리자', 'admin', 'LOCAL', 'PHONE', 'ADMIN');

INSERT INTO member_tb(id, email, password, member_name, nickname, login_type, verify_type, role)
VALUES ('host', 'host@spendolive.com', '$2a$10$pOfMsCN7jqLG1AuHoistSOaVyKM8qIZS0.Ze2x6nKEj4RIKKxL0s2', '파티장유저', 'partyhost', 'LOCAL', 'PHONE', 'HOST');

INSERT INTO member_tb(id, email, password, member_name, nickname, login_type, verify_type, role)
VALUES ('user', 'user@spendolive.com', '$2a$10$pOfMsCN7jqLG1AuHoistSOaVyKM8qIZS0.Ze2x6nKEj4RIKKxL0s2', '일반유저', 'olive', 'LOCAL', 'PHONE', 'USER');

INSERT INTO member_tb(id, email, password, member_name, nickname, login_type, verify_type, role)
VALUES ('naveruser', 'naveruser@spendolive.com', 'SOCIAL_LOGIN', '네이버유저', 'naverolive', 'NAVER', 'EMAIL', 'USER');

/* 테스트 지출: member_id는 seq_member 기준으로 user가 3번이라고 가정 */
INSERT INTO expense_tb(member_id, category_id, expense_title, amount, expense_date, payment_method, memo, fixed_yn)
VALUES (3, 4, '관리비', 120000, TRUNC(SYSDATE, 'MM') + 4, 'CARD', '이번 달 관리비', 'Y');

INSERT INTO expense_tb(member_id, category_id, expense_title, amount, expense_date, payment_method, memo, fixed_yn)
VALUES (3, 6, '점심', 9500, TRUNC(SYSDATE), 'CARD', '학원 근처 점심', 'N');

INSERT INTO expense_tb(member_id, category_id, expense_title, amount, expense_date, payment_method, memo, fixed_yn)
VALUES (3, 12, '넷플릭스', 4250, TRUNC(SYSDATE, 'MM') + 14, 'TRANSFER', 'OTT 정산금', 'Y');

/* 테스트 OTT 알림 */
INSERT INTO notification_tb(id, notification_type, title, message, link_url)
VALUES ('user', 'OTT', '넷플릭스 정산 요청', '이번 달 넷플릭스 정산금 입금이 필요합니다.', '/spendolive/ott/recruit.do?tab=settlement');

COMMIT;
