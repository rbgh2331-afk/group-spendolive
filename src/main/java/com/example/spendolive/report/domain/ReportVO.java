package com.example.spendolive.report.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportVO {
    private Long report_id;             // NUMBER -> Long
    private String reporter_id;         // VARCHAR2(20) // 신고자 아이디
    private String reported_member_id;  // VARCHAR2(20) 피신고자 아이디
    private int room_id;               // NUMBER -> Long (Null 허용이므로 래퍼 클래스 사용)
    private String report_reason;       // VARCHAR2(500) 신고이유
    private String report_status;       // VARCHAR2(20) DEFAULT 'WAIT' 신고 처리 상태
    private String admin_comment;       // VARCHAR2(1000) 관리자 답변
    private LocalDateTime created_at;   // DATE -> LocalDateTime 신고 날짜 YYYY/mm/DD HH:MM
    private LocalDateTime processed_at; // DATE -> LocalDateTime 처리 된 시간 YYYY/mm/DD HH:MM
}
