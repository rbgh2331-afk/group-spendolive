/* [고정지출 종료월] 이미 생성된 DB에 한 번만 실행하는 패치 SQL */
ALTER TABLE expense_tb ADD repeat_end_date DATE;
ALTER TABLE expense_tb ADD CONSTRAINT ck_expense_repeat_end_date CHECK (repeat_end_date IS NULL OR repeat_end_date >= expense_date);
COMMENT ON COLUMN expense_tb.repeat_end_date IS '고정지출 반복 종료일(선택한 종료월의 마지막 날짜)';
