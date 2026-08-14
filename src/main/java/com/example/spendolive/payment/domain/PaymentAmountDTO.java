package com.example.spendolive.payment.domain;

import lombok.Data;

/**
 * OTT 방 결제 화면과 실제 결제 처리에서 공통으로 사용하는 금액 정보입니다.
 * 화면에서 보인 금액과 실제 승인 금액이 달라지지 않도록 서버에서 한 번 계산
 */
@Data
public class PaymentAmountDTO {
    private final int roomId;
    private final int settlementId;
    private final String roomName;
    private final String hostLoginId;
    private final int memberLimit;
    private final int baseAmount;
    private final int feeRate;
    private final int feeAmount;
    private final int totalAmount;
    private final int automaticPaymentDay;

}
