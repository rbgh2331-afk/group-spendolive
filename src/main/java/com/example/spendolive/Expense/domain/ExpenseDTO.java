package com.example.spendolive.expense.domain;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseDTO {

    private Long expense_id;        // 지출 내역 고유 번호
    private Long member_id;         // 지출을 등록한 회원 고유 번호
    private Long category_id;       // 선택한 지출 카테고리 고유 번호

    private String category_name;   // 카테고리 이름
    private String expense_type;    // 지출 유형(FIXED: 고정, VARIABLE: 변동, OTT: OTT)


    private String expense_title;   // 지출 내역 제목
    private Integer amount;         // 지출 금액

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date expense_date;      // 실제 지출 날짜(yyyy-MM-dd)

    private String payment_method;  // 결제 수단
    private String memo;            // 지출 관련 메모

    private String repeat_yn;       // 반복 지출 여부(Y/N)
    private String repeat_cycle;    // 반복 주기(MONTHLY/WEEKLY/YEARLY)

    // [고정지출 종료월] DB에는 선택한 종료월의 마지막 날짜를 저장한다.
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date repeat_end_date;

    // [고정지출 종료월] input type="month"의 yyyy-MM 값을 받는 화면 전용 필드다.
    private String repeat_end_month;

    private String fixed_yn;        // 고정 지출 여부(Y/N)

    private String auto_generated_yn; // 반복 설정으로 화면에서 자동 생성된 내역 여부(Y/N)

    private Date created_at;        // 지출 내역 생성 일시(yyyy-MM-dd HH:mm:ss)
    private Date updated_at;        // 지출 내역 수정 일시(yyyy-MM-dd HH:mm:ss)
}
