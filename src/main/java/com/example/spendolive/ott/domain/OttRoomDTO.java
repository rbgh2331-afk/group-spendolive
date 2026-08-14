package com.example.spendolive.ott.domain;

import lombok.Getter;
import lombok.Setter;

// OTT 공유방 DTO - 방 정보와 사용자 참여 상태 전달
@Getter
@Setter
public class OttRoomDTO {

    // 방 식별 및 소유 정보
    private Long room_id;
    private String host_login_id;
    private String host_nickname;

    // OTT 서비스 및 방 설정
    private Long ott_service_id;
    private String service_name;
    private String room_name;
    private String plan_name;
    private Integer total_price;
    private Integer billing_day;
    private Integer member_limit;
    private Integer current_member_count;
    private String status;
    private String invite_code;

    // 방 종료 예약 정보
    private String close_requested_at;
    private String close_effective_date;
    private String close_reason;
    private String close_notice;
    private String closed_at;
    private String created_at;

    // 1인 정산 계산값
    private Integer share_amount;
    private Integer fee_amount;
    private Integer per_person_amount;
    private Double platform_fee_rate;
    private Integer base_price;
    private Integer extra_member_fee;
    private Integer extra_member_count;

    // 사용자 참여 및 탈퇴 상태
    private String my_application_status;
    private String leave_reserved_yn;
    private String leave_requested_at;
    private String leave_scheduled_date;
    private String leave_cancelled_at;
    private String leave_reason;

    // 화면 분기용 값
    private String room_mode;
    private String settlement_status;
}
