/* ============================================================================
   SpendOlive 전체 DB 삭제 스크립트
   - Oracle XE 21c / SQL Developer F5(스크립트 실행) 기준
   - SpendOlive에서 사용하는 객체만 삭제하며 다른 사용자 테이블은 건드리지 않음
   - 테이블 삭제 시 연결된 인덱스와 트리거도 함께 제거됨
   ============================================================================ */

SET SERVEROUTPUT ON;
SET DEFINE OFF;

PROMPT SpendOlive 테이블 삭제 시작

DECLARE
    TYPE name_list IS TABLE OF VARCHAR2(128);
    v_tables name_list := name_list(
        'WARNING_TB','REPORT_TB',
        'NOTICE_FAVORITE_TB','NOTICE_READ_TB','NOTIFICATION_TB','FAQ_TB','INQUIRY_FILE_TB','INQUIRY_TB','NOTICE_TB',
        'SELLER_ACCOUNT_TB','PLATFORM_REVENUE_TB','ESCROW_PAYOUT_TB','SETTLEMENT_REFUND_TB','SETTLEMENT_PAYMENT_TB',
        'OTT_CHAT_READ_TB','OTT_CHAT_MESSAGE_TB','OTT_ROOM_MEMBER_TB','SETTLEMENT_TB','OTT_ROOM_TB','OTT_SERVICE_TB',
        'MONTHLY_BUDGET_TB','EXPENSE_TB','EXPENSE_CATEGORY_TB',
        'MEMBER_TRAN_TB','MEMBER_CARD_TB','MEMBER_ACCOUNT_TB','MEMBER_TB'
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

PROMPT SpendOlive 시퀀스 삭제 시작

DECLARE
    TYPE name_list IS TABLE OF VARCHAR2(128);
    v_sequences name_list := name_list(
        'SEQ_WARNING','SEQ_REPORT','SEQ_NOTIFICATION','SEQ_FAQ','INQUIRY_FILE_SEQ','INQUIRY_SEQ','SEQ_NOTICE',
        'SEQ_SELLER_ACCOUNT','SEQ_PLATFORM_REVENUE','SEQ_ESCROW_PAYOUT','SEQ_SETTLEMENT_REFUND','SEQ_SETTLEMENT_PAYMENT',
        'SEQ_SETTLEMENT','SEQ_OTT_CHAT_MESSAGE','SEQ_OTT_ROOM_MEMBER','SEQ_OTT_ROOM','SEQ_OTT_SERVICE',
        'SEQ_MONTHLY_BUDGET','SEQ_EXPENSE','SEQ_EXPENSE_CATEGORY',
        'SEQ_MEMBER_TRAN','SEQ_MEMBER_CARD','SEQ_MEMBER_ACCOUNT','SEQ_MEMBER'
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

SELECT COUNT(*) AS remaining_spendolive_tables
FROM user_tables
WHERE table_name IN (
'MEMBER_TB','MEMBER_ACCOUNT_TB','MEMBER_CARD_TB','MEMBER_TRAN_TB','EXPENSE_CATEGORY_TB','EXPENSE_TB','MONTHLY_BUDGET_TB',
'OTT_SERVICE_TB','OTT_ROOM_TB','OTT_ROOM_MEMBER_TB','OTT_CHAT_MESSAGE_TB','OTT_CHAT_READ_TB','SETTLEMENT_TB','SETTLEMENT_PAYMENT_TB',
'SETTLEMENT_REFUND_TB','ESCROW_PAYOUT_TB','PLATFORM_REVENUE_TB','SELLER_ACCOUNT_TB','NOTICE_TB','INQUIRY_TB','INQUIRY_FILE_TB',
'FAQ_TB','NOTIFICATION_TB','NOTICE_READ_TB','NOTICE_FAVORITE_TB','REPORT_TB','WARNING_TB');

PROMPT 완료: REMAINING_SPENDOLIVE_TABLES가 0이면 전체 삭제된 것입니다.
