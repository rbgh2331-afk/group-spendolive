package com.example.spendolive.member.domain;

import lombok.Data;

@Data
public class MemberVO {
    private int member_id;
    private int warning_count; //경고 횟수
    private String email;
    private String password;
    private String id;
    private String member_name;
    private String nickname;
    private String phone;
    private String login_type; //로컬,카카오 로그인 경로
    private String verify_type; //인증 유형 핸드폰, 이메일 하지만 둘다 인증해야만 회원가입 가능
    private String role;
    private String status; //아이디 활성화 여부
    private String created_at; // YYYY/mm/DD HH:MM
    private String blocked_until; // 경고 제약 해제 YYYY/mm/DD HH:MM
    private String updated_at; // YYYY/mm/DD HH:MM
    private String last_login_at; // YYYY/mm/DD HH:MM
    private String card_status; // 카드 등록 여부
    private String account_status; // 계좌 등록 여부
    private String warninged_at; // YYYY/mm/DD HH:MM

}

