SpendOlive SQL 실행 순서 / 정리 기준
=====================================

[기본 원칙]
- SQL 폴더 구조는 기존 프로젝트의 split_by_category 구조를 그대로 사용합니다.
- 새 schema/seed/patch/archive 하위 폴더를 만들지 않습니다.
- 각 파일의 기존 역할과 파일명은 유지하면서 중복 컬럼/인덱스/제약조건과 잘못된 참조만 정리합니다.

[새 DB 전체 생성 권장 순서]
0. 00_reset_all_objects.sql                (선택: 기존 SpendOlive 객체 전체 삭제)
1. 01_member_schema.sql                    (회원/계좌/카드/거래)
2. 02_expense_calendar_schema.sql          (지출/예산/캘린더)
3. 03_ott_schema.sql                       (OTT 방/멤버/채팅/정산)
4. 03-1_payment.sql                        (정산 결제/환불)
5. 04_notice_notification_inquiry_faq.sql  (공지/문의/FAQ/알림)
6. 05_admin_report_warning_schema.sql      (신고/경고)

[기본 데이터]
- 기능별로 넣을 때
  06_seed_member_sample.sql
  07_seed_expense_categories_and_samples.sql
  08_seed_ott_services.sql
  09_seed_alert_sample.sql

- 한 번에 넣을 때
  10_seed_all_default_data.sql

※ 06~09와 10은 같은 종류의 샘플 데이터가 겹칠 수 있으므로 둘 중 한 방식을 선택합니다.

[참고/기존 DB 보정]
- 11_reference_queries.sql                 : 조회 참고용
- 12_patch_notification_type_expand.sql    : 기존 DB 알림 타입 확장용
- 13_member_card_name_patch.sql            : 기존 DB 카드 이름 컬럼 보정용
- 16_member_warning_penalty_patch.sql       : 기존 DB 경고 횟수 상한 해제 및 채팅 제한 정책 적용용
- 17_settlement_refund_room_id_remove_patch.sql : 기존 DB 환불 테이블의 과거 room_id/FK/인덱스 제거용

[이번 정리에서 수정한 핵심]
1. 00_reset_all_objects.sql
   - DROP 이후 존재하지 않는 테이블에 ALTER/INSERT하던 구문 제거
   - 초기화 파일은 DROP 역할만 수행하도록 정리

2. 01_member_schema.sql
   - MEMBER_ACCOUNT_TB 동일 컬럼/제약조건 중복 ALTER 제거
   - MEMBER_TRAN_TB의 BALANCE_AFTER 중복 ADD 제거

3. 03_ott_schema.sql
   - 나가기 예약 컬럼을 CREATE TABLE 정의에 포함
   - 동일 컬럼을 뒤에서 다시 추가하던 중복 패치 구문 제거

4. 03-1_payment.sql
   - settlement_refund_tb에 room_id 컬럼을 두지 않음
   - room_id 인덱스를 만들지 않음
   - 동일 이름 인덱스 중복 CREATE 제거
   - 방 정보가 필요할 때는 settlement_id를 통해 settlement_tb.room_id를 조회함

4-1. 전체 생성/기존 통합 SQL 환불 스키마
   - 03-1_payment.sql과 동일하게 settlement_refund_tb의 room_id/FK/인덱스를 제거
   - 기존 DB에 과거 room_id가 남아 있으면 17번 패치로 제거

5. 05_admin_report_warning_schema.sql
   - uk_report_duplicate UNIQUE 제약조건 중복 생성 제거

6. 06 / 10 테스트 회원 데이터
   - LOCAL 테스트 계정의 화면 입력 비밀번호는 1234 유지
   - DB에는 BCrypt 해시를 저장하여 현재 로그인 로직과 일치하도록 수정

[주의]
- 00번은 기존 데이터가 모두 삭제되므로 새 DB 구성 또는 완전 초기화 때만 실행합니다.
- 기존 운영/개발 DB에 전체 스키마 파일을 다시 실행하지 말고 필요한 patch SQL만 적용합니다.
- SQL Developer에서는 전체 스크립트 실행 시 F5 사용을 권장합니다.
