/* =========================================================
   [과거 통합본 - 실행 금지]
   현재 프로젝트의 컬럼 기준은 split_by_category 폴더의 번호별 SQL입니다.
   새 DB 구성은 README_EXECUTION_ORDER.txt 순서대로 실행하세요.
   ========================================================= */

/* =========================================================
   SpendOlive Oracle DB Schema - 오류 수정 + OTT 선결제/마감/추방/해산예약/환불 반영본
   기준 파일: spendolive_schema_final(2).sql
   작성 기준: Oracle XE 21c / Spring Boot + JSP + JdbcTemplate 프로젝트

   주석 구분
   - [팀 원본 사용]        : 팀 SQL에 있던 구조를 유지한 부분
   - [팀 원본 오류 수정]   : 팀 SQL에 있었지만 실행 오류가 나던 부분을 고친 부분
   - [방금 만든 OTT SQL 반영] : 이전에 만든 ott_tables_payment_close_refund_full_create.sql의 결제/마감/환불 시스템을 반영한 부분
   - [신규 추가]           : 이번 시스템 때문에 새로 추가한 테이블/컬럼/상태값

   주요 수정 요약
   1) 파일 맨 위 SELECT문 제거: 테이블 생성 전 SELECT 실행 오류 방지
   2) ott_service_tb 마지막 쉼표 제거
   3) ott_service_tb INSERT에서 사용하던 risk_level 컬럼 추가
   4) ott_room_member_tb FK 컬럼명 오류 수정: FOREIGN KEY(id) -> FOREIGN KEY(member_id)
   5) escrow_tb/payout_tb 인덱스 컬럼명 오류 수정: host_member_id -> host_id
   6) alert_tb 인덱스/INSERT/예시 SQL 컬럼명 오류 수정: member_id -> id
   7) 테스트 회원 INSERT 필수값 누락 수정: id, verify_type, password
   8) Wavve/Watcha OTT INSERT 따옴표 오류 수정
   9) 자주 쓰는 SQL은 실행되지 않도록 주석 처리
   10) OTT 선결제 기간, 결제 마감, 미결제 추방, 대체 모집, 방 해산 예약, 환불 테이블 반영
   ========================================================= */

SET DEFINE OFF;

/* =========================================================
   0. 기존 객체 삭제
   =========================================================
   - 다시 실행해도 오류가 나지 않도록 예외 처리.
   - 필요 없으면 이 블록 전체를 주석 처리해도 됨.
*/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE settlement_refund_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE platform_revenue_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE payout_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE escrow_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE settlement_payment_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE settlement_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE warning_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE report_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE notice_bookmark_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE inquiry_answer_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE inquiry_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE faq_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE notice_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE alert_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ott_chat_read_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ott_chat_message_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ott_room_block_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ott_room_member_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ott_room_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ott_service_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE expense_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE expense_category_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE member_tb CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_settlement_refund'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_platform_revenue'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_payout'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_escrow'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_settlement_payment'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_settlement'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_warning'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_report'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_notice_bookmark'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_inquiry_answer'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_inquiry'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_faq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_notice'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_alert'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_ott_chat_message'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_ott_room_block'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_ott_room_member'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_ott_room'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_ott_service'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_expense'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_expense_category'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_member'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

/* =========================================================
   1. [팀 원본 사용] 회원 테이블
   ========================================================= */
CREATE TABLE member_tb (
    member_id      NUMBER NOT NULL,
    id             VARCHAR2(20) NOT NULL,
    email          VARCHAR2(100) NOT NULL,
    password       VARCHAR2(255) NOT NULL,
    member_name    VARCHAR2(50) NOT NULL,
    nickname       VARCHAR2(50),
    phone          VARCHAR2(20),
    login_type     VARCHAR2(20) DEFAULT 'LOCAL' NOT NULL,
    verify_type    VARCHAR2(20) NOT NULL,
    role           VARCHAR2(20) DEFAULT 'USER' NOT NULL,
    status         VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    blocked_until  DATE,
    warning_count  NUMBER DEFAULT 0 NOT NULL,
    last_login_at  DATE,
    created_at     DATE DEFAULT SYSDATE NOT NULL,
    updated_at     DATE,

    CONSTRAINT pk_member PRIMARY KEY (member_id),
    CONSTRAINT uk_member_id UNIQUE (id),
    CONSTRAINT uk_member_email UNIQUE (email),
    CONSTRAINT ck_member_login_type CHECK (login_type IN ('LOCAL', 'KAKAO', 'GOOGLE', 'NAVER')),
    CONSTRAINT ck_member_verify_type CHECK (verify_type IN ('EMAIL', 'PHONE')),
    CONSTRAINT ck_member_role CHECK (role IN ('USER', 'HOST', 'ADMIN')),
    CONSTRAINT ck_member_status CHECK (status IN ('ACTIVE', 'LEAVE', 'BLOCK', 'PERM_BLOCK')),
    CONSTRAINT ck_member_warning_count CHECK (warning_count >= 0)
);

CREATE SEQUENCE seq_member START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_member_bi
BEFORE INSERT ON member_tb
FOR EACH ROW
WHEN (NEW.member_id IS NULL)
BEGIN
    SELECT seq_member.NEXTVAL INTO :NEW.member_id FROM dual;
END;
/

/* =========================================================
   2. [팀 원본 사용] 지출 카테고리 테이블
   ========================================================= */
CREATE TABLE expense_category_tb (
    category_id    NUMBER NOT NULL,
    category_name  VARCHAR2(50) NOT NULL,
    expense_type   VARCHAR2(20) NOT NULL,
    sort_order     NUMBER DEFAULT 0 NOT NULL,

    CONSTRAINT pk_expense_category PRIMARY KEY (category_id),
    CONSTRAINT ck_expense_category_type CHECK (expense_type IN ('FIXED', 'VARIABLE', 'OTT'))
);

CREATE SEQUENCE seq_expense_category START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_expense_category_bi
BEFORE INSERT ON expense_category_tb
FOR EACH ROW
WHEN (NEW.category_id IS NULL)
BEGIN
    SELECT seq_expense_category.NEXTVAL INTO :NEW.category_id FROM dual;
END;
/

/* =========================================================
   3. [팀 원본 사용] 지출 내역 테이블
   ========================================================= */
CREATE TABLE expense_tb (
    expense_id     NUMBER NOT NULL,
    member_id      NUMBER NOT NULL,
    category_id    NUMBER NOT NULL,
    expense_title  VARCHAR2(100) NOT NULL,
    amount         NUMBER NOT NULL,
    expense_date   DATE NOT NULL,
    payment_method VARCHAR2(30),
    memo           VARCHAR2(1000),
    repeat_yn      CHAR(1) DEFAULT 'N' NOT NULL,
    repeat_cycle   VARCHAR2(20),
    fixed_yn       CHAR(1) DEFAULT 'N' NOT NULL,
    created_at     DATE DEFAULT SYSDATE NOT NULL,
    updated_at     DATE,

    CONSTRAINT pk_expense PRIMARY KEY (expense_id),
    CONSTRAINT fk_expense_member FOREIGN KEY (member_id) REFERENCES member_tb(member_id),
    CONSTRAINT fk_expense_category FOREIGN KEY (category_id) REFERENCES expense_category_tb(category_id),
    CONSTRAINT ck_expense_amount CHECK (amount >= 0),
    CONSTRAINT ck_expense_repeat_yn CHECK (repeat_yn IN ('Y', 'N')),
    CONSTRAINT ck_expense_fixed_yn CHECK (fixed_yn IN ('Y', 'N')),
    CONSTRAINT ck_expense_repeat_cycle CHECK (repeat_cycle IS NULL OR repeat_cycle IN ('MONTHLY', 'WEEKLY', 'YEARLY'))
);

CREATE SEQUENCE seq_expense START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_expense_bi
BEFORE INSERT ON expense_tb
FOR EACH ROW
WHEN (NEW.expense_id IS NULL)
BEGIN
    SELECT seq_expense.NEXTVAL INTO :NEW.expense_id FROM dual;
END;
/

CREATE INDEX idx_expense_member_date ON expense_tb(member_id, expense_date);
CREATE INDEX idx_expense_category ON expense_tb(category_id);

/* =========================================================
   4. [팀 원본 사용 + 팀 원본 오류 수정] OTT 서비스 테이블
   =========================================================
   [팀 원본 오류 수정]
   - 마지막 CONSTRAINT 뒤에 있던 쉼표 제거
   - 기본 데이터 INSERT에서 사용하던 risk_level 컬럼 추가
*/
CREATE TABLE ott_service_tb (
    ott_service_id     NUMBER NOT NULL,
    service_name       VARCHAR2(50) NOT NULL,

    /*
       [피클플러스 방식 반영]
       default_price는 N분의 1을 할 최종 금액입니다.
       예) 넷플릭스 프리미엄 17,000 + 추가 계정 5,000 * 2 = 27,000
    */
    default_price      NUMBER DEFAULT 0 NOT NULL,
    fixed_plan_name    VARCHAR2(50) DEFAULT '프리미엄' NOT NULL,
    base_price         NUMBER DEFAULT 0 NOT NULL,
    extra_member_fee   NUMBER DEFAULT 0 NOT NULL,
    extra_member_count NUMBER DEFAULT 0 NOT NULL,
    max_member_limit   NUMBER DEFAULT 4 NOT NULL,
    platform_fee_rate  NUMBER(5,2) DEFAULT 3 NOT NULL,

    share_yn           CHAR(1) DEFAULT 'Y' NOT NULL,
    risk_level         VARCHAR2(20),
    block_reason       VARCHAR2(500),

    CONSTRAINT pk_ott_service PRIMARY KEY (ott_service_id),
    CONSTRAINT uk_ott_service_name UNIQUE (service_name),
    CONSTRAINT ck_ott_service_share CHECK (share_yn IN ('Y', 'N')),
    CONSTRAINT ck_ott_service_risk CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_ott_service_price CHECK (default_price >= 0 AND base_price >= 0),
    CONSTRAINT ck_ott_service_extra CHECK (extra_member_fee >= 0 AND extra_member_count >= 0),
    CONSTRAINT ck_ott_service_member_limit CHECK (max_member_limit BETWEEN 1 AND 6),
    CONSTRAINT ck_ott_service_fee_rate CHECK (platform_fee_rate >= 0)
);

CREATE SEQUENCE seq_ott_service START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_ott_service_bi
BEFORE INSERT ON ott_service_tb
FOR EACH ROW
WHEN (NEW.ott_service_id IS NULL)
BEGIN
    SELECT seq_ott_service.NEXTVAL INTO :NEW.ott_service_id FROM dual;
END;
/

/* =========================================================
   5. [팀 원본 사용 + 방금 만든 OTT SQL 반영] OTT 공유방 테이블
   =========================================================
   [팀 원본 사용]
   - room_id, host_member_id, ott_service_id, room_name, total_price,
     billing_day, member_limit, status, invite_code, created_at, updated_at 유지

   [방금 만든 OTT SQL 반영]
   - plan_name: 요금제명
   - CLOSE_REQUESTED: 파티장 방 삭제 요청 상태
   - close_requested_at / close_effective_date / close_reason / close_notice / closed_at
   - PAYMENT_OPEN, REPLACE_RECRUITING, CLOSED 상태 추가
*/
CREATE TABLE ott_room_tb (
    room_id              NUMBER NOT NULL,
    host_member_id       VARCHAR2(20) NOT NULL,
    ott_service_id       NUMBER NOT NULL,
    room_name            VARCHAR2(100) NOT NULL,
    plan_name            VARCHAR2(50) DEFAULT '기본' NOT NULL,
    total_price          NUMBER NOT NULL,
    billing_day          NUMBER NOT NULL,
    member_limit         NUMBER DEFAULT 4 NOT NULL,
    room_mode            VARCHAR2(20) DEFAULT 'RECRUIT' NOT NULL,
    status               VARCHAR2(30) DEFAULT 'RECRUITING' NOT NULL,
    invite_code          VARCHAR2(50),
    close_requested_at   DATE,
    close_effective_date DATE,
    close_reason         VARCHAR2(500),
    close_notice         VARCHAR2(1000),
    closed_at            DATE,
    created_at           DATE DEFAULT SYSDATE NOT NULL,
    updated_at           DATE,

    CONSTRAINT pk_ott_room PRIMARY KEY (room_id),
    CONSTRAINT fk_ott_room_host FOREIGN KEY (host_member_id) REFERENCES member_tb(id),
    CONSTRAINT fk_ott_room_service FOREIGN KEY (ott_service_id) REFERENCES ott_service_tb(ott_service_id),
    CONSTRAINT ck_ott_room_price CHECK (total_price >= 0),
    CONSTRAINT ck_ott_room_billing_day CHECK (billing_day BETWEEN 1 AND 31),
    CONSTRAINT ck_ott_room_member_limit CHECK (member_limit BETWEEN 1 AND 6),
    CONSTRAINT ck_ott_room_mode CHECK (room_mode IN ('FRIEND', 'RECRUIT')),
    CONSTRAINT ck_ott_room_status CHECK (
        status IN (
            'RECRUITING',
            'ACTIVE',
            'PAYMENT_OPEN',
            'REPLACE_RECRUITING',
            'CLOSE_REQUESTED',
            'CLOSED',
            'END'
        )
    ),
    CONSTRAINT uk_ott_room_invite UNIQUE (invite_code)
);

CREATE SEQUENCE seq_ott_room START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_ott_room_bi
BEFORE INSERT ON ott_room_tb
FOR EACH ROW
WHEN (NEW.room_id IS NULL)
BEGIN
    SELECT seq_ott_room.NEXTVAL INTO :NEW.room_id FROM dual;
END;
/

CREATE INDEX idx_ott_room_host ON ott_room_tb(host_member_id);
CREATE INDEX idx_ott_room_mode ON ott_room_tb(room_mode, status, created_at);
CREATE INDEX idx_ott_room_status ON ott_room_tb(status, created_at);
CREATE INDEX idx_ott_room_close ON ott_room_tb(status, close_effective_date);

/* =========================================================
   6. [팀 원본 사용 + 팀 원본 오류 수정 + 방금 만든 OTT SQL 반영] OTT 공유방 참여자 테이블
   =========================================================
   [팀 원본 오류 수정]
   - member_id 컬럼을 만들고 FK는 id로 걸려 있던 오류 수정
     기존 오류: FOREIGN KEY (id) REFERENCES member_tb(id)
     수정 후: FOREIGN KEY (member_id) REFERENCES member_tb(id)

   [방금 만든 OTT SQL 반영]
   - APPLIED, REJECTED, KICKED 상태 추가
   - kicked_at, kicked_reason, left_at 추가
*/
CREATE TABLE ott_room_member_tb (
    room_member_id NUMBER NOT NULL,
    room_id        NUMBER NOT NULL,
    member_id      VARCHAR2(20) NOT NULL,
    member_role    VARCHAR2(20) DEFAULT 'MEMBER' NOT NULL,
    share_amount   NUMBER DEFAULT 0 NOT NULL,
    fee_rate       NUMBER(5,2) DEFAULT 0 NOT NULL,
    fee_amount     NUMBER DEFAULT 0 NOT NULL,
    pay_amount     NUMBER DEFAULT 0 NOT NULL,
    joined_at      DATE DEFAULT SYSDATE NOT NULL,
    status         VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    kicked_at      DATE,
    kicked_reason  VARCHAR2(500),
    left_at        DATE,

    CONSTRAINT pk_ott_room_member PRIMARY KEY (room_member_id),
    CONSTRAINT fk_room_member_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_room_member_member FOREIGN KEY (member_id) REFERENCES member_tb(id),
    CONSTRAINT uk_room_member UNIQUE (room_id, member_id),
    CONSTRAINT ck_room_member_role CHECK (member_role IN ('HOST', 'MEMBER')),
    CONSTRAINT ck_room_member_status CHECK (status IN ('APPLIED', 'ACTIVE', 'REJECTED', 'OUT', 'KICKED')),
    CONSTRAINT ck_room_member_amount CHECK (share_amount >= 0),
    CONSTRAINT ck_room_member_fee_rate CHECK (fee_rate >= 0),
    CONSTRAINT ck_room_member_fee_amount CHECK (fee_amount >= 0),
    CONSTRAINT ck_room_member_pay_amount CHECK (pay_amount >= 0)
);

CREATE SEQUENCE seq_ott_room_member START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_ott_room_member_bi
BEFORE INSERT ON ott_room_member_tb
FOR EACH ROW
WHEN (NEW.room_member_id IS NULL)
BEGIN
    SELECT seq_ott_room_member.NEXTVAL INTO :NEW.room_member_id FROM dual;
END;
/

CREATE INDEX idx_room_member_room ON ott_room_member_tb(room_id, status);
CREATE INDEX idx_room_member_member ON ott_room_member_tb(member_id, status);

/* =========================================================
   7. [방금 만든 OTT SQL 반영 - 신규 추가] OTT 공유방 채팅 메시지 테이블
   =========================================================
   팀 최종 SQL에는 없었지만, 이전에 정리한 OTT 공유방 구조에 있던 채팅 기능 테이블.
   필요 없으면 이 테이블과 아래 ott_chat_read_tb를 빼도 됨.
*/
CREATE TABLE ott_chat_message_tb (
    message_id      NUMBER NOT NULL,
    room_id         NUMBER NOT NULL,
    sender_id       VARCHAR2(20) NOT NULL,
    message_content VARCHAR2(1000) NOT NULL,
    created_at      DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_ott_chat_message PRIMARY KEY (message_id),
    CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES member_tb(id)
);

CREATE SEQUENCE seq_ott_chat_message START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_ott_chat_message_bi
BEFORE INSERT ON ott_chat_message_tb
FOR EACH ROW
WHEN (NEW.message_id IS NULL)
BEGIN
    SELECT seq_ott_chat_message.NEXTVAL INTO :NEW.message_id FROM dual;
END;
/

CREATE INDEX idx_chat_message_room ON ott_chat_message_tb(room_id, created_at, message_id);
CREATE INDEX idx_chat_message_sender ON ott_chat_message_tb(sender_id, created_at);

/* =========================================================
   8. [방금 만든 OTT SQL 반영 - 신규 추가] OTT 공유방 채팅 읽음 기준 테이블
   ========================================================= */
CREATE TABLE ott_chat_read_tb (
    room_id      NUMBER NOT NULL,
    member_id    VARCHAR2(20) NOT NULL,
    last_read_at DATE,

    CONSTRAINT pk_ott_chat_read PRIMARY KEY (room_id, member_id),
    CONSTRAINT fk_chat_read_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_chat_read_member FOREIGN KEY (member_id) REFERENCES member_tb(id)
);

/* =========================================================
   9. [팀 원본 사용] OTT 공유방 차단 테이블
   ========================================================= */
CREATE TABLE ott_room_block_tb (
    room_block_id NUMBER NOT NULL,
    room_id       NUMBER NOT NULL,
    id            VARCHAR2(20) NOT NULL,
    blocked_by    VARCHAR2(20) NOT NULL,
    block_reason  VARCHAR2(500),
    blocked_at    DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_ott_room_block PRIMARY KEY (room_block_id),
    CONSTRAINT fk_room_block_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_room_block_member FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT fk_room_block_by FOREIGN KEY (blocked_by) REFERENCES member_tb(id),
    CONSTRAINT uk_room_block_member UNIQUE (room_id, id)
);

CREATE SEQUENCE seq_ott_room_block START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_ott_room_block_bi
BEFORE INSERT ON ott_room_block_tb
FOR EACH ROW
WHEN (NEW.room_block_id IS NULL)
BEGIN
    SELECT seq_ott_room_block.NEXTVAL INTO :NEW.room_block_id FROM dual;
END;
/

CREATE INDEX idx_room_block_room ON ott_room_block_tb(room_id);
CREATE INDEX idx_room_block_member ON ott_room_block_tb(id);

/* =========================================================
   10. [팀 원본 사용 + 방금 만든 OTT SQL 반영] 월별 정산 테이블
   =========================================================
   [팀 원본 사용]
   - settlement_id, room_id, settlement_month, total_price, total_fee,
     total_pay_amount, due_date, status, created_at 유지

   [방금 만든 OTT SQL 반영]
   - payment_start_date: 결제 가능 시작일. 예) 6월 결제일
   - payment_close_date: 결제 마감일. 예) 7월 결제일 5일 전
   - service_start_date: 해당 이용분 시작일. 예) 7월 결제일
   - service_end_date: 해당 이용분 종료일
   - replace_start_date / replace_end_date: 미결제자 추방 후 대체 모집 기간
   - status에 PAYMENT_OPEN, REPLACE_RECRUITING, CONFIRMED, CANCELLED, CLOSED 추가
*/
CREATE TABLE settlement_tb (
    settlement_id      NUMBER NOT NULL,
    room_id            NUMBER NOT NULL,
    settlement_month   CHAR(7) NOT NULL,
    total_price        NUMBER NOT NULL,
    total_fee          NUMBER DEFAULT 0 NOT NULL,
    total_pay_amount   NUMBER DEFAULT 0 NOT NULL,
    due_date           DATE NOT NULL,
    payment_start_date DATE,
    payment_close_date DATE,
    service_start_date DATE,
    service_end_date   DATE,
    replace_start_date DATE,
    replace_end_date   DATE,
    closed_at          DATE,
    status             VARCHAR2(30) DEFAULT 'READY' NOT NULL,
    created_at         DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_settlement PRIMARY KEY (settlement_id),
    CONSTRAINT fk_settlement_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT uk_settlement_room_month UNIQUE (room_id, settlement_month),
    CONSTRAINT ck_settlement_month CHECK (REGEXP_LIKE(settlement_month, '^[0-9]{4}-[0-9]{2}$')),
    CONSTRAINT ck_settlement_status CHECK (
        status IN (
            'READY',
            'REQUESTED',
            'DONE',
            'PAYMENT_OPEN',
            'REPLACE_RECRUITING',
            'CONFIRMED',
            'CANCELLED',
            'CLOSED'
        )
    ),
    CONSTRAINT ck_settlement_total_price CHECK (total_price >= 0),
    CONSTRAINT ck_settlement_total_fee CHECK (total_fee >= 0),
    CONSTRAINT ck_settlement_total_pay CHECK (total_pay_amount >= 0),
    CONSTRAINT ck_settlement_pay_period CHECK (
        payment_start_date IS NULL
        OR payment_close_date IS NULL
        OR payment_start_date <= payment_close_date
    ),
    CONSTRAINT ck_settlement_service_period CHECK (
        service_start_date IS NULL
        OR service_end_date IS NULL
        OR service_start_date <= service_end_date
    )
);

CREATE SEQUENCE seq_settlement START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_settlement_bi
BEFORE INSERT ON settlement_tb
FOR EACH ROW
WHEN (NEW.settlement_id IS NULL)
BEGIN
    SELECT seq_settlement.NEXTVAL INTO :NEW.settlement_id FROM dual;
END;
/

CREATE INDEX idx_settlement_room ON settlement_tb(room_id, settlement_month);
CREATE INDEX idx_settlement_status_close ON settlement_tb(status, payment_close_date);
CREATE INDEX idx_settlement_service ON settlement_tb(status, service_start_date, service_end_date);

/* =========================================================
   11. [팀 원본 사용 + 방금 만든 OTT SQL 반영] 팀원별 입금 상태 테이블
   =========================================================
   [방금 만든 OTT SQL 반영]
   - EXPIRED: 결제 마감일까지 결제하지 않아 만료
   - REFUND_REQUESTED / REFUNDED: 방 삭제 요청 등으로 환불 처리
   - confirmed_at, expired_at, cancelled_at 추가
*/
CREATE TABLE settlement_payment_tb (
    payment_id      NUMBER NOT NULL,
    settlement_id   NUMBER NOT NULL,
    id              VARCHAR2(20) NOT NULL,
    base_amount     NUMBER NOT NULL,
    fee_rate        NUMBER(5,2) DEFAULT 3 NOT NULL,
    fee_amount      NUMBER DEFAULT 0 NOT NULL,
    total_amount    NUMBER NOT NULL,
    payment_status  VARCHAR2(30) DEFAULT 'UNPAID' NOT NULL,
    paid_at         DATE,
    confirmed_at    DATE,
    expired_at      DATE,
    cancelled_at    DATE,
    memo            VARCHAR2(500),

    CONSTRAINT pk_settlement_payment PRIMARY KEY (payment_id),
    CONSTRAINT fk_payment_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_tb(settlement_id),
    CONSTRAINT fk_payment_member FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT uk_payment_member UNIQUE (settlement_id, id),
    CONSTRAINT ck_payment_base_amount CHECK (base_amount >= 0),
    CONSTRAINT ck_payment_fee_rate CHECK (fee_rate >= 0),
    CONSTRAINT ck_payment_fee_amount CHECK (fee_amount >= 0),
    CONSTRAINT ck_payment_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_payment_status CHECK (
        payment_status IN (
            'UNPAID',
            'PAID',
            'CONFIRMED',
            'EXPIRED',
            'CANCELLED',
            'REFUND_REQUESTED',
            'REFUNDED'
        )
    )
);

CREATE SEQUENCE seq_settlement_payment START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_settlement_payment_bi
BEFORE INSERT ON settlement_payment_tb
FOR EACH ROW
WHEN (NEW.payment_id IS NULL)
BEGIN
    SELECT seq_settlement_payment.NEXTVAL INTO :NEW.payment_id FROM dual;
END;
/

CREATE INDEX idx_payment_settlement ON settlement_payment_tb(settlement_id);
CREATE INDEX idx_payment_member ON settlement_payment_tb(id, payment_status);
CREATE INDEX idx_payment_status ON settlement_payment_tb(payment_status, paid_at);

/* =========================================================
   12. [방금 만든 OTT SQL 반영 - 신규 추가] 정산 환불 테이블
   =========================================================
   파티장이 방 삭제 요청을 했을 때 다음 달 이용분을 이미 결제한 참여자의 환불 기록을 저장.
*/
CREATE TABLE settlement_refund_tb (
    refund_id       NUMBER NOT NULL,
    payment_id      NUMBER NOT NULL,
    settlement_id   NUMBER NOT NULL,
    id              VARCHAR2(20) NOT NULL,
    refund_amount   NUMBER NOT NULL,
    refund_reason   VARCHAR2(30) DEFAULT 'ROOM_CLOSE' NOT NULL,
    refund_status   VARCHAR2(30) DEFAULT 'REQUESTED' NOT NULL,
    requested_at    DATE DEFAULT SYSDATE NOT NULL,
    completed_at    DATE,
    memo            VARCHAR2(500),

    CONSTRAINT pk_settlement_refund PRIMARY KEY (refund_id),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES settlement_payment_tb(payment_id),
    CONSTRAINT fk_refund_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_tb(settlement_id),
    CONSTRAINT fk_refund_member FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT uk_refund_payment UNIQUE (payment_id),
    CONSTRAINT ck_refund_amount CHECK (refund_amount >= 0),
    CONSTRAINT ck_refund_reason CHECK (refund_reason IN ('ROOM_CLOSE', 'PAYMENT_CANCEL', 'ADMIN_CANCEL', 'ETC')),
    CONSTRAINT ck_refund_status CHECK (refund_status IN ('REQUESTED', 'COMPLETED', 'FAILED'))
);

CREATE SEQUENCE seq_settlement_refund START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_settlement_refund_bi
BEFORE INSERT ON settlement_refund_tb
FOR EACH ROW
WHEN (NEW.refund_id IS NULL)
BEGIN
    SELECT seq_settlement_refund.NEXTVAL INTO :NEW.refund_id FROM dual;
END;
/

CREATE INDEX idx_refund_payment ON settlement_refund_tb(payment_id);
CREATE INDEX idx_refund_member ON settlement_refund_tb(id, refund_status);
/* =========================================================
   13. [팀 원본 사용] 홈페이지 보관금 테이블
   ========================================================= */
CREATE TABLE escrow_tb (
    escrow_id        NUMBER NOT NULL,
    settlement_id    NUMBER NOT NULL,
    room_id          NUMBER NOT NULL,
    payer_id         VARCHAR2(20) NOT NULL,
    host_id          VARCHAR2(20) NOT NULL,
    amount           NUMBER NOT NULL,
    escrow_status    VARCHAR2(20) DEFAULT 'HELD' NOT NULL,
    paid_at          DATE DEFAULT SYSDATE NOT NULL,
    release_due_date DATE,
    released_at      DATE,
    refunded_at      DATE,

    CONSTRAINT pk_escrow PRIMARY KEY (escrow_id),
    CONSTRAINT fk_escrow_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_tb(settlement_id),
    CONSTRAINT fk_escrow_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_escrow_payer FOREIGN KEY (payer_id) REFERENCES member_tb(id),
    CONSTRAINT fk_escrow_host FOREIGN KEY (host_id) REFERENCES member_tb(id),
    CONSTRAINT ck_escrow_amount CHECK (amount >= 0),
    CONSTRAINT ck_escrow_status CHECK (escrow_status IN ('HELD', 'RELEASED', 'REFUNDED', 'CANCELLED'))
);

CREATE SEQUENCE seq_escrow START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_escrow_bi
BEFORE INSERT ON escrow_tb
FOR EACH ROW
WHEN (NEW.escrow_id IS NULL)
BEGIN
    SELECT seq_escrow.NEXTVAL INTO :NEW.escrow_id FROM dual;
END;
/

CREATE INDEX idx_escrow_settlement ON escrow_tb(settlement_id);
/* [팀 원본 오류 수정] host_member_id 컬럼은 없으므로 host_id로 수정 */
CREATE INDEX idx_escrow_host_status ON escrow_tb(host_id, escrow_status);

/* =========================================================
   14. [팀 원본 사용] 플랫폼 수익 테이블
   ========================================================= */
CREATE TABLE platform_revenue_tb (
    revenue_id      NUMBER NOT NULL,
    settlement_id   NUMBER NOT NULL,
    room_id         NUMBER NOT NULL,
    payer_id        VARCHAR2(20) NOT NULL,
    base_amount     NUMBER NOT NULL,
    fee_rate        NUMBER(5,2) DEFAULT 3 NOT NULL,
    fee_amount      NUMBER NOT NULL,
    revenue_status  VARCHAR2(20) DEFAULT 'EARNED' NOT NULL,
    created_at      DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_platform_revenue PRIMARY KEY (revenue_id),
    CONSTRAINT fk_revenue_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_tb(settlement_id),
    CONSTRAINT fk_revenue_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_revenue_payer FOREIGN KEY (payer_id) REFERENCES member_tb(id),
    CONSTRAINT ck_revenue_base_amount CHECK (base_amount >= 0),
    CONSTRAINT ck_revenue_fee_rate CHECK (fee_rate >= 0),
    CONSTRAINT ck_revenue_fee_amount CHECK (fee_amount >= 0),
    CONSTRAINT ck_revenue_status CHECK (revenue_status IN ('EARNED', 'REFUNDED', 'CANCELLED'))
);

CREATE SEQUENCE seq_platform_revenue START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_platform_revenue_bi
BEFORE INSERT ON platform_revenue_tb
FOR EACH ROW
WHEN (NEW.revenue_id IS NULL)
BEGIN
    SELECT seq_platform_revenue.NEXTVAL INTO :NEW.revenue_id FROM dual;
END;
/

CREATE INDEX idx_revenue_settlement ON platform_revenue_tb(settlement_id);
CREATE INDEX idx_revenue_created ON platform_revenue_tb(created_at);

/* =========================================================
   15. [팀 원본 사용] 방장 지급 내역 테이블
   ========================================================= */
CREATE TABLE payout_tb (
    payout_id       NUMBER NOT NULL,
    settlement_id   NUMBER NOT NULL,
    room_id         NUMBER NOT NULL,
    host_id         VARCHAR2(20) NOT NULL,
    total_amount    NUMBER NOT NULL,
    payout_status   VARCHAR2(20) DEFAULT 'READY' NOT NULL,
    requested_at    DATE DEFAULT SYSDATE NOT NULL,
    paid_at         DATE,
    memo            VARCHAR2(500),

    CONSTRAINT pk_payout PRIMARY KEY (payout_id),
    CONSTRAINT fk_payout_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_tb(settlement_id),
    CONSTRAINT fk_payout_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_payout_host FOREIGN KEY (host_id) REFERENCES member_tb(id),
    CONSTRAINT ck_payout_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_payout_status CHECK (payout_status IN ('READY', 'PAID', 'FAILED', 'CANCELLED'))
);

CREATE SEQUENCE seq_payout START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_payout_bi
BEFORE INSERT ON payout_tb
FOR EACH ROW
WHEN (NEW.payout_id IS NULL)
BEGIN
    SELECT seq_payout.NEXTVAL INTO :NEW.payout_id FROM dual;
END;
/

/* [팀 원본 오류 수정] host_member_id 컬럼은 없으므로 host_id로 수정 */
CREATE INDEX idx_payout_host_status ON payout_tb(host_id, payout_status);

/* =========================================================
   16. [팀 원본 사용] 공지사항 테이블
   ========================================================= */
CREATE TABLE notice_tb (
    notice_id   NUMBER NOT NULL,
    admin_id    VARCHAR2(20) NOT NULL,
    title       VARCHAR2(200) NOT NULL,
    content     CLOB NOT NULL,
    pinned_yn   CHAR(1) DEFAULT 'N' NOT NULL,
    created_at  DATE DEFAULT SYSDATE NOT NULL,
    updated_at  DATE,

    CONSTRAINT pk_notice PRIMARY KEY (notice_id),
    CONSTRAINT fk_notice_admin FOREIGN KEY (admin_id) REFERENCES member_tb(id),
    CONSTRAINT ck_notice_pinned CHECK (pinned_yn IN ('Y', 'N'))
);

CREATE SEQUENCE seq_notice START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_notice_bi
BEFORE INSERT ON notice_tb
FOR EACH ROW
WHEN (NEW.notice_id IS NULL)
BEGIN
    SELECT seq_notice.NEXTVAL INTO :NEW.notice_id FROM dual;
END;
/

/* =========================================================
   17. [팀 원본 사용] FAQ 테이블
   ========================================================= */
CREATE TABLE faq_tb (
    faq_id      NUMBER NOT NULL,
    category    VARCHAR2(50) NOT NULL,
    question    VARCHAR2(300) NOT NULL,
    answer      CLOB NOT NULL,
    sort_order  NUMBER DEFAULT 0 NOT NULL,
    use_yn      CHAR(1) DEFAULT 'Y' NOT NULL,
    created_at  DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_faq PRIMARY KEY (faq_id),
    CONSTRAINT ck_faq_use CHECK (use_yn IN ('Y', 'N'))
);

CREATE SEQUENCE seq_faq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_faq_bi
BEFORE INSERT ON faq_tb
FOR EACH ROW
WHEN (NEW.faq_id IS NULL)
BEGIN
    SELECT seq_faq.NEXTVAL INTO :NEW.faq_id FROM dual;
END;
/

/* =========================================================
   18. [팀 원본 사용] 문의사항 테이블
   ========================================================= */
CREATE TABLE inquiry_tb (
    inquiry_id NUMBER NOT NULL,
    id         VARCHAR2(20) NOT NULL,
    title      VARCHAR2(200) NOT NULL,
    content    CLOB NOT NULL,
    status     VARCHAR2(20) DEFAULT 'WAITING' NOT NULL,
    created_at DATE DEFAULT SYSDATE NOT NULL,
    updated_at DATE,

    CONSTRAINT pk_inquiry PRIMARY KEY (inquiry_id),
    CONSTRAINT fk_inquiry_member FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT ck_inquiry_status CHECK (status IN ('WAITING', 'ANSWERED'))
);

CREATE SEQUENCE seq_inquiry START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_inquiry_bi
BEFORE INSERT ON inquiry_tb
FOR EACH ROW
WHEN (NEW.inquiry_id IS NULL)
BEGIN
    SELECT seq_inquiry.NEXTVAL INTO :NEW.inquiry_id FROM dual;
END;
/

/* =========================================================
   19. [팀 원본 사용] 문의 답변 테이블
   ========================================================= */
CREATE TABLE inquiry_answer_tb (
    answer_id  NUMBER NOT NULL,
    inquiry_id NUMBER NOT NULL,
    admin_id   VARCHAR2(20) NOT NULL,
    content    CLOB NOT NULL,
    created_at DATE DEFAULT SYSDATE NOT NULL,
    updated_at DATE,

    CONSTRAINT pk_inquiry_answer PRIMARY KEY (answer_id),
    CONSTRAINT fk_answer_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiry_tb(inquiry_id),
    CONSTRAINT fk_answer_admin FOREIGN KEY (admin_id) REFERENCES member_tb(id),
    CONSTRAINT uk_answer_inquiry UNIQUE (inquiry_id)
);

CREATE SEQUENCE seq_inquiry_answer START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_inquiry_answer_bi
BEFORE INSERT ON inquiry_answer_tb
FOR EACH ROW
WHEN (NEW.answer_id IS NULL)
BEGIN
    SELECT seq_inquiry_answer.NEXTVAL INTO :NEW.answer_id FROM dual;
END;
/

/* =========================================================
   20. [팀 원본 사용 + 팀 원본 오류 수정] 알림 테이블
   =========================================================
   [팀 원본 오류 수정]
   - 테이블 컬럼명은 id인데 인덱스/INSERT/예시 SQL에서 member_id를 사용하던 부분 수정
*/
CREATE TABLE alert_tb (
    alert_id   NUMBER NOT NULL,
    id         VARCHAR2(20) NOT NULL,
    alert_type VARCHAR2(30) NOT NULL,
    title      VARCHAR2(200) NOT NULL,
    content    VARCHAR2(1000),
    target_url VARCHAR2(300),
    read_yn    CHAR(1) DEFAULT 'N' NOT NULL,
    banner_yn  CHAR(1) DEFAULT 'Y' NOT NULL,
    created_at DATE DEFAULT SYSDATE NOT NULL,
    read_at    DATE,

    CONSTRAINT pk_alert PRIMARY KEY (alert_id),
    CONSTRAINT fk_alert_member FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT ck_alert_read CHECK (read_yn IN ('Y', 'N')),
    CONSTRAINT ck_alert_banner CHECK (banner_yn IN ('Y', 'N'))
);

CREATE SEQUENCE seq_alert START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_alert_bi
BEFORE INSERT ON alert_tb
FOR EACH ROW
WHEN (NEW.alert_id IS NULL)
BEGIN
    SELECT seq_alert.NEXTVAL INTO :NEW.alert_id FROM dual;
END;
/

CREATE INDEX idx_alert_member_read ON alert_tb(id, read_yn, created_at);
CREATE INDEX idx_alert_member_banner ON alert_tb(id, banner_yn, read_yn, created_at);

/* =========================================================
   21. [팀 원본 사용] 신고 테이블
   ========================================================= */
CREATE TABLE report_tb (
    report_id          NUMBER NOT NULL,
    reporter_id        VARCHAR2(20) NOT NULL,
    reported_member_id VARCHAR2(20) NOT NULL,
    room_id            NUMBER,
    report_reason      VARCHAR2(500) NOT NULL,
    report_status      VARCHAR2(20) DEFAULT 'WAIT' NOT NULL,
    admin_comment      VARCHAR2(1000),
    created_at         DATE DEFAULT SYSDATE NOT NULL,
    processed_at       DATE,

    CONSTRAINT pk_report PRIMARY KEY (report_id),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES member_tb(id),
    CONSTRAINT fk_report_reported FOREIGN KEY (reported_member_id) REFERENCES member_tb(id),
    CONSTRAINT fk_report_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT ck_report_status CHECK (report_status IN ('WAIT', 'PROCESSING', 'COMPLETE', 'REJECT'))
);

CREATE SEQUENCE seq_report START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_report_bi
BEFORE INSERT ON report_tb
FOR EACH ROW
WHEN (NEW.report_id IS NULL)
BEGIN
    SELECT seq_report.NEXTVAL INTO :NEW.report_id FROM dual;
END;
/

CREATE INDEX idx_report_reported ON report_tb(reported_member_id, report_status);

/* =========================================================
   22. [팀 원본 사용] 경고 테이블
   ========================================================= */
CREATE TABLE warning_tb (
    warning_id     NUMBER NOT NULL,
    member_id      VARCHAR2(20) NOT NULL,
    report_id      NUMBER,
    warning_reason VARCHAR2(500) NOT NULL,
    warning_level  NUMBER NOT NULL,
    penalty_days   NUMBER,
    permanent_yn   CHAR(1) DEFAULT 'N' NOT NULL,
    created_at     DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_warning PRIMARY KEY (warning_id),
    CONSTRAINT fk_warning_member FOREIGN KEY (member_id) REFERENCES member_tb(id),
    CONSTRAINT fk_warning_report FOREIGN KEY (report_id) REFERENCES report_tb(report_id),
    CONSTRAINT ck_warning_level CHECK (warning_level IN (1, 2, 3)),
    CONSTRAINT ck_warning_permanent CHECK (permanent_yn IN ('Y', 'N'))
);

CREATE SEQUENCE seq_warning START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_warning_bi
BEFORE INSERT ON warning_tb
FOR EACH ROW
WHEN (NEW.warning_id IS NULL)
BEGIN
    SELECT seq_warning.NEXTVAL INTO :NEW.warning_id FROM dual;
END;
/

CREATE INDEX idx_warning_member ON warning_tb(member_id);

/* =========================================================
   23. [팀 원본 사용] 회원별 공지 즐겨찾기 테이블
   ========================================================= */
CREATE TABLE notice_bookmark_tb (
    bookmark_id NUMBER NOT NULL,
    id          VARCHAR2(20) NOT NULL,
    notice_id   NUMBER NOT NULL,
    created_at  DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_notice_bookmark PRIMARY KEY (bookmark_id),
    CONSTRAINT fk_notice_bookmark_member FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT fk_notice_bookmark_notice FOREIGN KEY (notice_id) REFERENCES notice_tb(notice_id),
    CONSTRAINT uk_notice_bookmark UNIQUE (id, notice_id)
);

CREATE SEQUENCE seq_notice_bookmark START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_notice_bookmark_bi
BEFORE INSERT ON notice_bookmark_tb
FOR EACH ROW
WHEN (NEW.bookmark_id IS NULL)
BEGIN
    SELECT seq_notice_bookmark.NEXTVAL INTO :NEW.bookmark_id FROM dual;
END;
/

/* =========================================================
   24. [팀 원본 사용 + 오류 수정] 기본 데이터
   =========================================================
   [팀 원본 오류 수정]
   - 카테고리 중복 INSERT 정리
   - member_tb 필수 컬럼 id, verify_type 누락 수정
   - ott_service_tb INSERT의 risk_level/block_reason 값 오류 수정
   - alert_tb INSERT 컬럼명 member_id -> id 수정
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


/* 테스트 회원 */
INSERT INTO member_tb(id, email, password, member_name, nickname, login_type, verify_type, role)
VALUES ('admin', 'admin@spendolive.com', '1234', '관리자', 'admin', 'LOCAL', 'PHONE', 'ADMIN');

INSERT INTO member_tb(id, email, password, member_name, nickname, login_type, verify_type, role)
VALUES ('host', 'host@spendolive.com', '1234', '파티장유저', 'partyhost', 'LOCAL', 'PHONE', 'HOST');

INSERT INTO member_tb(id, email, password, member_name, nickname, login_type, verify_type, role)
VALUES ('user', 'user@spendolive.com', '1234', '일반유저', 'olive', 'LOCAL', 'PHONE', 'USER');

INSERT INTO member_tb(id, email, password, member_name, nickname, login_type, verify_type, role)
VALUES ('naveruser', 'naveruser@spendolive.com', 'SOCIAL_LOGIN', '네이버유저', 'naverolive', 'NAVER', 'EMAIL', 'USER');

/* 테스트 지출: member_id는 seq_member 기준으로 user가 3번이라고 가정 */
INSERT INTO expense_tb(member_id, category_id, expense_title, amount, expense_date, payment_method, memo, fixed_yn)
VALUES (3, 4, '관리비', 120000, TRUNC(SYSDATE, 'MM') + 4, 'CARD', '이번 달 관리비', 'Y');

INSERT INTO expense_tb(member_id, category_id, expense_title, amount, expense_date, payment_method, memo, fixed_yn)
VALUES (3, 6, '점심', 9500, TRUNC(SYSDATE), 'CARD', '학원 근처 점심', 'N');

INSERT INTO expense_tb(member_id, category_id, expense_title, amount, expense_date, payment_method, memo, fixed_yn)
VALUES (3, 12, '넷플릭스', 4250, TRUNC(SYSDATE, 'MM') + 14, 'TRANSFER', 'OTT 정산금', 'Y');

/* 테스트 알림 */
INSERT INTO alert_tb(id, alert_type, title, content, target_url)
VALUES ('user', 'SETTLEMENT', '넷플릭스 정산 요청', '이번 달 넷플릭스 정산금 입금이 필요합니다.', '/settlement/1');

COMMIT;

/* =========================================================
   25. 자주 쓰는 SQL 예시
   =========================================================
   아래 SQL은 참고용입니다.
   전체 스키마 실행 시 바인드 변수(:member_id 등) 때문에 멈출 수 있으므로 주석 처리했습니다.

-- 알림 이모지 빨간 점 표시용: 안 읽은 알림 개수
SELECT COUNT(*) AS unread_count
FROM alert_tb
WHERE id = :member_id
  AND read_yn = 'N';

-- 배너에 띄울 안 읽은 알림 목록
SELECT alert_id, title, content, target_url, created_at
FROM alert_tb
WHERE id = :member_id
  AND read_yn = 'N'
  AND banner_yn = 'Y'
ORDER BY created_at DESC;

-- 알림 클릭 시 읽음 처리
UPDATE alert_tb
SET read_yn = 'Y',
    read_at = SYSDATE,
    banner_yn = 'N'
WHERE alert_id = :alertId
  AND id = :member_id;

-- 3% 수수료 계산 예시
SELECT
    4250 AS base_amount,
    ROUND(4250 * 0.03) AS fee_amount,
    4250 + ROUND(4250 * 0.03) AS total_amount
FROM dual;

-- 월별 플랫폼 수익 합계
SELECT
    TO_CHAR(created_at, 'YYYY-MM') AS revenue_month,
    SUM(fee_amount) AS total_revenue
FROM platform_revenue_tb
GROUP BY TO_CHAR(created_at, 'YYYY-MM')
ORDER BY revenue_month;

-- 날짜별 지출 합계: 캘린더 표시용
SELECT
    TO_CHAR(e.expense_date, 'YYYY-MM-DD') AS expense_date,
    SUM(e.amount) AS total_amount,
    COUNT(*) AS expense_count
FROM expense_tb e
WHERE e.member_id = :member_id
  AND TO_CHAR(e.expense_date, 'YYYY-MM') = :month
GROUP BY TO_CHAR(e.expense_date, 'YYYY-MM-DD')
ORDER BY expense_date;

-- 특정 날짜 클릭 시 상세 내역
SELECT
    e.expense_id,
    e.expense_title,
    e.amount,
    TO_CHAR(e.expense_date, 'YYYY-MM-DD') AS expense_date,
    e.payment_method,
    e.memo,
    e.fixed_yn,
    c.category_name,
    c.expense_type
FROM expense_tb e
JOIN expense_category_tb c ON e.category_id = c.category_id
WHERE e.member_id = :member_id
  AND TRUNC(e.expense_date) = TO_DATE(:expenseDate, 'YYYY-MM-DD')
ORDER BY e.expense_id DESC;

-- 결제일이 15일인 방의 2026년 7월 이용분 예시
-- payment_start_date = 2026-06-15
-- payment_close_date = 2026-07-10
-- service_start_date = 2026-07-15
-- service_end_date   = 2026-08-14
========================================================= */
