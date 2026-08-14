package com.example.spendolive.payment.domain;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 14. 플랫폼 수익 테이블 도메인 객체
 */
@Data
public class PlatformRevenueVO {

    private int revenue_id;             // 플랫폼 수익 고유 ID (PK, 시퀀스 자동 생성)
    private int settlement_id;          // 월별 정산 고유 ID (FK)
    private int room_id;                // OTT 매칭방 고유 ID (FK)
    private String payer_id;            // 수수료를 지불한 회원(파티원) 아이디 (FK)
    private int base_amount;            // 수수료 계산의 기준이 된 원금
    private double fee_rate;            // 적용 수수료율 (기본값: 3.00%)
    private int fee_amount;             // 플랫폼이 떼어간 최종 수수료 수익 금액

    // 상태값: EARNED(수익 확정), REFUNDED(환불로 인한 수익 취소), CANCELLED(취소됨)
    private String status;      // 수수료 수익 상태 (기본값: 'EARNED')

    private LocalDateTime created_at;   // 플랫폼 수익 발생 일시 (기본값: SYSDATE) YYYY/mm/DD HH:MM
}
