package com.example.spendolive.report.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WarningVO {
    private Long warning_id;            // NUMBER -> Long 
    private String member_id;           // VARCHAR2(20) 
    private int report_id;             // NUMBER -> Long (Null 허용)
    private String warning_reason;      // VARCHAR2(500) 경고 이유 -> 신고 채팅 내용 
    private Integer penalty_days;       // NUMBER -> Integer
    private String status;        // CHAR(1) DEFAULT 'N'
    private LocalDateTime created_at;   // DATE -> LocalDateTime
}