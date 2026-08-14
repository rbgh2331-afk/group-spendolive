package com.example.spendolive.ott.domain;

import lombok.Getter;
import lombok.Setter;

// OTT 서비스 DTO - 서비스 정보와 고정 요금 규칙 전달
@Getter
@Setter
public class OttServiceDTO {
    private Long ott_service_id;
    private String service_name;

    // 외부 모집방 분담 기준 총액
    // 기본요금과 추가 인원 요금 합계
    // defaultPrice는 기존 컬럼명 유지: 피클플러스 방식으로 계산된 최종 분담 대상 금액
    // 예) 넷플릭스 프리미엄 17,000 + 추가회원 5,000 * 2 = 27,000
    private Integer default_price;

    // 공유 가능 여부 및 위험 정보
    private String share_yn;
    private String risk_level;
    private String block_reason;

    // OTT별 최고 멤버십 고정 정보
    private String fixed_plan_name;
    private Integer base_price;
    private Integer extra_member_fee;
    private Integer extra_member_count;
    private Integer max_member_limit;
    private Double platform_fee_rate;

    // 화면 표시용 계산값
    // 사용자 1인 기준 계산값
    private Integer share_amount;
    private Integer fee_amount;
    private Integer per_person_amount;
}
