/* =========================================================
   settlement_refund_tb room_id 제거 보정 패치
   - 최종 환불 스키마는 settlement_id로 방 정보를 추적한다.
   - 기존 DB에 과거 room_id 컬럼/FK/인덱스가 남아 있을 때만 제거한다.
   - 이미 제거된 DB에서 다시 실행해도 대상이 없으면 건너뛴다.
   ========================================================= */

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_indexes
     WHERE index_name = 'IDX_REFUND_ROOM';

    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'DROP INDEX idx_refund_room';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_constraints
     WHERE constraint_name = 'FK_REFUND_ROOM'
       AND table_name = 'SETTLEMENT_REFUND_TB';

    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE settlement_refund_tb DROP CONSTRAINT fk_refund_room';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'SETTLEMENT_REFUND_TB'
       AND column_name = 'ROOM_ID';

    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE settlement_refund_tb DROP COLUMN room_id';
    END IF;
END;
/
