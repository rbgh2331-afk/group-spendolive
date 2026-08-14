package com.example.spendolive.payment.domain;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EscrowPayoutVO {

    private int escrow_payout_id;    // PK
    private int settlement_id;      // 정산 마스터 외래키
    private int room_id;            // 방 번호 외래키
    private String payer_id;         // 돈을 낸 파티원 ID
    private String host_id;          // 돈을 받을 방장 ID
    private int amount;            // 방장에게 지급될 순수 정산금
    private String status;          // 상태 (HELD: 보관, RELEASED: 방장지급완료, REFUNDED)
    private LocalDateTime created_at; // 파티원 결제 시점 (금고 입고) YYYY/mm/DD HH:MM
    private LocalDate payout_due_date; // 방장 정산 예정일 (이용 기간 끝나고 몇 일 뒤) YYYY/mm/DD
    private LocalDateTime payout_at;  // 방장 계좌로 입금이체(or 포인트) 쏴준 시점 YYYY/mm/DD HH:MM
}
