package com.example.spendolive.payment.domain;
import lombok.Data;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class SettlementPaymentVO {
    private int payment_id;         // PK
    private int settlement_id;      // 정산 마스터 외래키
    private String id;              // 파티원 ID (member_tb 외래키)
    private int base_amount;        // 순수 분담금 (넷플릭스 1/N 가격)
    private Double fee_rate;         // 수수료율 (ex: 3.00)
    private int fee_amount;         // 수수료 금액 (플랫폼 수익)
    private int total_amount;       // 최종 결제 금액 (분담금 + 수수료)
    private String payment_status;   // 상태 (UNPAID, PAID, CONFIRMED, REFUNDED 등)
    private String paymentKey;
    private String orderId;
    private String card_company;   // card_company (VARCHAR2)
    private String card_number;    // card_number (VARCHAR2)

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime paid_at;       // 토스 카드 결제 완료 시점  YYYY/mm/DD HH:MM
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime confirmed_at;  // 정산 확정 시점 (방장에게 돈 가도 된다고 확정)  YYYY/mm/DD HH:MM
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime expired_at;    // 안 내고 버티다 추방된 시점  YYYY/mm/DD HH:MM
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime cancelled_at;  // 환불/취소 완료 시점  YYYY/mm/DD HH:MM
    private String memo;                // 환불 사유 등 비고란
}
