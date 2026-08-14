package com.example.spendolive.ott.domain;

import lombok.Getter;
import lombok.Setter;

// OTT 정산 DTO - 방 정산과 개인 결제 상태 전달
@Getter
@Setter
public class OttSettlementDTO {

    // 정산 회차 정보
    private Long settlement_id;
    private Long room_id;
    private String room_name;
    private String service_name;
    private String settlement_month;
    private Integer total_price;
    private Integer total_fee;
    private Integer total_pay_amount;

    // 결제 및 이용 기간
    private String due_date;
    private String payment_start_date;
    private String payment_close_date;
    private String service_start_date;
    private String service_end_date;
    private String replace_start_date;
    private String replace_end_date;
    private String status;
    private String created_at;

    // 사용자 역할과 결제 상태
    private String my_role;
    private int member_limit;
    private Long payment_id;
    private String my_payment_status;
    private Integer my_total_amount;
    private String settlement_status;

    // 방장이 보는 팀원별 정산 상태 표시용
    // 팀원별 정산 상태
    private String member_login_id;
    private String member_name;
    private String member_nickname;
    private Integer base_amount;
    private Integer fee_amount;
    private Integer total_amount;
    private String payment_status;
    private String paid_at;

    // 방장 로그인 ID
    private String host_login_id;
}
