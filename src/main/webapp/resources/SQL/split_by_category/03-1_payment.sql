/* =========================================================
   SpendOlive 03-1_payment_fixed_no_card_fk.sql
   ---------------------------------------------------------
   실행 전 필수:
   1) member_tb 존재
   2) settlement_tb 존재
      → settlement_tb는 03_ott_schema_fixed.sql 실행 시 생성됨

   핵심 수정:
   - 기존 오류 원인인 card_number / card_company FK 제거
   - 현재 Java 코드가 쓰는 id, card_number, card_company, paymentKey, orderId 컬럼 유지
   ========================================================= */

SET DEFINE OFF;

/* 기존 결제 객체 전체 초기화는 00_reset_all_objects.sql에서 수행합니다. */

/* =========================================================
   1. 팀원별 결제 상태 테이블
   ========================================================= */
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
    expired_at      DATE, --??
    cancelled_at    DATE,
    paymentKey      VARCHAR2(100),
    orderId         VARCHAR2(100),
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
        payment_status IN ('UNPAID','PAID','CONFIRMED','EXPIRED','CANCELLED','REFUND_REQUESTED','REFUNDED')
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
   2. 환불 기록 테이블
   ========================================================= */
CREATE TABLE settlement_refund_tb (
    refund_id       NUMBER NOT NULL,
    payment_id      NUMBER NOT NULL,
    settlement_id   NUMBER NOT NULL,
    member_login_id VARCHAR2(20) NOT NULL,
    refund_amount   NUMBER NOT NULL,
    refund_reason   VARCHAR2(30) DEFAULT 'ROOM_CLOSE' ,
    refund_status   VARCHAR2(30) DEFAULT 'REQUESTED' ,
    requested_at    DATE DEFAULT SYSDATE NOT NULL,
    completed_at    DATE,
    memo            VARCHAR2(500),

    CONSTRAINT pk_settlement_refund PRIMARY KEY (refund_id),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES settlement_payment_tb(payment_id),
    CONSTRAINT fk_refund_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_tb(settlement_id),
    CONSTRAINT fk_refund_member FOREIGN KEY (member_login_id) REFERENCES member_tb(id),
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

/* 환불 조회 인덱스
   - settlement_refund_tb에는 room_id 컬럼이 없으므로 room_id 인덱스를 만들지 않는다.
   - 동일 인덱스명 중복 생성문을 제거한다. */
CREATE INDEX idx_refund_payment ON settlement_refund_tb(payment_id);
CREATE INDEX idx_refund_member_login ON settlement_refund_tb(member_login_id, refund_status);

/* =========================================================
   3. 통합 정산 금고 테이블
   ========================================================= */
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
    CONSTRAINT ck_ep_status CHECK (status IN ('HELD', 'RELEASED', 'REFUNDED', 'CANCELLED'))
);

CREATE SEQUENCE seq_escrow_payout START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_escrow_payout_bi
BEFORE INSERT ON escrow_payout_tb
FOR EACH ROW
WHEN (NEW.escrow_payout_id IS NULL)
BEGIN
    SELECT seq_escrow_payout.NEXTVAL INTO :NEW.escrow_payout_id FROM dual;
END;
/

CREATE INDEX idx_ep_settlement ON escrow_payout_tb(settlement_id);
CREATE INDEX idx_ep_host_status ON escrow_payout_tb(host_id, status);

/* =========================================================
   4. 플랫폼 수익 테이블
   ========================================================= */
CREATE TABLE platform_revenue_tb (
    revenue_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    settlement_id    NUMBER NOT NULL,
    room_id          NUMBER NOT NULL,
    payer_id         VARCHAR2(20) NOT NULL,
    base_amount      NUMBER NOT NULL,
    fee_rate         NUMBER(5, 2) DEFAULT 3.00,
    fee_amount       NUMBER NOT NULL,
    status           VARCHAR2(20) DEFAULT 'EARNED',
    created_at       DATE DEFAULT SYSDATE,

    CONSTRAINT fk_revenue_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT ck_revenue_status CHECK (status IN ('EARNED', 'REFUNDED', 'CANCELLED')),
    CONSTRAINT fk_revenue_payer FOREIGN KEY (payer_id) REFERENCES member_tb(id)
);



/* =========================================================
   5. 판매자 계좌 테이블
   ========================================================= */
CREATE TABLE seller_account_tb (
    seller_idx       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id        VARCHAR2(20) NOT NULL,
    bank_name        VARCHAR2(50) NOT NULL,
    account_number   VARCHAR2(30) NOT NULL,
    traceId          VARCHAR2(100) NOT NULL,
    reg_date         DATE DEFAULT SYSDATE,

    CONSTRAINT fk_seller_member_id FOREIGN KEY (member_id)
    REFERENCES member_tb(id) ON DELETE CASCADE
);

/* =========================================================
   6. 생성 확인
   ========================================================= */
SELECT table_name
FROM user_tables
WHERE table_name IN (
    'SETTLEMENT_PAYMENT_TB',
    'SETTLEMENT_REFUND_TB',
    'ESCROW_PAYOUT_TB',
    'PLATFORM_REVENUE_TB',
    'SELLER_ACCOUNT_TB'
)
ORDER BY table_name;