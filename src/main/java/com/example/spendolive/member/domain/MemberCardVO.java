package com.example.spendolive.member.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 회원 카드 정보.
 * 카드 이름 기능을 추가하면서 Lombok 생성 메서드에만 의존하지 않도록
 * 명시적인 getter/setter를 사용
 */
@Data
public class MemberCardVO {
    private int card_idx;
    private String id;
    private String billing_key;
    private String card_company;
    private String card_number;
    private String card_name;
    private LocalDateTime reg_date;
    private String status;
}
