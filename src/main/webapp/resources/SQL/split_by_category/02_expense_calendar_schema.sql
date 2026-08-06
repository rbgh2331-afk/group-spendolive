/* =========================================================
   02. 지출/캘린더 SQL
   =========================================================
   실행 안내: 01_member_schema.sql 실행 후 실행. 지출관리/캘린더 기능에 필요.
   ========================================================= */

SET DEFINE OFF;

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
    repeat_end_date DATE,                                 -- [고정지출 종료월] 선택한 종료월의 마지막 날짜
    fixed_yn       CHAR(1) DEFAULT 'N' NOT NULL,
    created_at     DATE DEFAULT SYSDATE NOT NULL,
    updated_at     DATE,

    CONSTRAINT pk_expense PRIMARY KEY (expense_id),
    CONSTRAINT fk_expense_member FOREIGN KEY (member_id) REFERENCES member_tb(member_id),
    CONSTRAINT fk_expense_category FOREIGN KEY (category_id) REFERENCES expense_category_tb(category_id),
    CONSTRAINT ck_expense_amount CHECK (amount >= 0),
    CONSTRAINT ck_expense_repeat_yn CHECK (repeat_yn IN ('Y', 'N')),
    CONSTRAINT ck_expense_fixed_yn CHECK (fixed_yn IN ('Y', 'N')),
    CONSTRAINT ck_expense_repeat_cycle CHECK (repeat_cycle IS NULL OR repeat_cycle IN ('MONTHLY', 'WEEKLY', 'YEARLY')),
    CONSTRAINT ck_expense_repeat_end_date CHECK (repeat_end_date IS NULL OR repeat_end_date >= expense_date)
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

-- [고정지출 종료월] NULL이면 무기한 반복, 값이 있으면 해당 날짜까지만 반복한다.
COMMENT ON COLUMN expense_tb.repeat_end_date IS '고정지출 반복 종료일(선택한 종료월의 마지막 날짜)';

/* =========================================================
   4. 회원별 월간 예산 테이블
   - 회원이 연월별로 설정한 예산을 저장
   - budget_month는 YYYY-MM 형식으로 저장 (예: 2026-07)
   - created_at, updated_at은 화면에 표시하지 않고 변경 이력 확인용으로만 보관
   ========================================================= */
    CREATE TABLE monthly_budget_tb (
        budget_id      NUMBER NOT NULL,                     -- 월별 예산 고유번호
        member_id      NUMBER NOT NULL,                     -- 예산을 설정한 회원 고유번호
        budget_month   VARCHAR2(7) NOT NULL,                -- 예산 적용 연월 (YYYY-MM)
        budget_amount  NUMBER DEFAULT 0 NOT NULL,           -- 해당 월의 예산 금액
        created_at     DATE DEFAULT SYSDATE NOT NULL,       -- 최초 등록일
        updated_at     DATE DEFAULT SYSDATE NOT NULL,       -- 마지막 수정일

        CONSTRAINT pk_monthly_budget PRIMARY KEY (budget_id),
        CONSTRAINT fk_monthly_budget_member
            FOREIGN KEY (member_id)
            REFERENCES member_tb(member_id)
            ON DELETE CASCADE,
        CONSTRAINT uk_monthly_budget_member_month
            UNIQUE (member_id, budget_month),
        CONSTRAINT ck_monthly_budget_amount
            CHECK (budget_amount >= 0),
        CONSTRAINT ck_monthly_budget_month
            CHECK (REGEXP_LIKE(budget_month, '^[0-9]{4}-(0[1-9]|1[0-2])$'))
    );

CREATE SEQUENCE seq_monthly_budget START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_monthly_budget_bi
BEFORE INSERT ON monthly_budget_tb
FOR EACH ROW
WHEN (NEW.budget_id IS NULL)
BEGIN
    SELECT seq_monthly_budget.NEXTVAL INTO :NEW.budget_id FROM dual;
END;
/

COMMENT ON TABLE monthly_budget_tb IS '회원별 월간 예산 저장 테이블';
COMMENT ON COLUMN monthly_budget_tb.budget_id IS '월별 예산 고유번호';
COMMENT ON COLUMN monthly_budget_tb.member_id IS '예산을 설정한 회원 고유번호';
COMMENT ON COLUMN monthly_budget_tb.budget_month IS '예산 적용 연월(YYYY-MM)';
COMMENT ON COLUMN monthly_budget_tb.budget_amount IS '해당 월의 예산 금액';
COMMENT ON COLUMN monthly_budget_tb.created_at IS '예산 최초 등록일';
COMMENT ON COLUMN monthly_budget_tb.updated_at IS '예산 마지막 수정일';

