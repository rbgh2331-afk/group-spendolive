/* =========================================================
   00. SpendOlive 전체 객체 초기화
   ---------------------------------------------------------
   목적  : 새 DB를 다시 구성하기 전에 SpendOlive 객체만 삭제한다.
   주의  : 프로젝트 데이터가 모두 삭제되므로 기존 DB 보존 시 실행하지 않는다.
   규칙  : 이 파일에는 DROP만 둔다. CREATE / ALTER / INSERT는 넣지 않는다.
   실행  : SQL Developer에서 F5(스크립트 실행)
   ========================================================= */
SET SERVEROUTPUT ON;
SET DEFINE OFF;

PROMPT [SpendOlive] 프로젝트 테이블 삭제 시작

DECLARE
    TYPE name_list IS TABLE OF VARCHAR2(128);
    v_tables name_list := name_list(
        'WARNING_TB', 'REPORT_TB',
        'NOTICE_BOOKMARK_TB', 'NOTICE_FAVORITE_TB', 'NOTICE_READ_TB',
        'ALERT_TB', 'NOTIFICATION_TB', 'FAQ_TB', 'INQUIRY_ANSWER_TB', 'INQUIRY_FILE_TB', 'INQUIRY_TB', 'NOTICE_TB',
        'SELLER_ACCOUNT_TB', 'PLATFORM_REVENUE_TB', 'ESCROW_PAYOUT_TB', 'SETTLEMENT_REFUND_TB', 'SETTLEMENT_PAYMENT_TB',
        'OTT_CHAT_READ_TB', 'OTT_CHAT_MESSAGE_TB', 'OTT_ROOM_MEMBER_TB', 'SETTLEMENT_TB', 'OTT_ROOM_TB', 'OTT_SERVICE_TB',
        'MONTHLY_BUDGET_TB', 'EXPENSE_TB', 'EXPENSE_CATEGORY_TB',
        'MEMBER_TRAN_TB', 'MEMBER_CARD_TB', 'MEMBER_ACCOUNT_TB', 'MEMBER_TB'
    );
BEGIN
    FOR i IN 1 .. v_tables.COUNT LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP TABLE ' || v_tables(i) || ' CASCADE CONSTRAINTS PURGE';
            DBMS_OUTPUT.PUT_LINE('[TABLE DROP] ' || v_tables(i));
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE = -942 THEN
                    DBMS_OUTPUT.PUT_LINE('[TABLE SKIP] ' || v_tables(i) || ' 없음');
                ELSE
                    RAISE;
                END IF;
        END;
    END LOOP;
END;
/

PROMPT [SpendOlive] 프로젝트 시퀀스 삭제 시작

DECLARE
    TYPE name_list IS TABLE OF VARCHAR2(128);
    v_sequences name_list := name_list(
        'SEQ_REPORT', 'SEQ_ALERT', 'SEQ_NOTICE_BOOKMARK', 'SEQ_NOTIFICATION', 'SEQ_FAQ',
        'SEQ_INQUIRY_ANSWER', 'SEQ_INQUIRY', 'INQUIRY_FILE_SEQ', 'INQUIRY_SEQ', 'SEQ_NOTICE',
        'SEQ_ESCROW_PAYOUT', 'SEQ_SETTLEMENT_REFUND', 'SEQ_SETTLEMENT_PAYMENT',
        'SEQ_SETTLEMENT', 'SEQ_OTT_CHAT_MESSAGE', 'SEQ_OTT_ROOM_MEMBER', 'SEQ_OTT_ROOM', 'SEQ_OTT_SERVICE',
        'SEQ_MONTHLY_BUDGET', 'SEQ_EXPENSE', 'SEQ_EXPENSE_CATEGORY', 'SEQ_MEMBER'
    );
BEGIN
    FOR i IN 1 .. v_sequences.COUNT LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP SEQUENCE ' || v_sequences(i);
            DBMS_OUTPUT.PUT_LINE('[SEQUENCE DROP] ' || v_sequences(i));
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE = -2289 THEN
                    DBMS_OUTPUT.PUT_LINE('[SEQUENCE SKIP] ' || v_sequences(i) || ' 없음');
                ELSE
                    RAISE;
                END IF;
        END;
    END LOOP;
END;
/

PURGE RECYCLEBIN;
PROMPT [SpendOlive] 초기화 완료
