package com.example.spendolive.payment.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

/**
 * 12. 정산 환불 테이블 도메인 객체
 */
@Getter
@Setter
@ToString
public class SettlementRefundVO {

    private int refund_id;              // 환불 내역 고유 ID (PK, 시퀀스 자동 생성)
    private int payment_id;             // 원본 입금 내역 고유 ID (FK)
    private int settlement_id;          // 월별 정산 고유 ID (FK)
    private String member_login_id;                  // 환불받을 회원 아이디 (FK, member_tb)
    private int refund_amount;          // 최종 환불 예정 금액

    // 사유값: ROOM_CLOSE(방 삭제), PAYMENT_CANCEL(결제 취소), ADMIN_CANCEL(관리자 취소), ETC(기타)
    private String refund_reason;       // 환불 발생 사유 (기본값: 'ROOM_CLOSE')

    // 상태값: REQUESTED(환불 요청됨), COMPLETED(환불 완료), FAILED(환불 실패)
    private String refund_status;       // 환불 처리 상태 (기본값: 'REQUESTED')

    private LocalDateTime requested_at; // 환불 요청 일시 (기본값: SYSDATE) YYYY/mm/DD HH:MM
    private LocalDateTime completed_at; //  환불 최종 완료 일시 YYYY/mm/DD HH:MM
    private String memo;                // 환불 실패 사유 등 상세 메모
}
