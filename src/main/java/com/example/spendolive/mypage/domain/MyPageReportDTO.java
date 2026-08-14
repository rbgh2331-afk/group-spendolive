package com.example.spendolive.mypage.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 마이페이지 신고 내역 전달 객체
 */
@Getter
@Setter
public class MyPageReportDTO {
    private Long report_id;
    private String reported_member_id;
    private String reported_member_name;
    private String reported_member_nickname;
    private String report_reason;
    private String report_status;
    private String admin_comment;
    private String created_at;
    private String processed_at;
    private String blocked_yn;

}
