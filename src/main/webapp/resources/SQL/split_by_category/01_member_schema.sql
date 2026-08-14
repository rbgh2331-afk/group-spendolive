/* =========================================================
   01. 회원 / 계좌 / 카드 / 거래 스키마
   ---------------------------------------------------------
   선행 파일 : 없음
   후속 파일 : 02_expense_calendar_schema.sql 이후 기능별 스키마
   규칙      : 새 DB 생성용 파일이므로 동일 컬럼을 ALTER로 다시 추가하지 않는다.
   ========================================================= */
SET DEFINE OFF;

/* 1. 회원 */
CREATE TABLE member_tb (
    member_id       NUMBER NOT NULL,
    id              VARCHAR2(20) NOT NULL,
    email           VARCHAR2(100) NOT NULL,
    password        VARCHAR2(255) NOT NULL,
    member_name     VARCHAR2(50) NOT NULL,
    nickname        VARCHAR2(50),
    phone           VARCHAR2(20),
    login_type      VARCHAR2(20) DEFAULT 'LOCAL' NOT NULL,
    verify_type     VARCHAR2(20) NOT NULL,
    role            VARCHAR2(20) DEFAULT 'USER' NOT NULL,
    status          VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    blocked_until   DATE,
    warning_count   NUMBER DEFAULT 0 NOT NULL,
    warninged_at    DATE,
    last_login_at   DATE,
    created_at      DATE DEFAULT SYSDATE NOT NULL,
    updated_at      DATE,
    account_status  VARCHAR2(4) DEFAULT 'NO' NOT NULL,
    card_status     VARCHAR2(4) DEFAULT 'NO' NOT NULL,

    CONSTRAINT pk_member PRIMARY KEY (member_id),
    CONSTRAINT uk_member_id UNIQUE (id),
    CONSTRAINT uk_member_email UNIQUE (email),
    CONSTRAINT ck_member_login_type CHECK (login_type IN ('LOCAL', 'KAKAO', 'GOOGLE', 'NAVER')),
    CONSTRAINT ck_member_verify_type CHECK (verify_type IN ('EMAIL', 'PHONE')),
    CONSTRAINT ck_member_role CHECK (role IN ('USER', 'HOST', 'ADMIN')),
    CONSTRAINT ck_member_status CHECK (status IN ('ACTIVE', 'LEAVE', 'BLOCK', 'PERM_BLOCK')),
    CONSTRAINT ck_member_warning_count CHECK (warning_count >= 0),
    CONSTRAINT ck_member_account_link CHECK (account_status IN ('YES', 'NO')),
    CONSTRAINT ck_member_card_link CHECK (card_status IN ('YES', 'NO'))
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

/* 2. 오픈뱅킹 계좌
   - status=YES는 회원의 현재 주계좌를 의미한다.
   - OPEN_BANK_TOKEN은 실제 토큰 길이를 고려해 VARCHAR2(1000)으로 생성한다. */
CREATE TABLE member_account_tb (
    account_idx        NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id                 VARCHAR2(20) NOT NULL,
    bank_code          VARCHAR2(50) NOT NULL,
    account_number     VARCHAR2(30) NOT NULL,
    fintech_use_num    VARCHAR2(50) NOT NULL,
    balance            NUMBER DEFAULT 0,
    open_bank_token    VARCHAR2(1000) NOT NULL,
    open_bank_user_seq VARCHAR2(50) NOT NULL,
    account_holder_nam VARCHAR2(50),
    reg_date           DATE DEFAULT SYSDATE,
    account_name       VARCHAR2(20) DEFAULT '계좌',
    to_date            VARCHAR2(8) DEFAULT '20260721',
    from_date          VARCHAR2(8) DEFAULT '20260701',
    to_time            VARCHAR2(6) DEFAULT '235959',
    from_time          VARCHAR2(6) DEFAULT '000000',
    status             VARCHAR2(20) DEFAULT 'NO' NOT NULL,

    CONSTRAINT fk_member_account_member FOREIGN KEY (id) REFERENCES member_tb(id) ON DELETE CASCADE,
    CONSTRAINT ck_member_account_primary CHECK (status IN ('YES', 'NO'))
);

CREATE INDEX idx_member_account_member ON member_account_tb(id, status, account_idx);

/* 3. 결제 카드
   - status=YES는 회원의 현재 주카드를 의미한다.
   - card_name은 마이페이지에서 수정하는 사용자 표시명이다. */
CREATE TABLE member_card_tb (
    card_idx      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id            VARCHAR2(20) NOT NULL,
    billing_key   VARCHAR2(100) NOT NULL,
    card_company  VARCHAR2(50),
    card_number   VARCHAR2(20),
    card_name     VARCHAR2(30),
    reg_date      DATE DEFAULT SYSDATE,
    status        VARCHAR2(20) DEFAULT 'NO' NOT NULL,

    CONSTRAINT fk_member_card_member FOREIGN KEY (id) REFERENCES member_tb(id) ON DELETE CASCADE,
    CONSTRAINT ck_member_card_primary CHECK (status IN ('YES', 'NO'))
);

CREATE INDEX idx_member_card_member ON member_card_tb(id, status, card_idx);

/* 4. 계좌 거래내역
   - BALANCE_AFTER는 해당 거래 직후 잔액이며 러닝밸런스 표시에 사용한다. */
CREATE TABLE member_tran_tb (
    member_tran_idx NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id              VARCHAR2(20) NOT NULL,
    account_idx     NUMBER NOT NULL,
    tran_date       VARCHAR2(30) NOT NULL,
    inout_type      VARCHAR2(10) NOT NULL,
    tran_amt        NUMBER,
    balance_after   NUMBER,
    reg_date        DATE DEFAULT SYSDATE,

    CONSTRAINT fk_member_tran_member FOREIGN KEY (id) REFERENCES member_tb(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_tran_account FOREIGN KEY (account_idx) REFERENCES member_account_tb(account_idx) ON DELETE CASCADE
);

CREATE INDEX idx_member_tran_account ON member_tran_tb(id, account_idx, member_tran_idx);

COMMENT ON COLUMN member_tran_tb.balance_after IS '해당 거래가 끝난 직후의 계좌 잔액';

PROMPT [01] 회원/계좌/카드/거래 스키마 생성 완료
