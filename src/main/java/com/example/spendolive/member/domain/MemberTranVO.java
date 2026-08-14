package com.example.spendolive.member.domain;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberTranVO {
    private int member_tran_idx;
    private int account_idx;       // 거래가 발생한 계좌 번호
    private String id;             // 회원 아이디
    private int tran_amt;          // 입금은 양수, 출금은 음수로 저장
    private String inout_type;     // 입금 또는 출금
    private String tran_date;      // 거래일시(yyyyMMddHHmmss)
    private Long balance_after;    // 해당 거래가 끝난 직후의 계좌 잔액
    private String reg_date;       // DB 등록일시
}
