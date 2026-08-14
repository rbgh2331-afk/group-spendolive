/* ============================================================================
   SpendOlive 전체 DB 생성 스크립트
   - Oracle XE 21c / SQL Developer F5(스크립트 실행) 기준
   - 최신 프로젝트 소스에서 실제 참조하는 27개 테이블만 구성
   - PK, FK, UNIQUE, CHECK, INDEX, SEQUENCE, TRIGGER 포함
   - 테스트 회원/지출/알림은 넣지 않고 운영에 필요한 기준 데이터만 입력
   ============================================================================ */

SET DEFINE OFF;
SET SERVEROUTPUT ON;

PROMPT [1/8] 회원/계좌/카드/거래 테이블 생성

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
    CONSTRAINT ck_member_login_type CHECK (login_type IN ('LOCAL','KAKAO','GOOGLE','NAVER')),
    CONSTRAINT ck_member_verify_type CHECK (verify_type IN ('EMAIL','PHONE')),
    CONSTRAINT ck_member_role CHECK (role IN ('USER','HOST','ADMIN')),
    CONSTRAINT ck_member_status CHECK (status IN ('ACTIVE','LEAVE','BLOCK','PERM_BLOCK')),
    CONSTRAINT ck_member_warning_count CHECK (warning_count >= 0),
    CONSTRAINT ck_member_account_link CHECK (account_status IN ('YES','NO')),
    CONSTRAINT ck_member_card_link CHECK (card_status IN ('YES','NO'))
);

CREATE SEQUENCE seq_member START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_member_bi BEFORE INSERT ON member_tb FOR EACH ROW
WHEN (NEW.member_id IS NULL)
BEGIN SELECT seq_member.NEXTVAL INTO :NEW.member_id FROM dual; END;
/

CREATE TABLE member_account_tb (
    account_idx        NUMBER NOT NULL,
    id                 VARCHAR2(20) NOT NULL,
    bank_code          VARCHAR2(50) NOT NULL,
    account_number     VARCHAR2(30) NOT NULL,
    fintech_use_num    VARCHAR2(50) NOT NULL,
    balance            NUMBER DEFAULT 0 NOT NULL,
    open_bank_token    VARCHAR2(1000) NOT NULL,
    open_bank_user_seq VARCHAR2(50) NOT NULL,
    account_holder_nam VARCHAR2(50),
    reg_date           DATE DEFAULT SYSDATE NOT NULL,
    account_name       VARCHAR2(20) DEFAULT '계좌' NOT NULL,
    to_date            VARCHAR2(8),
    from_date          VARCHAR2(8),
    to_time            VARCHAR2(6),
    from_time          VARCHAR2(6),
    status             VARCHAR2(20) DEFAULT 'NO' NOT NULL,
    CONSTRAINT pk_member_account PRIMARY KEY (account_idx),
    CONSTRAINT fk_member_account_member FOREIGN KEY (id) REFERENCES member_tb(id) ON DELETE CASCADE,
    CONSTRAINT ck_member_account_primary CHECK (status IN ('YES','NO'))
);
CREATE SEQUENCE seq_member_account START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_member_account_bi BEFORE INSERT ON member_account_tb FOR EACH ROW
WHEN (NEW.account_idx IS NULL)
BEGIN SELECT seq_member_account.NEXTVAL INTO :NEW.account_idx FROM dual; END;
/
CREATE INDEX idx_member_account_id ON member_account_tb(id, status, account_idx);

CREATE TABLE member_card_tb (
    card_idx     NUMBER NOT NULL,
    id           VARCHAR2(20) NOT NULL,
    billing_key  VARCHAR2(100) NOT NULL,
    card_company VARCHAR2(50),
    card_number  VARCHAR2(20),
    card_name    VARCHAR2(30),
    reg_date     DATE DEFAULT SYSDATE NOT NULL,
    status       VARCHAR2(20) DEFAULT 'NO' NOT NULL,
    CONSTRAINT pk_member_card PRIMARY KEY (card_idx),
    CONSTRAINT fk_member_card_member FOREIGN KEY (id) REFERENCES member_tb(id) ON DELETE CASCADE,
    CONSTRAINT ck_member_card_primary CHECK (status IN ('YES','NO'))
);
CREATE SEQUENCE seq_member_card START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_member_card_bi BEFORE INSERT ON member_card_tb FOR EACH ROW
WHEN (NEW.card_idx IS NULL)
BEGIN SELECT seq_member_card.NEXTVAL INTO :NEW.card_idx FROM dual; END;
/
CREATE INDEX idx_member_card_id ON member_card_tb(id, status, card_idx);

CREATE TABLE member_tran_tb (
    member_tran_idx NUMBER NOT NULL,
    id              VARCHAR2(20) NOT NULL,
    account_idx     NUMBER NOT NULL,
    tran_date       VARCHAR2(30) NOT NULL,
    inout_type      VARCHAR2(10) NOT NULL,
    tran_amt        NUMBER,
    balance_after   NUMBER,
    reg_date        DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_member_tran PRIMARY KEY (member_tran_idx),
    CONSTRAINT fk_member_tran_member FOREIGN KEY (id) REFERENCES member_tb(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_tran_account FOREIGN KEY (account_idx) REFERENCES member_account_tb(account_idx) ON DELETE CASCADE
);
CREATE SEQUENCE seq_member_tran START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_member_tran_bi BEFORE INSERT ON member_tran_tb FOR EACH ROW
WHEN (NEW.member_tran_idx IS NULL)
BEGIN SELECT seq_member_tran.NEXTVAL INTO :NEW.member_tran_idx FROM dual; END;
/
CREATE INDEX idx_member_tran_account ON member_tran_tb(id, account_idx, tran_date);

PROMPT [2/8] 지출/예산 테이블 생성

CREATE TABLE expense_category_tb (
    category_id   NUMBER NOT NULL,
    category_name VARCHAR2(50) NOT NULL,
    expense_type  VARCHAR2(20) NOT NULL,
    sort_order    NUMBER DEFAULT 0 NOT NULL,
    CONSTRAINT pk_expense_category PRIMARY KEY (category_id),
    CONSTRAINT uk_expense_category UNIQUE (category_name, expense_type),
    CONSTRAINT ck_expense_category_type CHECK (expense_type IN ('FIXED','VARIABLE','OTT'))
);
CREATE SEQUENCE seq_expense_category START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_expense_category_bi BEFORE INSERT ON expense_category_tb FOR EACH ROW
WHEN (NEW.category_id IS NULL)
BEGIN SELECT seq_expense_category.NEXTVAL INTO :NEW.category_id FROM dual; END;
/

CREATE TABLE expense_tb (
    expense_id      NUMBER NOT NULL,
    member_id       NUMBER NOT NULL,
    category_id     NUMBER NOT NULL,
    expense_title   VARCHAR2(100) NOT NULL,
    amount          NUMBER NOT NULL,
    expense_date    DATE NOT NULL,
    payment_method  VARCHAR2(30),
    memo            VARCHAR2(1000),
    repeat_yn       CHAR(1) DEFAULT 'N' NOT NULL,
    repeat_cycle    VARCHAR2(20),
    repeat_end_date DATE,
    fixed_yn        CHAR(1) DEFAULT 'N' NOT NULL,
    created_at      DATE DEFAULT SYSDATE NOT NULL,
    updated_at      DATE,
    CONSTRAINT pk_expense PRIMARY KEY (expense_id),
    CONSTRAINT fk_expense_member FOREIGN KEY (member_id) REFERENCES member_tb(member_id),
    CONSTRAINT fk_expense_category FOREIGN KEY (category_id) REFERENCES expense_category_tb(category_id),
    CONSTRAINT ck_expense_amount CHECK (amount >= 0),
    CONSTRAINT ck_expense_repeat_yn CHECK (repeat_yn IN ('Y','N')),
    CONSTRAINT ck_expense_fixed_yn CHECK (fixed_yn IN ('Y','N')),
    CONSTRAINT ck_expense_repeat_cycle CHECK (repeat_cycle IS NULL OR repeat_cycle IN ('MONTHLY','WEEKLY','YEARLY')),
    CONSTRAINT ck_expense_repeat_end CHECK (repeat_end_date IS NULL OR repeat_end_date >= expense_date)
);
CREATE SEQUENCE seq_expense START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_expense_bi BEFORE INSERT ON expense_tb FOR EACH ROW
WHEN (NEW.expense_id IS NULL)
BEGIN SELECT seq_expense.NEXTVAL INTO :NEW.expense_id FROM dual; END;
/
CREATE INDEX idx_expense_member_date ON expense_tb(member_id, expense_date);
CREATE INDEX idx_expense_category ON expense_tb(category_id);

CREATE TABLE monthly_budget_tb (
    budget_id     NUMBER NOT NULL,
    member_id     NUMBER NOT NULL,
    budget_month  VARCHAR2(7) NOT NULL,
    budget_amount NUMBER DEFAULT 0 NOT NULL,
    created_at    DATE DEFAULT SYSDATE NOT NULL,
    updated_at    DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_monthly_budget PRIMARY KEY (budget_id),
    CONSTRAINT fk_monthly_budget_member FOREIGN KEY (member_id) REFERENCES member_tb(member_id) ON DELETE CASCADE,
    CONSTRAINT uk_monthly_budget UNIQUE (member_id, budget_month),
    CONSTRAINT ck_monthly_budget_amount CHECK (budget_amount >= 0),
    CONSTRAINT ck_monthly_budget_month CHECK (REGEXP_LIKE(budget_month, '^[0-9]{4}-(0[1-9]|1[0-2])$'))
);
CREATE SEQUENCE seq_monthly_budget START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_monthly_budget_bi BEFORE INSERT ON monthly_budget_tb FOR EACH ROW
WHEN (NEW.budget_id IS NULL)
BEGIN SELECT seq_monthly_budget.NEXTVAL INTO :NEW.budget_id FROM dual; END;
/

PROMPT [3/8] OTT/채팅/정산 테이블 생성

CREATE TABLE ott_service_tb (
    ott_service_id     NUMBER NOT NULL,
    service_name       VARCHAR2(50) NOT NULL,
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
    CONSTRAINT ck_ott_service_share CHECK (share_yn IN ('Y','N')),
    CONSTRAINT ck_ott_service_risk CHECK (risk_level IS NULL OR risk_level IN ('LOW','MEDIUM','HIGH')),
    CONSTRAINT ck_ott_service_price CHECK (default_price >= 0 AND base_price >= 0),
    CONSTRAINT ck_ott_service_extra CHECK (extra_member_fee >= 0 AND extra_member_count >= 0),
    CONSTRAINT ck_ott_service_limit CHECK (max_member_limit BETWEEN 1 AND 6),
    CONSTRAINT ck_ott_service_fee CHECK (platform_fee_rate >= 0)
);
CREATE SEQUENCE seq_ott_service START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_ott_service_bi BEFORE INSERT ON ott_service_tb FOR EACH ROW
WHEN (NEW.ott_service_id IS NULL)
BEGIN SELECT seq_ott_service.NEXTVAL INTO :NEW.ott_service_id FROM dual; END;
/

CREATE TABLE ott_room_tb (
    room_id              NUMBER NOT NULL,
    host_login_id        VARCHAR2(20) NOT NULL,
    ott_service_id       NUMBER NOT NULL,
    room_name            VARCHAR2(100) NOT NULL,
    plan_name            VARCHAR2(50) DEFAULT '프리미엄' NOT NULL,
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
    CONSTRAINT fk_ott_room_host FOREIGN KEY (host_login_id) REFERENCES member_tb(id),
    CONSTRAINT fk_ott_room_service FOREIGN KEY (ott_service_id) REFERENCES ott_service_tb(ott_service_id),
    CONSTRAINT uk_ott_room_invite UNIQUE (invite_code),
    CONSTRAINT ck_ott_room_price CHECK (total_price >= 0),
    CONSTRAINT ck_ott_room_billing CHECK (billing_day BETWEEN 1 AND 31),
    CONSTRAINT ck_ott_room_limit CHECK (member_limit BETWEEN 1 AND 6),
    CONSTRAINT ck_ott_room_mode CHECK (room_mode IN ('FRIEND','RECRUIT')),
    CONSTRAINT ck_ott_room_status CHECK (status IN ('RECRUITING','FIRST','ACTIVE','PAYMENT_OPEN','REPLACE_RECRUITING','CLOSE_REQUESTED','CLOSED','END'))
);
CREATE SEQUENCE seq_ott_room START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_ott_room_bi BEFORE INSERT ON ott_room_tb FOR EACH ROW
WHEN (NEW.room_id IS NULL)
BEGIN SELECT seq_ott_room.NEXTVAL INTO :NEW.room_id FROM dual; END;
/
CREATE INDEX idx_ott_room_host ON ott_room_tb(host_login_id);
CREATE INDEX idx_ott_room_mode ON ott_room_tb(room_mode, status, created_at);
CREATE INDEX idx_ott_room_status ON ott_room_tb(status, created_at);

CREATE TABLE ott_room_member_tb (
    room_member_id      NUMBER NOT NULL,
    room_id             NUMBER NOT NULL,
    member_login_id     VARCHAR2(20) NOT NULL,
    member_role         VARCHAR2(20) DEFAULT 'MEMBER' NOT NULL,
    share_amount        NUMBER DEFAULT 0 NOT NULL,
    fee_rate            NUMBER(5,2) DEFAULT 0 NOT NULL,
    fee_amount          NUMBER DEFAULT 0 NOT NULL,
    pay_amount          NUMBER DEFAULT 0 NOT NULL,
    pay_day             NUMBER,
    pay_late_day        NUMBER DEFAULT 0 NOT NULL,
    settlement_status   VARCHAR2(30) DEFAULT 'READY' NOT NULL,
    joined_at           DATE DEFAULT SYSDATE NOT NULL,
    status              VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    kicked_at           DATE,
    kicked_reason       VARCHAR2(500),
    left_at             DATE,
    leave_reserved_yn   CHAR(1) DEFAULT 'N' NOT NULL,
    leave_requested_at  DATE,
    leave_scheduled_date DATE,
    leave_cancelled_at  DATE,
    leave_reason        VARCHAR2(500),
    CONSTRAINT pk_ott_room_member PRIMARY KEY (room_member_id),
    CONSTRAINT fk_room_member_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_room_member_member FOREIGN KEY (member_login_id) REFERENCES member_tb(id),
    CONSTRAINT uk_room_member UNIQUE (room_id, member_login_id),
    CONSTRAINT ck_room_member_role CHECK (member_role IN ('HOST','MEMBER')),
    CONSTRAINT ck_room_member_status CHECK (status IN ('APPLIED','ACTIVE','REJECTED','OUT','KICKED')),
    CONSTRAINT ck_room_member_amount CHECK (share_amount >= 0 AND fee_rate >= 0 AND fee_amount >= 0 AND pay_amount >= 0),
    CONSTRAINT ck_room_member_settle CHECK (settlement_status IN ('YET','READY','DONE')),
    CONSTRAINT ck_room_member_leave CHECK (leave_reserved_yn IN ('Y','N'))
);
CREATE SEQUENCE seq_ott_room_member START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_ott_room_member_bi BEFORE INSERT ON ott_room_member_tb FOR EACH ROW
WHEN (NEW.room_member_id IS NULL)
BEGIN SELECT seq_ott_room_member.NEXTVAL INTO :NEW.room_member_id FROM dual; END;
/
CREATE INDEX idx_room_member_room ON ott_room_member_tb(room_id, status);
CREATE INDEX idx_room_member_login ON ott_room_member_tb(member_login_id, status);

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
CREATE OR REPLACE TRIGGER trg_ott_chat_message_bi BEFORE INSERT ON ott_chat_message_tb FOR EACH ROW
WHEN (NEW.message_id IS NULL)
BEGIN SELECT seq_ott_chat_message.NEXTVAL INTO :NEW.message_id FROM dual; END;
/
CREATE INDEX idx_chat_message_room ON ott_chat_message_tb(room_id, created_at, message_id);

CREATE TABLE ott_chat_read_tb (
    room_id          NUMBER,
    member_login_id  VARCHAR2(20) NOT NULL,
    last_read_at     DATE,
    CONSTRAINT pk_ott_chat_read PRIMARY KEY (room_id, member_login_id),
    CONSTRAINT fk_chat_read_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_chat_read_member FOREIGN KEY (member_login_id) REFERENCES member_tb(id)
);

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
    settlement_status  VARCHAR2(30) DEFAULT 'READY' NOT NULL,
    created_at         DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_settlement PRIMARY KEY (settlement_id),
    CONSTRAINT fk_settlement_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT uk_settlement_room_month UNIQUE (room_id, settlement_month),
    CONSTRAINT ck_settlement_month CHECK (REGEXP_LIKE(settlement_month, '^[0-9]{4}-(0[1-9]|1[0-2])$')),
    CONSTRAINT ck_settlement_status CHECK (status IN ('READY','REQUESTED','DONE','PAYMENT_OPEN','REPLACE_RECRUITING','CONFIRMED','CANCELLED','CLOSED')),
    CONSTRAINT ck_settlement_payout CHECK (settlement_status IN ('YET','READY','DONE')),
    CONSTRAINT ck_settlement_amount CHECK (total_price >= 0 AND total_fee >= 0 AND total_pay_amount >= 0),
    CONSTRAINT ck_settlement_pay_period CHECK (payment_start_date IS NULL OR payment_close_date IS NULL OR payment_start_date <= payment_close_date),
    CONSTRAINT ck_settlement_service_period CHECK (service_start_date IS NULL OR service_end_date IS NULL OR service_start_date <= service_end_date)
);
CREATE SEQUENCE seq_settlement START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_settlement_bi BEFORE INSERT ON settlement_tb FOR EACH ROW
WHEN (NEW.settlement_id IS NULL)
BEGIN SELECT seq_settlement.NEXTVAL INTO :NEW.settlement_id FROM dual; END;
/
CREATE INDEX idx_settlement_room ON settlement_tb(room_id, settlement_month);
CREATE INDEX idx_settlement_status ON settlement_tb(status, payment_close_date);

PROMPT [4/8] 결제/환불/지급 테이블 생성

CREATE TABLE settlement_payment_tb (
    payment_id      NUMBER NOT NULL,
    settlement_id   NUMBER NOT NULL,
    id              VARCHAR2(20) NOT NULL,
    base_amount     NUMBER NOT NULL,
    fee_rate        NUMBER(5,2) DEFAULT 3 NOT NULL,
    fee_amount      NUMBER DEFAULT 0 NOT NULL,
    total_amount    NUMBER NOT NULL,
    payment_status  VARCHAR2(30) DEFAULT 'UNPAID' NOT NULL,
    card_number     VARCHAR2(50),
    card_company    VARCHAR2(20),
    paid_at         DATE,
    confirmed_at    DATE,
    expired_at      DATE,
    cancelled_at    DATE,
    paymentKey      VARCHAR2(100),
    orderId         VARCHAR2(100),
    memo            VARCHAR2(500),
    CONSTRAINT pk_settlement_payment PRIMARY KEY (payment_id),
    CONSTRAINT fk_payment_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_tb(settlement_id),
    CONSTRAINT fk_payment_member FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT uk_payment_member UNIQUE (settlement_id, id),
    CONSTRAINT ck_payment_amount CHECK (base_amount >= 0 AND fee_rate >= 0 AND fee_amount >= 0 AND total_amount >= 0),
    CONSTRAINT ck_payment_status CHECK (payment_status IN ('UNPAID','PAID','CONFIRMED','EXPIRED','CANCELLED','REFUND_REQUESTED','REFUNDED'))
);
CREATE SEQUENCE seq_settlement_payment START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_settlement_payment_bi BEFORE INSERT ON settlement_payment_tb FOR EACH ROW
WHEN (NEW.payment_id IS NULL)
BEGIN SELECT seq_settlement_payment.NEXTVAL INTO :NEW.payment_id FROM dual; END;
/
CREATE INDEX idx_payment_settlement ON settlement_payment_tb(settlement_id);
CREATE INDEX idx_payment_member ON settlement_payment_tb(id, payment_status);

CREATE TABLE settlement_refund_tb (
    refund_id        NUMBER NOT NULL,
    payment_id       NUMBER NOT NULL,
    settlement_id    NUMBER NOT NULL,
    member_login_id  VARCHAR2(20) NOT NULL,
    refund_amount    NUMBER NOT NULL,
    refund_reason    VARCHAR2(30) DEFAULT 'ROOM_CLOSE' NOT NULL,
    refund_status    VARCHAR2(30) DEFAULT 'REQUESTED' NOT NULL,
    requested_at     DATE DEFAULT SYSDATE NOT NULL,
    completed_at     DATE,
    memo             VARCHAR2(500),
    CONSTRAINT pk_settlement_refund PRIMARY KEY (refund_id),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES settlement_payment_tb(payment_id),
    CONSTRAINT fk_refund_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_tb(settlement_id),
    CONSTRAINT fk_refund_member FOREIGN KEY (member_login_id) REFERENCES member_tb(id),
    CONSTRAINT uk_refund_payment UNIQUE (payment_id),
    CONSTRAINT ck_refund_amount CHECK (refund_amount >= 0),
    CONSTRAINT ck_refund_reason CHECK (refund_reason IN ('ROOM_CLOSE','PAYMENT_CANCEL','ADMIN_CANCEL','ETC')),
    CONSTRAINT ck_refund_status CHECK (refund_status IN ('REQUESTED','COMPLETED','FAILED'))
);
CREATE SEQUENCE seq_settlement_refund START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_settlement_refund_bi BEFORE INSERT ON settlement_refund_tb FOR EACH ROW
WHEN (NEW.refund_id IS NULL)
BEGIN SELECT seq_settlement_refund.NEXTVAL INTO :NEW.refund_id FROM dual; END;
/
CREATE INDEX idx_refund_member ON settlement_refund_tb(member_login_id, refund_status);
CREATE TABLE escrow_payout_tb (
    escrow_payout_id NUMBER NOT NULL,
    settlement_id    NUMBER NOT NULL,
    room_id          NUMBER NOT NULL,
    payer_id         VARCHAR2(20) NOT NULL,
    host_id          VARCHAR2(20) NOT NULL,
    amount           NUMBER NOT NULL,
    status           VARCHAR2(30) DEFAULT 'HELD' NOT NULL,
    created_at       DATE DEFAULT SYSDATE NOT NULL,
    payout_due_date  DATE,
    payout_at        DATE,
    CONSTRAINT pk_escrow_payout PRIMARY KEY (escrow_payout_id),
    CONSTRAINT fk_ep_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_tb(settlement_id),
    CONSTRAINT fk_ep_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_ep_payer FOREIGN KEY (payer_id) REFERENCES member_tb(id),
    CONSTRAINT fk_ep_host FOREIGN KEY (host_id) REFERENCES member_tb(id),
    CONSTRAINT ck_ep_amount CHECK (amount >= 0),
    CONSTRAINT ck_ep_status CHECK (status IN ('HELD','RELEASED','REFUNDED','CANCELLED'))
);
CREATE SEQUENCE seq_escrow_payout START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_escrow_payout_bi BEFORE INSERT ON escrow_payout_tb FOR EACH ROW
WHEN (NEW.escrow_payout_id IS NULL)
BEGIN SELECT seq_escrow_payout.NEXTVAL INTO :NEW.escrow_payout_id FROM dual; END;
/
CREATE INDEX idx_ep_host_status ON escrow_payout_tb(host_id, status);

CREATE TABLE platform_revenue_tb (
    revenue_id    NUMBER NOT NULL,
    settlement_id NUMBER NOT NULL,
    room_id       NUMBER NOT NULL,
    payer_id      VARCHAR2(20) NOT NULL,
    base_amount   NUMBER NOT NULL,
    fee_rate      NUMBER(5,2) DEFAULT 3 NOT NULL,
    fee_amount    NUMBER NOT NULL,
    status        VARCHAR2(20) DEFAULT 'EARNED' NOT NULL,
    created_at    DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_platform_revenue PRIMARY KEY (revenue_id),
    CONSTRAINT fk_revenue_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_tb(settlement_id),
    CONSTRAINT fk_revenue_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT fk_revenue_payer FOREIGN KEY (payer_id) REFERENCES member_tb(id),
    CONSTRAINT ck_revenue_amount CHECK (base_amount >= 0 AND fee_rate >= 0 AND fee_amount >= 0),
    CONSTRAINT ck_revenue_status CHECK (status IN ('EARNED','REFUNDED','CANCELLED'))
);
CREATE SEQUENCE seq_platform_revenue START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_platform_revenue_bi BEFORE INSERT ON platform_revenue_tb FOR EACH ROW
WHEN (NEW.revenue_id IS NULL)
BEGIN SELECT seq_platform_revenue.NEXTVAL INTO :NEW.revenue_id FROM dual; END;
/

CREATE TABLE seller_account_tb (
    seller_idx      NUMBER NOT NULL,
    member_id       VARCHAR2(20) NOT NULL,
    bank_name       VARCHAR2(50) NOT NULL,
    account_number  VARCHAR2(30) NOT NULL,
    traceId         VARCHAR2(100) NOT NULL,
    reg_date        DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_seller_account PRIMARY KEY (seller_idx),
    CONSTRAINT fk_seller_member FOREIGN KEY (member_id) REFERENCES member_tb(id) ON DELETE CASCADE
);
CREATE SEQUENCE seq_seller_account START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_seller_account_bi BEFORE INSERT ON seller_account_tb FOR EACH ROW
WHEN (NEW.seller_idx IS NULL)
BEGIN SELECT seq_seller_account.NEXTVAL INTO :NEW.seller_idx FROM dual; END;
/

PROMPT [5/8] 공지/문의/FAQ/알림 테이블 생성

CREATE TABLE notice_tb (
    notice_id  NUMBER NOT NULL,
    admin_id   VARCHAR2(20) NOT NULL,
    title      VARCHAR2(200) NOT NULL,
    content    CLOB NOT NULL,
    pinned_yn  CHAR(1) DEFAULT 'N' NOT NULL,
    created_at DATE DEFAULT SYSDATE NOT NULL,
    updated_at DATE,
    CONSTRAINT pk_notice PRIMARY KEY (notice_id),
    CONSTRAINT fk_notice_admin FOREIGN KEY (admin_id) REFERENCES member_tb(id),
    CONSTRAINT ck_notice_pinned CHECK (pinned_yn IN ('Y','N'))
);
CREATE SEQUENCE seq_notice START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_notice_bi BEFORE INSERT ON notice_tb FOR EACH ROW
WHEN (NEW.notice_id IS NULL)
BEGIN SELECT seq_notice.NEXTVAL INTO :NEW.notice_id FROM dual; END;
/

CREATE TABLE inquiry_tb (
    inquiry_id    NUMBER NOT NULL,
    id            VARCHAR2(20) NOT NULL,
    category      VARCHAR2(30) NOT NULL,
    inquiry_type  VARCHAR2(30) NOT NULL,
    title         VARCHAR2(50 CHAR) NOT NULL,
    content       VARCHAR2(1000 CHAR) NOT NULL,
    status        VARCHAR2(10) DEFAULT 'WAIT' NOT NULL,
    reg_date      DATE DEFAULT SYSDATE NOT NULL,
    reply_content VARCHAR2(1000 CHAR),
    reply_date    DATE,
    CONSTRAINT pk_inquiry PRIMARY KEY (inquiry_id),
    CONSTRAINT fk_inquiry_member FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT ck_inquiry_status CHECK (status IN ('WAIT','DONE','REVIEW'))
);
CREATE SEQUENCE inquiry_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_inquiry_bi BEFORE INSERT ON inquiry_tb FOR EACH ROW
WHEN (NEW.inquiry_id IS NULL)
BEGIN SELECT inquiry_seq.NEXTVAL INTO :NEW.inquiry_id FROM dual; END;
/
CREATE INDEX idx_inquiry_member ON inquiry_tb(id, status, reg_date);

CREATE TABLE inquiry_file_tb (
    file_id     NUMBER NOT NULL,
    inquiry_id  NUMBER NOT NULL,
    origin_name VARCHAR2(200) NOT NULL,
    saved_name  VARCHAR2(200) NOT NULL,
    file_path   VARCHAR2(500) NOT NULL,
    file_size   NUMBER,
    reg_date    DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_inquiry_file PRIMARY KEY (file_id),
    CONSTRAINT fk_inquiry_file FOREIGN KEY (inquiry_id) REFERENCES inquiry_tb(inquiry_id) ON DELETE CASCADE
);
CREATE SEQUENCE inquiry_file_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_inquiry_file_bi BEFORE INSERT ON inquiry_file_tb FOR EACH ROW
WHEN (NEW.file_id IS NULL)
BEGIN SELECT inquiry_file_seq.NEXTVAL INTO :NEW.file_id FROM dual; END;
/

CREATE TABLE faq_tb (
    faq_id      NUMBER NOT NULL,
    category    VARCHAR2(50) NOT NULL,
    question    VARCHAR2(300) NOT NULL,
    answer      CLOB NOT NULL,
    sort_order  NUMBER DEFAULT 0 NOT NULL,
    use_yn      CHAR(1) DEFAULT 'Y' NOT NULL,
    created_at  DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_faq PRIMARY KEY (faq_id),
    CONSTRAINT ck_faq_use CHECK (use_yn IN ('Y','N'))
);
CREATE SEQUENCE seq_faq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_faq_bi BEFORE INSERT ON faq_tb FOR EACH ROW
WHEN (NEW.faq_id IS NULL)
BEGIN SELECT seq_faq.NEXTVAL INTO :NEW.faq_id FROM dual; END;
/

CREATE TABLE notification_tb (
    notification_id   NUMBER NOT NULL,
    id                VARCHAR2(20) NOT NULL,
    notification_type VARCHAR2(20) NOT NULL,
    title             VARCHAR2(200) NOT NULL,
    message           VARCHAR2(1000) NOT NULL,
    link_url          VARCHAR2(500),
    read_yn           CHAR(1) DEFAULT 'N' NOT NULL,
    star_yn           CHAR(1) DEFAULT 'N' NOT NULL,
    created_at        DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_notification PRIMARY KEY (notification_id),
    CONSTRAINT fk_notification_member FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT ck_notification_type CHECK (notification_type IN ('HOME','PERSONAL','OTT','CHAT','PAYMENT_FAIL','PAYMENT_DUE','SIGNUP','SETTLEMENT_REQUEST','SETTLEMENT_DONE','ROOM_FULL','ROOM_LEAVE_KICK','CARD_EXPIRING','REFUND_DONE','INQUIRY_REPLY','EXPENSE_DUE')),
    CONSTRAINT ck_notification_read CHECK (read_yn IN ('Y','N')),
    CONSTRAINT ck_notification_star CHECK (star_yn IN ('Y','N'))
);
CREATE SEQUENCE seq_notification START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_notification_bi BEFORE INSERT ON notification_tb FOR EACH ROW
WHEN (NEW.notification_id IS NULL)
BEGIN SELECT seq_notification.NEXTVAL INTO :NEW.notification_id FROM dual; END;
/
CREATE INDEX idx_notification_read ON notification_tb(id, read_yn, created_at DESC);
CREATE INDEX idx_notification_type ON notification_tb(id, notification_type, created_at DESC);

CREATE TABLE notice_read_tb (
    notice_id NUMBER NOT NULL,
    id        VARCHAR2(20) NOT NULL,
    read_at   DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_notice_read PRIMARY KEY (notice_id, id),
    CONSTRAINT fk_notice_read_notice FOREIGN KEY (notice_id) REFERENCES notice_tb(notice_id) ON DELETE CASCADE,
    CONSTRAINT fk_notice_read_member FOREIGN KEY (id) REFERENCES member_tb(id) ON DELETE CASCADE
);

CREATE TABLE notice_favorite_tb (
    notice_id  NUMBER NOT NULL,
    id         VARCHAR2(20) NOT NULL,
    created_at DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_notice_favorite PRIMARY KEY (notice_id, id),
    CONSTRAINT fk_notice_favorite_notice FOREIGN KEY (notice_id) REFERENCES notice_tb(notice_id) ON DELETE CASCADE,
    CONSTRAINT fk_notice_favorite_member FOREIGN KEY (id) REFERENCES member_tb(id) ON DELETE CASCADE
);

PROMPT [6/8] 신고/경고 테이블 생성

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
    CONSTRAINT uk_report_duplicate UNIQUE (reporter_id, room_id, report_reason),
    CONSTRAINT ck_report_status CHECK (report_status IN ('WAIT','PROCESSING','COMPLETE','REJECT'))
);
CREATE SEQUENCE seq_report START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_report_bi BEFORE INSERT ON report_tb FOR EACH ROW
WHEN (NEW.report_id IS NULL)
BEGIN SELECT seq_report.NEXTVAL INTO :NEW.report_id FROM dual; END;
/
CREATE INDEX idx_report_reported ON report_tb(reported_member_id, report_status);

CREATE TABLE warning_tb (
    warning_id     NUMBER NOT NULL,
    member_id      VARCHAR2(20) NOT NULL,
    report_id      NUMBER,
    warning_reason VARCHAR2(500) NOT NULL,
    penalty_days   NUMBER,
    status         CHAR(1) DEFAULT 'N' NOT NULL,
    created_at     DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_warning PRIMARY KEY (warning_id),
    CONSTRAINT fk_warning_member FOREIGN KEY (member_id) REFERENCES member_tb(id),
    CONSTRAINT fk_warning_report FOREIGN KEY (report_id) REFERENCES report_tb(report_id),
    CONSTRAINT ck_warning_status CHECK (status IN ('Y','N'))
);
CREATE SEQUENCE seq_warning START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE OR REPLACE TRIGGER trg_warning_bi BEFORE INSERT ON warning_tb FOR EACH ROW
WHEN (NEW.warning_id IS NULL)
BEGIN SELECT seq_warning.NEXTVAL INTO :NEW.warning_id FROM dual; END;
/
CREATE INDEX idx_warning_member ON warning_tb(member_id, created_at);

PROMPT [7/8] 운영 기본 데이터 입력

/* 지출 카테고리: 테스트 데이터가 아니라 화면 등록에 필요한 기준 데이터 */
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('월세','FIXED',1);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('통신비','FIXED',2);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('보험료','FIXED',3);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('관리비','FIXED',4);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('공과금','FIXED',5);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('식비','VARIABLE',1);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('교통비','VARIABLE',2);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('생활비','VARIABLE',3);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('쇼핑','VARIABLE',4);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('문화, 취미생활','VARIABLE',5);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('기타','VARIABLE',99);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Netflix','OTT',1);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Disney+','OTT',2);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('TVING','OTT',3);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Wavve','OTT',4);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Watcha','OTT',5);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Laftel','OTT',6);
INSERT INTO expense_category_tb(category_name, expense_type, sort_order) VALUES ('Coupang Play','OTT',7);

INSERT INTO ott_service_tb(service_name,default_price,fixed_plan_name,base_price,extra_member_fee,extra_member_count,max_member_limit,platform_fee_rate,share_yn,risk_level,block_reason) VALUES ('Netflix',27000,'프리미엄',17000,5000,2,4,3,'Y','LOW',NULL);
INSERT INTO ott_service_tb(service_name,default_price,fixed_plan_name,base_price,extra_member_fee,extra_member_count,max_member_limit,platform_fee_rate,share_yn,risk_level,block_reason) VALUES ('TVING',22000,'프리미엄',17000,5000,1,4,3,'Y','LOW',NULL);
INSERT INTO ott_service_tb(service_name,default_price,fixed_plan_name,base_price,extra_member_fee,extra_member_count,max_member_limit,platform_fee_rate,share_yn,risk_level,block_reason) VALUES ('Wavve',13900,'프리미엄',13900,0,0,4,3,'Y','LOW',NULL);
INSERT INTO ott_service_tb(service_name,default_price,fixed_plan_name,base_price,extra_member_fee,extra_member_count,max_member_limit,platform_fee_rate,share_yn,risk_level,block_reason) VALUES ('Watcha',12900,'프리미엄',12900,0,0,4,3,'Y','LOW',NULL);
INSERT INTO ott_service_tb(service_name,default_price,fixed_plan_name,base_price,extra_member_fee,extra_member_count,max_member_limit,platform_fee_rate,share_yn,risk_level,block_reason) VALUES ('Disney+',17900,'프리미엄',13900,4000,1,4,3,'Y','LOW',NULL);
INSERT INTO ott_service_tb(service_name,default_price,fixed_plan_name,base_price,extra_member_fee,extra_member_count,max_member_limit,platform_fee_rate,share_yn,risk_level,block_reason) VALUES ('Laftel',14900,'프리미엄',14900,0,0,4,3,'Y','LOW',NULL);
INSERT INTO ott_service_tb(service_name,default_price,fixed_plan_name,base_price,extra_member_fee,extra_member_count,max_member_limit,platform_fee_rate,share_yn,risk_level,block_reason) VALUES ('쿠팡플레이',7890,'단일 멤버십',7890,0,0,1,3,'N','HIGH','쿠팡 계정과 주문정보가 연결될 수 있어 공유를 제한합니다.');
INSERT INTO ott_service_tb(service_name,default_price,fixed_plan_name,base_price,extra_member_fee,extra_member_count,max_member_limit,platform_fee_rate,share_yn,risk_level,block_reason) VALUES ('애플TV+',6500,'단일 멤버십',6500,0,0,1,3,'N','HIGH','Apple ID 직접 공유 위험으로 공유를 제한합니다.');

COMMIT;

PROMPT [8/8] 생성 결과 확인
SELECT COUNT(*) AS created_table_count FROM user_tables WHERE table_name IN (
'MEMBER_TB','MEMBER_ACCOUNT_TB','MEMBER_CARD_TB','MEMBER_TRAN_TB','EXPENSE_CATEGORY_TB','EXPENSE_TB','MONTHLY_BUDGET_TB',
'OTT_SERVICE_TB','OTT_ROOM_TB','OTT_ROOM_MEMBER_TB','OTT_CHAT_MESSAGE_TB','OTT_CHAT_READ_TB','SETTLEMENT_TB','SETTLEMENT_PAYMENT_TB',
'SETTLEMENT_REFUND_TB','ESCROW_PAYOUT_TB','PLATFORM_REVENUE_TB','SELLER_ACCOUNT_TB','NOTICE_TB','INQUIRY_TB','INQUIRY_FILE_TB',
'FAQ_TB','NOTIFICATION_TB','NOTICE_READ_TB','NOTICE_FAVORITE_TB','REPORT_TB','WARNING_TB');
PROMPT 완료: CREATED_TABLE_COUNT가 27이면 전체 테이블이 생성된 것입니다.
