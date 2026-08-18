-- ============================================================
-- 대상: 공지사항 / FAQ / 문의하기(+첨부파일) / 알림 / 공지 읽음·찜
-- ============================================================
-- 0. 기존 객체 정리 (없으면 에러 무시하고 통과, 자식 테이블부터 삭제)
-- ============================================================

BEGIN EXECUTE IMMEDIATE 'DROP TABLE inquiry_file_tb CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE inquiry_file_seq';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP TABLE inquiry_tb CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE inquiry_seq';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;
/

-- 답변 전용 테이블 (04번 원본에만 있던 것, 안 씀)
BEGIN EXECUTE IMMEDIATE 'DROP TABLE inquiry_answer_tb CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_inquiry_answer';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_inquiry';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP TABLE faq_tb CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_faq';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP TABLE notification_tb CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_notification';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP TABLE alert_tb CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_alert';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE notice_bookmark_tb CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_notice_bookmark';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;
/

-- notice_read_tb/notice_favorite_tb를 먼저 지워야 notice_tb를 지울 때 FK 걸림 없음
BEGIN EXECUTE IMMEDIATE 'DROP TABLE notice_favorite_tb CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE notice_read_tb CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP TABLE notice_tb CASCADE CONSTRAINTS PURGE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE seq_notice';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF; END;
/


-- ============================================================
-- 1. 공지사항 (notice_tb)
-- ============================================================

    CREATE TABLE notice_tb (
        notice_id   NUMBER          NOT NULL,
        admin_id    VARCHAR2(20)    NOT NULL,
        title       VARCHAR2(200)   NOT NULL,
        content     CLOB            NOT NULL,
        pinned_yn   CHAR(1)         DEFAULT 'N' NOT NULL,
        created_at  DATE            DEFAULT SYSDATE NOT NULL,
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


-- ============================================================
-- 2. 문의하기 (inquiry_tb) — 지금까지 만든 코드 기준으로 확정
--    InquiryVO / InquiryRepository / InquiryService / InquiryController 전부 이 스키마 기준
-- ============================================================

CREATE SEQUENCE inquiry_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE;

CREATE TABLE inquiry_tb (
    inquiry_id      NUMBER          NOT NULL,
    id              VARCHAR2(50)    NOT NULL,   -- member_tb.id 참조 (작성자)
    category        VARCHAR2(30)    NOT NULL,   -- 계정·로그인 / 지출관리 / OTT관리 / 캘린더 / 공지·알림 / 결제·정산 / 기타
    inquiry_type    VARCHAR2(30)    NOT NULL,   -- 오류/버그 신고 / 기능 개선 제안 / 사용 방법 문의 / 기타 문의
    title           VARCHAR2(50 CHAR)   NOT NULL,
    content         VARCHAR2(1000 CHAR) NOT NULL,
    status          VARCHAR2(10)    DEFAULT 'WAIT' NOT NULL,   -- WAIT / DONE / REVIEW
    reg_date        DATE            DEFAULT SYSDATE NOT NULL,
    reply_content   VARCHAR2(1000 CHAR),
    reply_date      DATE,

    CONSTRAINT pk_inquiry_tb PRIMARY KEY (inquiry_id),
    CONSTRAINT fk_inquiry_member FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT ck_inquiry_status CHECK (status IN ('WAIT','DONE','REVIEW'))
);

-- 첨부파일 (InquiryFileVO / InquiryFileRepository / FileStorageService 연동)
CREATE SEQUENCE inquiry_file_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE inquiry_file_tb (
    file_id         NUMBER          NOT NULL,
    inquiry_id      NUMBER          NOT NULL,
    origin_name     VARCHAR2(200)   NOT NULL,  -- 업로드 당시 원본 파일명
    saved_name      VARCHAR2(200)   NOT NULL,  -- 서버에 저장된 파일명 (UUID + 확장자)
    file_path       VARCHAR2(500)   NOT NULL,  -- 서버 디스크 실제 경로
    file_size       NUMBER,
    reg_date        DATE            DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_inquiry_file_tb PRIMARY KEY (file_id),
    CONSTRAINT fk_inquiry_file_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiry_tb(inquiry_id) ON DELETE CASCADE
);


-- ============================================================
-- 3. FAQ (faq_tb)
-- ============================================================

CREATE SEQUENCE seq_faq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE faq_tb (
    faq_id      NUMBER          NOT NULL,
    category    VARCHAR2(50)    NOT NULL,   -- faqList.jsp 카테고리 버튼과 맞춰서 값 사용 (account/expense/ott/notice/etc 등)
    question    VARCHAR2(300)   NOT NULL,
    answer      CLOB            NOT NULL,
    sort_order  NUMBER          DEFAULT 0 NOT NULL,  -- 관리자가 지정하는 노출 순서
    use_yn      CHAR(1)         DEFAULT 'Y' NOT NULL, -- 노출 여부 (관리자가 숨김 처리 가능)
    created_at  DATE            DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_faq PRIMARY KEY (faq_id),
    CONSTRAINT ck_faq_use CHECK (use_yn IN ('Y', 'N'))
);

CREATE OR REPLACE TRIGGER trg_faq_bi
BEFORE INSERT ON faq_tb
FOR EACH ROW
WHEN (NEW.faq_id IS NULL)
BEGIN
    SELECT seq_faq.NEXTVAL INTO :NEW.faq_id FROM dual;
END;
/


-- ============================================================
-- 4. 알림 (notification_tb)
-- ============================================================

CREATE SEQUENCE seq_notification START WITH 1 INCREMENT BY 1;

CREATE TABLE notification_tb (
    notification_id    NUMBER          NOT NULL,
    id                  VARCHAR2(50)    NOT NULL,

    notification_type   VARCHAR2(20)    NOT NULL,  -- HOME(공지) / CHAT / PAYMENT_FAIL / PAYMENT_DUE /
                                                     -- SIGNUP / SETTLEMENT_REQUEST / SETTLEMENT_DONE /
                                                     -- ROOM_FULL / ROOM_LEAVE_KICK / CARD_EXPIRING /
                                                     -- REFUND_DONE / INQUIRY_REPLY / EXPENSE_DUE
    title               VARCHAR2(200)   NOT NULL,
    message             VARCHAR2(1000)  NOT NULL,
    link_url            VARCHAR2(500),

    read_yn             CHAR(1)         DEFAULT 'N' NOT NULL,
    star_yn             CHAR(1)         DEFAULT 'N' NOT NULL,
    created_at          DATE            DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_notification PRIMARY KEY (notification_id),
    CONSTRAINT fk_notification_member_id FOREIGN KEY (id) REFERENCES member_tb(id),
    CONSTRAINT chk_notification_type CHECK (notification_type IN (
        'HOME', 'PERSONAL', 'OTT',
        'CHAT', 'PAYMENT_FAIL', 'PAYMENT_DUE', 'SIGNUP',
        'SETTLEMENT_REQUEST', 'SETTLEMENT_DONE',
        'ROOM_FULL', 'ROOM_LEAVE_KICK', 'CARD_EXPIRING',
        'REFUND_DONE', 'INQUIRY_REPLY', 'EXPENSE_DUE'
    )),
    CONSTRAINT chk_notification_read CHECK (read_yn IN ('Y', 'N')),
    CONSTRAINT chk_notification_star CHECK (star_yn IN ('Y', 'N'))
);

CREATE OR REPLACE TRIGGER trg_notification_bi
BEFORE INSERT ON notification_tb
FOR EACH ROW
BEGIN
    IF :NEW.notification_id IS NULL THEN
        SELECT seq_notification.NEXTVAL INTO :NEW.notification_id FROM dual;
    END IF;
END;
/

CREATE INDEX idx_notification_id_read ON notification_tb(id, read_yn, created_at DESC);
CREATE INDEX idx_notification_id_type ON notification_tb(id, notification_type, created_at DESC);


-- ============================================================
-- 5. 공지 읽음 / 찜 (notice_tb는 1번 섹션에서 이미 생성됨)
-- ============================================================

CREATE TABLE notice_read_tb (
    notice_id   NUMBER          NOT NULL,
    id          VARCHAR2(50)    NOT NULL,
    read_at     DATE            DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_notice_read PRIMARY KEY (notice_id, id),
    CONSTRAINT fk_notice_read_notice FOREIGN KEY (notice_id) REFERENCES notice_tb(notice_id),
    CONSTRAINT fk_notice_read_member FOREIGN KEY (id) REFERENCES member_tb(id)
);

CREATE TABLE notice_favorite_tb (
    notice_id   NUMBER          NOT NULL,
    id          VARCHAR2(50)    NOT NULL,
    created_at  DATE            DEFAULT SYSDATE NOT NULL,

    CONSTRAINT pk_notice_favorite PRIMARY KEY (notice_id, id),
    CONSTRAINT fk_notice_favorite_notice FOREIGN KEY (notice_id) REFERENCES notice_tb(notice_id),
    CONSTRAINT fk_notice_favorite_member FOREIGN KEY (id) REFERENCES member_tb(id)
);


-- ============================================================
-- 6. 샘플 데이터 (문의하기) — member_tb에 등록된 첫 번째 회원 id로 삽입
-- ============================================================

DECLARE
    v_id member_tb.id%TYPE;
BEGIN
    SELECT id INTO v_id FROM member_tb WHERE ROWNUM = 1;

    INSERT INTO inquiry_tb (inquiry_id, id, category, inquiry_type, title, content, status, reg_date, reply_content, reply_date)
    VALUES (inquiry_seq.NEXTVAL, v_id, '지출관리', '오류/버그 신고',
            '고정지출 등록 후 캘린더에 반영이 안 되어요',
            '고정지출을 새로 등록했는데 캘린더 뷰에서 해당 날짜에 표시가 되지 않습니다. 확인 부탁드립니다.',
            'DONE', TO_DATE('2026.06.20','YYYY.MM.DD'),
            '확인 결과 고정지출 등록 시 캘린더 이벤트 생성 로직에 누락이 있었습니다. 다음 배포에서 수정될 예정입니다. 불편을 드려 죄송합니다.',
            TO_DATE('2026.06.21','YYYY.MM.DD'));

    INSERT INTO inquiry_tb (inquiry_id, id, category, inquiry_type, title, content, status, reg_date)
    VALUES (inquiry_seq.NEXTVAL, v_id, 'OTT관리', '기능 개선 제안',
            'OTT 방장 양도 기능이 필요합니다',
            '방장이 탈퇴하거나 비활성 상태가 될 경우 멤버 중 한 명에게 방장 권한을 넘길 수 있으면 좋겠습니다.',
            'REVIEW', TO_DATE('2026.06.24','YYYY.MM.DD'));

    INSERT INTO inquiry_tb (inquiry_id, id, category, inquiry_type, title, content, status, reg_date)
    VALUES (inquiry_seq.NEXTVAL, v_id, '계정·로그인', '오류/버그 신고',
            '카카오 로그인 연동 후 알림 수신이 안 됩니다',
            '카카오 소셜 로그인으로 전환 후 벨 알림이 전혀 오지 않습니다. 이메일 기반 계정이었을 때는 잘 왔었는데 원인을 알고 싶습니다.',
            'WAIT', TO_DATE('2026.06.27','YYYY.MM.DD'));

    INSERT INTO inquiry_tb (inquiry_id, id, category, inquiry_type, title, content, status, reg_date)
    VALUES (inquiry_seq.NEXTVAL, v_id, '결제·정산', '사용 방법 문의',
            '정산 요청을 취소하고 싶어요',
            '실수로 정산 요청을 보냈는데 취소하는 방법을 모르겠습니다. 상대방이 아직 승인 전인데 취소가 가능한가요?',
            'WAIT', TO_DATE('2026.07.01','YYYY.MM.DD'));

    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('member_tb에 회원 데이터가 없어 샘플 문의 데이터는 넣지 않았습니다. 회원가입 후 다시 실행해 주세요.');
END;
/



-- ============================================================
-- 7. 샘플 데이터 (자주 묻는 질문)
-- ============================================================

-- 계정·로그인
INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('account', '오픈뱅킹 계좌 연동은 꼭 해야 하나요?',
 '오픈뱅킹 계좌 연동은 OTT 공유방 입장, 정산금 이체, 자동결제(빌링) 등 실제 돈이 오가는 기능을 이용하기 위한 안전장치예요. 연동하지 않아도 서비스 둘러보기는 가능하지만, 방 참여나 정산 기능은 계좌 연동 후에 이용할 수 있어요.', 1, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('account', '카카오 로그인과 이메일 로그인을 같이 쓸 수 있나요?',
 '하나의 계정은 하나의 로그인 방식(이메일 또는 카카오)으로 관리돼요. 로그인 방식을 변경하고 싶다면 마이페이지에서 계정 설정을 확인해 주세요.', 2, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('account', '카드 등록은 어떻게 하나요?',
 '마이페이지 > 카드 등록 메뉴에서 진행할 수 있어요. 등록 시 입력하신 카드 정보는 저희 서버가 아니라 Toss Payments 결제창에서 직접 처리되며, 저희는 결제에 필요한 일회성 인증키만 전달받아요.', 3, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('account', '비밀번호를 잊어버렸어요. 어떻게 재설정하나요?',
 '로그인 화면의 비밀번호 찾기 메뉴에서 가입 시 등록한 이메일로 재설정 링크를 받을 수 있어요. 카카오 로그인으로 가입하신 경우 별도 비밀번호가 없으니 카카오 계정 설정에서 확인해 주세요.', 4, 'Y');

-- 지출관리
INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('expense', '고정지출과 변동지출의 차이가 뭔가요?',
 '고정지출은 월세, 통신비처럼 매달 반복되는 지출이고, 변동지출은 식비, 쇼핑처럼 금액과 시기가 달라지는 지출이에요. 고정지출로 등록하면 매달 캘린더에 자동으로 반영돼요.', 1, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('expense', '캘린더에 지출 내역이 안 보여요.',
 '지출 등록 시 날짜가 정확히 입력되었는지 확인해 주세요. 고정지출은 등록한 날짜를 기준으로 매달 같은 날짜에 자동 반영되며, 반영까지 새로고침이 필요할 수 있어요.', 2, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('expense', '지출 카테고리를 직접 추가할 수 있나요?',
 '현재는 기본 제공되는 카테고리(월세, 식비, 교통비 등) 내에서 지출을 등록할 수 있어요. 원하는 카테고리가 없다면 문의하기를 통해 추가 요청을 남겨주세요.', 3, 'Y');

-- OTT관리
INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('ott', '가족 공유방과 외부 모집방은 뭐가 다른가요?',
 '가족 공유방(FRIEND)은 이미 아는 사람끼리 초대해서 만드는 방이고, 외부 모집방(RECRUIT)은 낯선 사람도 참여 신청을 통해 합류할 수 있는 방이에요. 두 모드 모두 정산 방식은 동일해요.', 1, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('ott', '방장이 나가면 방은 어떻게 되나요?',
 '방장이 탈퇴하거나 활동을 중단하면 다른 멤버에게 방장 권한을 넘기는 기능을 준비 중이에요. 현재는 방장 탈퇴 시 고객센터로 문의해 주시면 처리를 도와드려요.', 2, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('ott', '정산 금액은 어떻게 계산되나요?',
 '전체 구독료를 참여 인원수로 나눈 금액(N분의 1)에 플랫폼 이용 수수료 3%를 더해서 각 멤버에게 청구돼요. 정산 내역은 방 상세 화면에서 실시간으로 확인할 수 있어요.', 3, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('ott', '원하는 OTT 서비스가 목록에 없어요.',
 '현재 Netflix, Disney+, TVING, Wavve, Watcha, Laftel, Coupang Play를 지원하고 있어요. 추가를 원하는 서비스가 있다면 문의하기로 알려주세요.', 4, 'Y');

-- 공지·알림
INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('notice', '알림은 어디서 확인하나요?',
 '상단 종 모양 아이콘을 누르면 알림 센터로 이동해요. 결제, 정산, 공지사항 관련 알림을 한 곳에서 확인할 수 있고, 읽지 않은 알림은 빨간 배지로 표시돼요.', 1, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('notice', '공지사항 찜(즐겨찾기) 기능은 어떻게 쓰나요?',
 '공지사항 상세 화면에서 별 아이콘을 누르면 찜 목록에 저장돼요. 나중에 다시 찾아보고 싶은 공지를 모아둘 때 유용해요.', 2, 'Y');

-- 기타
INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('etc', '문의하기와 신고하기는 어떻게 다른가요?',
 '문의하기는 서비스 이용 중 궁금한 점이나 오류를 알리는 용도이고, 신고하기는 다른 회원의 부적절한 행동(정산 미이행, 사기 등)을 알리는 용도예요. 신고 접수 시 별도로 검토 후 조치돼요.', 1, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('etc', '서비스 이용료가 따로 있나요?',
 '가입 및 기본 이용은 무료이며, OTT 공유방 정산 시에만 정산 금액의 3%가 플랫폼 수수료로 부과돼요.', 2, 'Y');

INSERT INTO faq_tb (category, question, answer, sort_order, use_yn)
VALUES ('etc', '회원 탈퇴는 어떻게 하나요?',
 '마이페이지 하단의 회원 탈퇴 메뉴에서 진행할 수 있어요. 진행 중인 정산이나 참여 중인 방이 있다면 정산 완료 후 탈퇴가 가능해요.', 3, 'Y');



-- ============================================================
-- 8. 샘플 데이터 (공지사항)
-- ============================================================

DECLARE
    v_admin_id member_tb.id%TYPE;
BEGIN
    SELECT id INTO v_admin_id
    FROM member_tb
    WHERE role = 'ADMIN'
      AND ROWNUM = 1;

    INSERT INTO notice_tb (notice_id, admin_id, title, content, pinned_yn, created_at)
    VALUES (seq_notice.NEXTVAL, v_admin_id,
        '[필독] SpendOlive 서비스 이용약관 개정 안내',
        '안녕하세요, SpendOlive입니다.' || CHR(10) ||
        '2026년 8월 20일부로 서비스 이용약관 및 개인정보처리방침이 일부 개정될 예정입니다.' || CHR(10) ||
        '주요 변경사항은 OTT 공유방 정산 수수료 정책 명확화 및 오픈뱅킹 연동 관련 조항 추가입니다.' || CHR(10) ||
        '자세한 내용은 마이페이지 > 약관 메뉴에서 확인하실 수 있습니다.',
        'Y', TO_DATE('2026.08.05','YYYY.MM.DD'));

    INSERT INTO notice_tb (notice_id, admin_id, title, content, pinned_yn, created_at)
    VALUES (seq_notice.NEXTVAL, v_admin_id,
        '추석 연휴 고객센터 운영 안내',
        '추석 연휴 기간 동안 고객센터 상담 운영이 일시 중단됩니다.' || CHR(10) ||
        '문의하기를 통해 남겨주신 내용은 연휴 종료 후 순차적으로 답변드릴 예정이니 양해 부탁드립니다.' || CHR(10) ||
        '서비스 자체는 연휴 기간에도 정상적으로 이용 가능합니다.',
        'Y', TO_DATE('2026.08.10','YYYY.MM.DD'));

    INSERT INTO notice_tb (notice_id, admin_id, title, content, pinned_yn, created_at)
    VALUES (seq_notice.NEXTVAL, v_admin_id,
        '캘린더 기능 업데이트 안내',
        '캘린더에서 반복되는 고정지출 표시 방식이 개선되었습니다.' || CHR(10) ||
        '이제 고정지출을 등록하면 매달 자동으로 해당 날짜에 반영되며, 팝오버를 통해 상세 내역을 바로 확인할 수 있습니다.' || CHR(10) ||
        '이용 중 불편사항이 있으시면 문의하기로 알려주세요.',
        'N', TO_DATE('2026.07.28','YYYY.MM.DD'));

    INSERT INTO notice_tb (notice_id, admin_id, title, content, pinned_yn, created_at)
    VALUES (seq_notice.NEXTVAL, v_admin_id,
        'OTT 지원 서비스 추가 안내 (Coupang Play)',
        '회원분들의 요청이 많았던 Coupang Play가 OTT 공유방 목록에 새롭게 추가되었습니다.' || CHR(10) ||
        '방 생성 시 OTT 종류 선택 화면에서 바로 이용하실 수 있습니다.',
        'N', TO_DATE('2026.07.15','YYYY.MM.DD'));

    INSERT INTO notice_tb (notice_id, admin_id, title, content, pinned_yn, created_at)
    VALUES (seq_notice.NEXTVAL, v_admin_id,
        '서버 정기 점검 안내 (8월 15일 새벽 2시~4시)',
        '보다 안정적인 서비스 제공을 위해 아래와 같이 정기 점검을 진행합니다.' || CHR(10) ||
        '점검 시간: 2026년 8월 15일 새벽 2시 ~ 4시 (약 2시간)' || CHR(10) ||
        '점검 시간 동안에는 로그인 및 결제 관련 기능 이용이 일시적으로 제한될 수 있습니다.' || CHR(10) ||
        '이용에 불편을 드려 죄송합니다.',
        'N', TO_DATE('2026.08.11','YYYY.MM.DD'));

    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('member_tb에 role=ADMIN인 회원이 없어 샘플 공지 데이터는 넣지 않았습니다. 관리자 계정 생성 후 다시 실행해 주세요.');
END;
/
