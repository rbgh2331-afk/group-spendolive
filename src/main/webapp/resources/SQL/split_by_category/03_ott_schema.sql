/* =========================================================
   03. OTT 통합 SQL - 공유방 / 모집글 / 채팅 / 정산
   =========================================================
   이 파일 하나로 처리하는 내용:
   1) OTT 서비스 기준 테이블 생성
   2) 가족·지인 공유방 / 외부 모집방 테이블 생성
   3) 공유방 참여자·신청자 테이블 생성
   4) 공유방 채팅 테이블 생성
   5) 월별 정산 요청 테이블 생성
   6) 참여자별 결제 상태 테이블 생성
   7) 방 삭제 요청 시 환불 기록 테이블 생성

   실행 순서:
   1) 00_reset_all_objects.sql          -- 전체 초기화가 필요할 때만 실행
   2) 01_member_schema.sql              -- member_tb가 먼저 있어야 함
   3) 02_expense_calendar_schema.sql    -- 지출/캘린더가 필요하면 실행
   4) 03_ott_schema.sql                 -- 현재 파일
   5) 04_notice_notification_inquiry_faq.sql -- 공지/문의/FAQ/알림까지 사용할 때 실행

   기존 OTT 관련 파일 정리:
   - 예전에는 03_ott_schema.sql, 08_seed_ott_services.sql,
     12_patch_ott_pickle_rules.sql, 13_patch_ott_room_mode.sql,
     14_patch_ott_login_id_column_names.sql을 따로 관리했습니다.
   - 이제 새 DB를 만드는 기준에서는 이 03번 파일 하나만 실행하면 됩니다.
   - 12/13/14 패치 내용은 최신 CREATE TABLE 구조에 이미 반영했습니다.
   - OTT 서비스 기본 데이터는 08_seed_ott_services.sql 또는 10_seed_all_default_data.sql에서 입력합니다.

   현재 지원 OTT:
   - Netflix
   - Disney+
   - TVING
   - Wavve
   - Watcha
   - Laftel

   금액 계산 기준:
   - OTT는 최고 멤버십 기준으로 고정합니다.
   - 추가 IP/추가 멤버 비용이 있는 OTT는 기본 멤버십 금액에 추가 비용을 더합니다.
   - 1인 금액은 Java 코드에서 아래 방식으로 계산합니다.
       (총 금액 / 최대 인원) + 서비스 수수료 3%
   - DB에는 계산 기준이 되는 base_price, extra_member_fee,
     extra_member_count, default_price, max_member_limit, platform_fee_rate를 저장합니다.

   컬럼명 정리 기준:
   - member_tb.member_id는 숫자 PK입니다.
   - OTT 화면과 코드에서는 로그인 ID 문자열을 기준으로 방장/참여자를 다룹니다.
   - 그래서 OTT 테이블에서는 member_id라는 이름 대신
     host_login_id, member_login_id를 사용합니다.

   현재 사용하지 않는 과거 테이블:
   - ott_room_block_tb
   - escrow_tb
   - platform_revenue_tb
   - payout_tb
   위 테이블들은 현재 최소 구조에서 제외했습니다.
   신고/차단은 report_tb, warning_tb 쪽에서 관리하는 방향이 더 깔끔합니다.
   ========================================================= */

SET DEFINE OFF;

/* =========================================================
   1. OTT 서비스 테이블
   역할: Netflix, TVING, Disney+ 같은 OTT별 고정 요금/최대 인원/수수료 기준 저장
   ========================================================= */
CREATE TABLE ott_service_tb (
    ott_service_id     NUMBER NOT NULL,              -- OTT 서비스 PK
    service_name       VARCHAR2(50) NOT NULL,         -- OTT 이름. 예: Netflix, TVING
    default_price      NUMBER DEFAULT 0 NOT NULL,     -- 최종 분담 기준 금액. 예: 기본요금 + 추가회원비
    fixed_plan_name    VARCHAR2(50) DEFAULT '프리미엄' NOT NULL, -- 고정 사용할 최고 멤버십명
    base_price         NUMBER DEFAULT 0 NOT NULL,     -- OTT 기본 멤버십 가격
    extra_member_fee   NUMBER DEFAULT 0 NOT NULL,     -- 추가 회원 1명당 비용. 없으면 0
    extra_member_count NUMBER DEFAULT 0 NOT NULL,     -- 추가 회원 수. 없으면 0
    max_member_limit   NUMBER DEFAULT 4 NOT NULL,     -- 최대 참여 가능 인원
    platform_fee_rate  NUMBER(5,2) DEFAULT 3 NOT NULL,-- 서비스 수수료율. 현재 3%
    share_yn           CHAR(1) DEFAULT 'Y' NOT NULL,  -- 공유 가능 여부
    risk_level         VARCHAR2(20),                  -- 공유 위험도. LOW/MEDIUM/HIGH
    block_reason       VARCHAR2(500),                 -- 공유 불가 사유

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
   2. OTT 공유방 테이블
   역할: 가족/지인 공유방, 외부 모집방의 기본 정보 저장
   ========================================================= */
CREATE TABLE ott_room_tb (
    room_id              NUMBER NOT NULL,             -- 공유방 PK
    host_login_id       VARCHAR2(20) NOT NULL,        -- 방장 ID. member_tb.id 참조
    ott_service_id       NUMBER NOT NULL,              -- OTT 서비스 ID
    room_name            VARCHAR2(100) NOT NULL,       -- 공유방 이름
    plan_name            VARCHAR2(50) DEFAULT '프리미엄' NOT NULL, -- 요금제명
    total_price          NUMBER NOT NULL,              -- 방 전체 기준 금액
    billing_day          NUMBER NOT NULL,              -- 매월 결제일. 1~31
    member_limit         NUMBER DEFAULT 4 NOT NULL,    -- 최대 인원
    room_mode            VARCHAR2(20) DEFAULT 'RECRUIT' NOT NULL, -- FRIEND/RECRUIT
    status               VARCHAR2(30) DEFAULT 'RECRUITING' NOT NULL, -- 방 상태
    invite_code          VARCHAR2(50),                 -- 가족/지인 초대 코드
    close_requested_at   DATE,                         -- 방 삭제 요청일
    close_effective_date DATE,                         -- 실제 방 종료 예정일
    close_reason         VARCHAR2(500),                -- 방 삭제 사유
    close_notice         VARCHAR2(1000),               -- 방장이 남긴 공지
    closed_at            DATE,                         -- 실제 종료일
    created_at           DATE DEFAULT SYSDATE NOT NULL,-- 생성일
    updated_at           DATE,                         -- 수정일

    CONSTRAINT pk_ott_room PRIMARY KEY (room_id),
    CONSTRAINT fk_ott_room_host FOREIGN KEY (host_login_id) REFERENCES member_tb(id),
    CONSTRAINT fk_ott_room_service FOREIGN KEY (ott_service_id) REFERENCES ott_service_tb(ott_service_id),
    CONSTRAINT ck_ott_room_price CHECK (total_price >= 0),
    CONSTRAINT ck_ott_room_billing_day CHECK (billing_day BETWEEN 1 AND 31),
    CONSTRAINT ck_ott_room_member_limit CHECK (member_limit BETWEEN 1 AND 6),
    CONSTRAINT ck_ott_room_mode CHECK (room_mode IN ('FRIEND', 'RECRUIT')),
    CONSTRAINT ck_ott_room_status CHECK (
        status IN (
            'RECRUITING',          -- 모집중
            'ACTIVE',              -- 정상 운영중
            'PAYMENT_OPEN',        -- 결제 가능 기간
            'REPLACE_RECRUITING',  -- 미결제자 추방 후 대체 모집중
            'CLOSE_REQUESTED',     -- 방 삭제 요청됨. 이번 이용분까지만 유지
            'CLOSED',              -- 종료됨
            'END'                  -- 기존 호환용 종료 상태
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

CREATE INDEX idx_ott_room_host ON ott_room_tb(host_login_id);
CREATE INDEX idx_ott_room_mode ON ott_room_tb(room_mode, status, created_at);
CREATE INDEX idx_ott_room_status ON ott_room_tb(status, created_at);
CREATE INDEX idx_ott_room_close ON ott_room_tb(status, close_effective_date);

/* =========================================================
   3. OTT 공유방 참여자 테이블
   역할: 방장, 참여자, 신청자 상태 관리
   ========================================================= */
CREATE TABLE ott_room_member_tb (
    room_member_id NUMBER NOT NULL,                   -- 방 참여자 PK
    room_id        NUMBER NOT NULL,                   -- 공유방 ID
    member_login_id VARCHAR2(20) NOT NULL,              -- 참여자 로그인 ID. member_tb.id 참조
    member_role    VARCHAR2(20) DEFAULT 'MEMBER' NOT NULL, -- HOST/MEMBER
    share_amount   NUMBER DEFAULT 0 NOT NULL,          -- 순수 N분의1 금액
    fee_rate       NUMBER(5,2) DEFAULT 0 NOT NULL,     -- 수수료율
    fee_amount     NUMBER DEFAULT 0 NOT NULL,          -- 수수료 금액
    pay_amount     NUMBER DEFAULT 0 NOT NULL,          -- 실제 결제해야 할 금액
    joined_at      DATE DEFAULT SYSDATE NOT NULL,      -- 참여/신청일
    status         VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL, -- 신청/참여 상태
    kicked_at      DATE,                               -- 추방일
    kicked_reason  VARCHAR2(500),                      -- 추방 사유
    left_at        DATE,                               -- 탈퇴일
    leave_reserved_yn    CHAR(1) DEFAULT 'N' NOT NULL, -- 나가기 예약 여부
    leave_requested_at   DATE,                         -- 나가기 예약 요청일
    leave_scheduled_date DATE,                         -- 실제 나가기 예정일
    leave_cancelled_at   DATE,                         -- 예약 취소일
    leave_reason         VARCHAR2(500),                -- 나가기 사유

    CONSTRAINT pk_ott_room_member PRIMARY KEY (room_member_id),
    CONSTRAINT fk_room_member_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_room_member_member FOREIGN KEY (member_login_id) REFERENCES member_tb(id),
    CONSTRAINT uk_room_member UNIQUE (room_id, member_login_id),
    CONSTRAINT ck_room_member_role CHECK (member_role IN ('HOST', 'MEMBER')),
    CONSTRAINT ck_room_member_status CHECK (status IN ('APPLIED', 'ACTIVE', 'REJECTED', 'OUT', 'KICKED')),
    CONSTRAINT ck_room_member_leave_reserved CHECK (leave_reserved_yn IN ('Y', 'N')),
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
ALTER TABLE ott_room_member_tb ADD (
    pay_day         NUMBER ,
    pay_late_day    NUMBER DEFAULT 0,
    settlement_status  VARCHAR2(30) DEFAULT 'READY' , -- 방장 정산(송금) 후에 상태
    CONSTRAINT ck_ott_member_settlement_status CHECK (
        settlement_status IN (
            'YET',                -- 다음 정산 회차로 전환되기 전 상태
            'READY',              -- 정산 준비 상태
            'DONE'                -- 정산 완료
        )
    )
);
CREATE INDEX idx_room_member_room ON ott_room_member_tb(room_id, status);
CREATE INDEX idx_room_member_login ON ott_room_member_tb(member_login_id, status);

/* =========================================================
   4. OTT 대화방 메시지 테이블
   역할: 공유방별 채팅 메시지 저장
   ========================================================= */
CREATE TABLE ott_chat_message_tb (
    message_id      NUMBER NOT NULL,                  -- 메시지 PK
    room_id         NUMBER NOT NULL,                  -- 공유방 ID
    sender_id       VARCHAR2(20) NOT NULL,             -- 보낸 사람 ID
    message_content VARCHAR2(1000) NOT NULL,           -- 메시지 내용
    created_at      DATE DEFAULT SYSDATE NOT NULL,     -- 발송일

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
   5. OTT 대화방 읽음 기준 테이블
   역할: 사용자별 마지막 읽은 시간을 저장해서 안 읽은 메시지 수 계산
   ========================================================= */
CREATE TABLE ott_chat_read_tb (
    room_id      NUMBER NOT NULL,                     -- 공유방 ID
    member_login_id VARCHAR2(20) NOT NULL,                -- 사용자 로그인 ID
    last_read_at DATE,                                 -- 마지막 읽은 시간

    CONSTRAINT pk_ott_chat_read PRIMARY KEY (room_id, member_login_id),
    CONSTRAINT fk_chat_read_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_chat_read_member FOREIGN KEY (member_login_id) REFERENCES member_tb(id)
);

/* =========================================================
   6. 월별 정산 요청 테이블
   역할: 방장이 만든 '이번 달/다음 달 정산 요청 묶음' 저장
   정산팀 핵심 테이블 1
   ========================================================= */

CREATE TABLE settlement_tb (
    settlement_id      NUMBER NOT NULL,               -- 정산 요청 PK
    room_id            NUMBER NOT NULL,               -- 공유방 ID
    settlement_month   CHAR(7) NOT NULL,              -- 정산 대상 월. 예: 2026-07
    total_price        NUMBER NOT NULL,               -- 방 전체 기준 금액
    total_fee          NUMBER DEFAULT 0 NOT NULL,     -- 전체 수수료 합계
    total_pay_amount   NUMBER DEFAULT 0 NOT NULL,     -- 전체 결제 요청 금액
    due_date           DATE NOT NULL,                 -- 기존 호환용 마감일
    payment_start_date DATE,                          -- 결제 가능 시작일
    payment_close_date DATE,                          -- 결제 마감일. 이용 시작 7일 전
    service_start_date DATE,                          -- 이용 시작일
    service_end_date   DATE,                          -- 이용 종료일
    replace_start_date DATE,                          -- 대체 모집 시작일
    replace_end_date   DATE,                          -- 대체 모집 종료일
    closed_at          DATE,                          -- 정산 종료일
    status             VARCHAR2(30) DEFAULT 'READY' NOT NULL, -- 정산 상태
    created_at         DATE DEFAULT SYSDATE NOT NULL, -- 생성일
    settlement_status  VARCHAR2(30) DEFAULT 'READY' , -- 방장 정산(송금) 후에 상태

    CONSTRAINT pk_settlement PRIMARY KEY (settlement_id),
    CONSTRAINT fk_settlement_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT uk_settlement_room_month UNIQUE (room_id, settlement_month),
    CONSTRAINT ck_settlement_month CHECK (REGEXP_LIKE(settlement_month, '^[0-9]{4}-[0-9]{2}$')),
    CONSTRAINT ck_settlement_status CHECK (
        status IN (
            'READY',              -- 정산 생성 전/준비 필요 없
            'REQUESTED',          -- 방장이 정산 요청함 필요 없
            'DONE',               -- 기존 호환용 완료
            'PAYMENT_OPEN',       -- 결제 가능 기간 
            'REPLACE_RECRUITING', -- 미결제자 추방 후 대체 모집
            'CONFIRMED',          -- 정산 확정
            'CANCELLED',          -- 정산 취소
            'CLOSED'              -- 정산 종료
        )),
    CONSTRAINT ck_settlement_payout_status CHECK (
        settlement_status IN (
            'YET',
            'READY',              -- 정산 안됨
            'DONE'               -- 정산 완료
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
   결제/환불 및 에스크로 관련 테이블은 03-1_payment.sql에서 생성합니다.
   03_ott_schema.sql 실행 직후 03-1_payment.sql을 실행하세요.
   ========================================================= */

/* 기존 DB의 나가기 예약 컬럼 추가는 patch/14_ott_leave_reservation_patch.sql을 사용합니다. */

/* =========================================================
   정산/결제 구현 기준 요약
   =========================================================
   1) 방장이 정산 요청
      - settlement_tb 1건 생성
      - 참여자별 settlement_payment_tb 여러 건 생성

   2) 참여자가 결제
      - settlement_payment_tb.payment_status = 'PAID'
      - settlement_payment_tb.paid_at = SYSDATE

   3) 방장이 입금 확인
      - settlement_payment_tb.payment_status = 'CONFIRMED'
      - settlement_payment_tb.confirmed_at = SYSDATE

   4) 결제 마감일이 지남
      - 미결제자는 settlement_payment_tb.payment_status = 'EXPIRED'
      - 해당 멤버는 ott_room_member_tb.status = 'KICKED'
      - 방은 필요 시 ott_room_tb.status = 'REPLACE_RECRUITING'

   5) 방 삭제 요청
      - ott_room_tb.status = 'CLOSE_REQUESTED'
      - 다음 이용분 결제 완료자는 settlement_refund_tb에 환불 기록 생성
   ========================================================= */

/* =========================================================
   실행 완료 후 확인용 쿼리
   =========================================================
   SELECT * FROM ott_service_tb ORDER BY ott_service_id;
   SELECT table_name FROM user_tables WHERE table_name LIKE 'OTT_%' OR table_name LIKE 'SETTLEMENT_%';
   ========================================================= */
