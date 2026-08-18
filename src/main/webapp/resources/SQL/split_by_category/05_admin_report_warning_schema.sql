/* =========================================================
   05. 신고/경고 관리자 SQL
   =========================================================
   실행 안내: 01_member_schema.sql과 03_ott_schema.sql 실행 후 실행. 신고 테이블이 ott_room_tb를 참조함.
   ========================================================= */

SET DEFINE OFF;

/* =========================================================
   21. [팀 원본 사용] 신고 테이블
   ========================================================= */
CREATE TABLE report_tb (
    report_id          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reporter_id        VARCHAR2(20) NOT NULL,
    reported_member_id VARCHAR2(20) NOT NULL,
    room_id            NUMBER,
    report_reason      VARCHAR2(500) NOT NULL,
    report_status      VARCHAR2(20) DEFAULT 'WAIT' NOT NULL,
    admin_comment      VARCHAR2(1000),
    created_at         DATE DEFAULT SYSDATE NOT NULL,
    processed_at       DATE,

    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES member_tb(id),
    CONSTRAINT fk_report_reported FOREIGN KEY (reported_member_id) REFERENCES member_tb(id),
    CONSTRAINT fk_report_room FOREIGN KEY (room_id) REFERENCES ott_room_tb(room_id),
    CONSTRAINT uk_report_duplicate UNIQUE (reporter_id, room_id, report_reason),
    CONSTRAINT ck_report_status CHECK (report_status IN ('WAIT', 'PROCESSING', 'COMPLETE', 'REJECT'))
);

CREATE INDEX idx_report_reported ON report_tb(reported_member_id, report_status);
-- uk_report_duplicate는 CREATE TABLE 안에서 이미 생성하므로 중복 ALTER를 두지 않는다.
/* =========================================================
   22. [팀 원본 사용] 경고 테이블
   ========================================================= */
CREATE TABLE warning_tb (
    warning_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id      VARCHAR2(20) NOT NULL,
    report_id      NUMBER,
    warning_reason VARCHAR2(500) NOT NULL,
    penalty_days   NUMBER,
    status         CHAR(1) DEFAULT 'N' NOT NULL,
    created_at     DATE DEFAULT SYSDATE NOT NULL,


    CONSTRAINT fk_warning_member FOREIGN KEY (member_id) REFERENCES member_tb(id),
    CONSTRAINT fk_warning_report FOREIGN KEY (report_id) REFERENCES report_tb(report_id),
    CONSTRAINT ck_warning_status CHECK (status IN ('Y', 'N'))
);



CREATE INDEX idx_warning_member ON warning_tb(member_id);
