package com.example.spendolive.member.domain;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberAccountVO {

    private int account_idx;        // account_idx (NUMBER -> int 또는 Integer)
    private String account_holder_nam; //예금주
    private String id;             // id (VARCHAR2)
    private String bank_code;       // bank_code (VARCHAR2) 은행코드
    private String account_number;  // account_number (VARCHAR2) 계좌 번호
    private String fintech_use_num;  // fintech_use_num (VARCHAR2) 계좌 금융api 사용 키
    private int balance;           // 계좌잔액
    private String open_bank_token;  // open_bank_token (VARCHAR2) 금결원 인증 토큰
    private String open_bank_user_seq;// open_bank_user_seq (VARCHAR2) 금결원 인증 사용자 번호
    private  LocalDateTime reg_date;          // reg_date (DATE)YYYY/mm/DD
    private String from_date;  // 거래내역 조회 시작 날짜
    private String to_date;// 거래내역 조회 종료 날짜
    private String from_time;  // 거래내역 조회 시작 날짜
    private String to_time;// 거래내역 조회 종료 날짜
    private String account_name;            // 계좌 이름 별명
    private String status;            // 계좌 이름 별명

}
